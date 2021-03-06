package com.mugu.blog.article.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.mugu.blog.article.common.model.po.Article;
import com.mugu.blog.article.common.model.po.Type;
import com.mugu.blog.article.common.model.req.ArticleAddReq;
import com.mugu.blog.article.common.model.req.ArticleDelReq;
import com.mugu.blog.article.common.model.req.ArticleInfoReq;
import com.mugu.blog.article.common.model.req.ArticleListReq;
import com.mugu.blog.article.common.model.vo.ArticleInfoVo;
import com.mugu.blog.article.common.model.vo.ArticleVo;
import com.mugu.blog.article.dao.ArticleMapper;
import com.mugu.blog.article.es.dao.ArticleRepository;
import com.mugu.blog.article.es.model.ArticleEs;
import com.mugu.blog.article.mq.config.RabbitMqConfig;
import com.mugu.blog.article.mq.model.ArticleMq;
import com.mugu.blog.article.service.ArticleService;
import com.mugu.blog.article.service.SendMsgService;
import com.mugu.blog.article.service.TypeService;
import com.mugu.blog.comments.api.feign.CommentFeign;
import com.mugu.blog.comments.common.model.req.CommentListReq;
import com.mugu.blog.comments.common.model.vo.TotalVo;
import com.mugu.blog.common.utils.AssertUtils;
import com.mugu.blog.core.constant.KeyConstant;
import com.mugu.blog.core.model.BaseParam;
import com.mugu.blog.core.model.PageData;
import com.mugu.blog.core.model.ResultCode;
import com.mugu.blog.core.model.ResultMsg;
import com.mugu.blog.core.utils.IpUtils;
import com.mugu.blog.core.utils.OauthUtils;
import com.mugu.blog.core.utils.RequestContextUtils;
import com.mugu.blog.core.utils.SnowflakeUtil;
import com.mugu.blog.mybatis.config.utils.PageUtils;
import com.mugu.blog.user.api.feign.UserFeign;
import com.mugu.blog.user.common.po.SysUser;
import lombok.SneakyThrows;
import org.apache.skywalking.apm.toolkit.trace.SupplierWrapper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SendMsgService sendMsgService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserFeign userFeign;

    @Autowired
    private TypeService typeService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CommentFeign commentFeign;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    @Transactional
    @Override
    public void add(ArticleAddReq req) {
        Article article = new Article();
        BeanUtils.copyProperties(req,article);
        //?????????????????????userId
        article.setAuthorId(OauthUtils.getCurrentUser().getUserId());
        article.setCreateTime(new Date());
        article.setUpdateTime(new Date());
        article.setArticleId(String.valueOf(SnowflakeUtil.nextId()));
        //????????????
        article.setViewNum(0L);
        int i = articleMapper.insert(article);
        AssertUtils.assertTrue(i==1);

        ArticleMq articleMq = new ArticleMq();
        BeanUtils.copyProperties(article,articleMq);
        if (Integer.valueOf(2).equals(req.getStatus())){
            //??????
            articleMq.setFlag(1);
            //???????????????MQ
            sendMsgService.sendMsg(RabbitMqConfig.EXCHANGE_NAME,RabbitMqConfig.ROUTING_KEY, JSON.toJSONString(articleMq));
        }
    }

    @SneakyThrows
    @Override
    public PageData<ArticleVo> list(ArticleListReq req) {
        //???????????????????????????
        PageData<Article> articlePage = PageUtils.getPage(req, req1 -> articleMapper.pageList(req));
        AssertUtils.assertTrue(CollectionUtil.isNotEmpty(articlePage.getResult()),ResultCode.ARTICLE_NO_PAGE);

        List<ArticleVo> articleVos = new ArrayList<>();
        PageData<ArticleVo> data = new PageData<>();
        data.setPages(articlePage.getPages());
        data.setTotal(articlePage.getTotal());
        /**
         * TODO ????????????????????????????????????????????????????????????
         * TODO ?????????????????????????????????????????????????????????????????????????????????????????????
         * ????????????List??????????????????????????????????????????????????????
         */
        List<Article> articles = articlePage.getResult();
        //??????????????????????????????
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        //1. ?????????????????????????????????
        CompletableFuture<List<SysUser>> futureSysUsers = CompletableFuture.supplyAsync(SupplierWrapper.of(() -> {
            //?????????????????????????????????????????????????????????????????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(attributes);
            //?????????????????????ID
            List<String> authorIds = articles.stream().map(Article::getAuthorId).collect(Collectors.toList());
            List<SysUser> sysUsers = new ArrayList<>();
            //??????user????????????????????????
            ResultMsg<List<SysUser>> authorRes = userFeign.listByUserId(authorIds);
            if (ResultMsg.isSuccess(authorRes) && authorRes.getData() != null) {
                sysUsers = authorRes.getData();
            }
            return sysUsers;
        }), asyncTaskExecutor);

        //2. ?????????????????????????????????
        CompletableFuture<List<TotalVo>> futureCommentTotal = CompletableFuture.supplyAsync(SupplierWrapper.of(() -> {
            //?????????????????????????????????????????????????????????????????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(attributes);
            List<TotalVo> commentVos = new ArrayList<>();
            //?????????????????????ID
            List<CommentListReq> params = articles.stream().map(p -> {
                CommentListReq param = new CommentListReq();
                param.setArticleId(p.getArticleId());
                return param;
            }).collect(Collectors.toList());
            //????????????????????????????????????
            ResultMsg<List<TotalVo>> commentRes = commentFeign.listTotal(params);
            if (ResultMsg.isSuccess(commentRes) && commentRes.getData() != null) {
                commentVos = commentRes.getData();
            }
            return commentVos;
        }), asyncTaskExecutor);

        CompletableFuture.allOf(futureSysUsers,futureCommentTotal).join();

        for (Article article : articlePage.getResult()) {
            ArticleVo vo = new ArticleVo();
            BeanUtil.copyProperties(article, vo);

            //??????????????????
            futureSysUsers.get().stream().filter(p-> StrUtil.equals(article.getAuthorId(),p.getUserId())).limit(1).peek(p->{
                vo.setAuthorName(p.getNickname());
                vo.setAuthorAvatar(p.getAvatar());
            }).collect(Collectors.toList());

            //??????
            futureCommentTotal.get().stream().filter(p-> StrUtil.equals(article.getArticleId(),p.getArticleId())).limit(1).peek(p-> vo.setCommentNum(p.getTotal())).collect(Collectors.toList());

            vo.setViewNum(stringRedisTemplate.opsForHyperLogLog().size(KeyConstant.ARTICLE_V+article.getArticleId()));

            articleVos.add(vo);
        }
        data.setResult(articleVos);
        return  data;
    }

    @Transactional
    @Override
    public int delById(ArticleDelReq params) {
        int i = articleMapper.delById(params);
        AssertUtils.assertTrue(i>0);
        //??????????????????es????????????
        ArticleMq articleMq = new ArticleMq();
        articleMq.setArticleId(params.getId());
        articleMq.setFlag(3);
        sendMsgService.sendMsg(RabbitMqConfig.EXCHANGE_NAME,RabbitMqConfig.ROUTING_KEY, JSON.toJSONString(articleMq));
        return i;
    }

    @Override
    public ArticleVo getById(ArticleInfoReq param) {
        Article article = articleMapper.load(param);
        ArticleInfoVo vo = new ArticleInfoVo();
        AssertUtils.assertTrue(article!=null,ResultCode.ARTICLE_NOT_EXIST);
        BeanUtil.copyProperties(article, vo);

        //??????????????????????????????
        ResultMsg<SysUser> res1 = userFeign.getByUserId(article.getAuthorId());
        if (ResultMsg.isSuccess(res1)&&Objects.nonNull(res1.getData())){
            vo.setAuthorName(res1.getData().getNickname());
            vo.setAuthorAvatar(res1.getData().getAvatar());
        }


        //?????????????????????
        Type type = typeService.getById(article.getTypeId());
        if (Objects.nonNull(type)){
            vo.setTypeName(type.getName());
            vo.setTypeId(article.getTypeId());
        }

        //??????????????????
        CommentListReq listReq = new CommentListReq();
        listReq.setArticleId(article.getArticleId());
        ResultMsg<Long> commentRes = commentFeign.total(listReq);
        if (ResultMsg.isSuccess(commentRes)){
            vo.setCommentNum(commentRes.getData());
        }

        //???????????????,?????????????????????????????????HyperLogLog??????????????????SET???
        stringRedisTemplate.opsForHyperLogLog().add(KeyConstant.ARTICLE_V+article.getArticleId(), IpUtils.getIpAddress(RequestContextUtils.getRequest()));
        vo.setViewNum(stringRedisTemplate.opsForHyperLogLog().size(KeyConstant.ARTICLE_V+article.getArticleId()));
        return vo;
    }

    @Override
    public PageData<ArticleVo> search(BaseParam req) {
        PageRequest pageable = PageRequest.of(req.getPageNum()-1, req.getPageSize());
        //????????????
        String whiteKeyword=StrUtil.replace(req.getKeyword()," ","");
        //????????????
        PageData<ArticleEs> pages = searchHighLight(whiteKeyword, req.getPageNum(), req.getPageSize());
        List<ArticleVo> articleVos = new ArrayList<>();
        PageData<ArticleVo> data = new PageData<>();
        data.setPages(pages.getPages());
        data.setTotal(pages.getTotal());
        //?????????
        if (!CollectionUtil.isNotEmpty(pages.getResult())){
            data.setResult(null);
            return data;
        }

        for (ArticleEs article : pages.getResult()) {
            ArticleVo vo = new ArticleVo();
            BeanUtil.copyProperties(article, vo);

            //??????????????????????????????
            ResultMsg<SysUser> userRes = userFeign.getByUserId(article.getAuthorId());
            if (ResultMsg.isSuccess(userRes)&&Objects.nonNull(userRes.getData())){
                vo.setAuthorName(userRes.getData().getNickname());
                vo.setAuthorAvatar(userRes.getData().getAvatar());
            }


            //?????????????????????
            Type type = typeService.getById(article.getTypeId());
            if (Objects.nonNull(type)){
                vo.setTypeName(type.getName());
                vo.setTypeId(article.getTypeId());
            }

            articleVos.add(vo);
        }
        data.setResult(articleVos);
        return data;
    }

    private PageData<ArticleEs> searchHighLight(String keyword,Integer pageNum,Integer pageSize){
        //?????????????????????????????????  ???????????????  ?????????????????????????????????????????????????????????????????????????????????
        //????????????????????????????????????????????????
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .should(QueryBuilders.matchQuery("title", keyword))
                .should(QueryBuilders.matchQuery("content", keyword))
                .should(QueryBuilders.matchQuery("describe", keyword));
        //??????????????????
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(PageRequest.of(pageNum-1, pageSize))
                .withHighlightFields(
                        new HighlightBuilder.Field("title"),
                        new HighlightBuilder.Field("describe"))
                .withHighlightBuilder(new HighlightBuilder().preTags("<span style='color:red'>").postTags("</span>"))
                .build();

        AggregatedPage<ArticleEs> aggregatedPage = elasticsearchTemplate.queryForPage(searchQuery, ArticleEs.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                List<T> chunk = new ArrayList<T>();
                for (SearchHit searchHit : response.getHits().getHits()) {
                    chunk.add((T)mergeResult(searchHit));
                }
                AggregatedPage<T> result=new AggregatedPageImpl<T>(chunk,pageable,response.getHits().getTotalHits());

                return result;
            }

            @Override
            public <T> T mapSearchHit(SearchHit searchHit, Class<T> type) {
                return null;
            }
        });
        PageData<ArticleEs> pageData = new PageData<>();
        pageData.setPages(aggregatedPage.getTotalPages());
        pageData.setTotal(aggregatedPage.getTotalElements());
        pageData.setResult(aggregatedPage.getContent());
        return pageData;
    }

    private ArticleEs mergeResult(SearchHit hit){
        ArticleEs articleEs = JSON.parseObject(hit.getSourceAsString(), ArticleEs.class);
        Map<String, HighlightField> highlightFields = hit.getHighlightFields();
        highlightFields.forEach((field,value)->{
            if (StrUtil.equals("title",field)){
                articleEs.setTitle(value.getFragments()[0].toString());
            }else if (StrUtil.equals("describe",field)){
                articleEs.setDescribe(value.getFragments()[0].toString());
            }else if (StrUtil.equals("content",field)){
                articleEs.setContent(value.getFragments()[0].toString());
            }
        });
        return articleEs;
    }



    @Override
    public Long total() {
        return articleMapper.total();
    }
}
