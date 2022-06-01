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
import com.mugu.blog.common.utils.OauthUtils;
import com.mugu.blog.core.constant.KeyConstant;
import com.mugu.blog.core.model.BaseParam;
import com.mugu.blog.core.model.PageData;
import com.mugu.blog.core.model.ResultCode;
import com.mugu.blog.core.model.ResultMsg;
import com.mugu.blog.core.utils.IpUtils;
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
import java.util.concurrent.Executor;
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
    private Executor asyncTaskExecutor;

    @Transactional
    @Override
    public void add(ArticleAddReq req) {
        Article article = new Article();
        BeanUtils.copyProperties(req,article);
        //获取当前用户的userId
        article.setAuthorId(OauthUtils.getCurrentUser().getUserId());
        article.setCreateTime(new Date());
        article.setUpdateTime(new Date());
        article.setArticleId(String.valueOf(SnowflakeUtil.nextId()));
        //阅读人数
        article.setViewNum(0L);
        int i = articleMapper.insert(article);
        AssertUtils.assertTrue(i==1);

        ArticleMq articleMq = new ArticleMq();
        BeanUtils.copyProperties(article,articleMq);
        if (Integer.valueOf(2).equals(req.getStatus())){
            //新增
            articleMq.setFlag(1);
            //发送消息给MQ
            sendMsgService.sendMsg(RabbitMqConfig.EXCHANGE_NAME,RabbitMqConfig.ROUTING_KEY, JSON.toJSONString(articleMq));
        }
    }

    @SneakyThrows
    @Override
    public PageData<ArticleVo> list(ArticleListReq req) {
        //分页获取所有的内容
        PageData<Article> articlePage = PageUtils.getPage(req, req1 -> articleMapper.pageList(req));
        AssertUtils.assertTrue(CollectionUtil.isNotEmpty(articlePage.getResult()),ResultCode.ARTICLE_NO_PAGE);

        List<ArticleVo> articleVos = new ArrayList<>();
        PageData<ArticleVo> data = new PageData<>();
        data.setPages(articlePage.getPages());
        data.setTotal(articlePage.getTotal());
        /**
         * TODO 优化点一：此处需要优化，不能循环调用服务
         * TODO 优化点二：此处采用的异步并行调用服务，因为调用之间并无任何依赖
         * 将条件以List方式传入，一次调用获取集合，然后聚合
         */
        List<Article> articles = articlePage.getResult();
        //获取主线程的请求信息
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        //1. 异步获取文章作者的信息
        CompletableFuture<List<SysUser>> futureSysUsers = CompletableFuture.supplyAsync(SupplierWrapper.of(() -> {
            //将主线程的请求信息设置到异步线程中，否则会丢失请求上下文，导致调用失败
            RequestContextHolder.setRequestAttributes(attributes);
            //获取所有的作者ID
            List<String> authorIds = articles.stream().map(Article::getAuthorId).collect(Collectors.toList());
            List<SysUser> sysUsers = new ArrayList<>();
            //调用user服务获取作者信息
            ResultMsg<List<SysUser>> authorRes = userFeign.listByUserId(authorIds);
            if (ResultMsg.isSuccess(authorRes) && authorRes.getData() != null) {
                sysUsers = authorRes.getData();
            }
            return sysUsers;
        }), asyncTaskExecutor);

        //2. 异步获取文章的评论总数
        CompletableFuture<List<TotalVo>> futureCommentTotal = CompletableFuture.supplyAsync(SupplierWrapper.of(() -> {
            //将主线程的请求信息设置到异步线程中，否则会丢失请求上下文，导致调用失败
            RequestContextHolder.setRequestAttributes(attributes);
            List<TotalVo> commentVos = new ArrayList<>();
            //获取所有的文章ID
            List<CommentListReq> params = articles.stream().map(p -> {
                CommentListReq param = new CommentListReq();
                param.setArticleId(p.getArticleId());
                return param;
            }).collect(Collectors.toList());
            //调用评论信息获取评论总数
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

            //作者相关信息
            futureSysUsers.get().stream().filter(p-> StrUtil.equals(article.getAuthorId(),p.getUserId())).limit(1).peek(p->{
                vo.setAuthorName(p.getNickname());
                vo.setAuthorAvatar(p.getAvatar());
            }).collect(Collectors.toList());

            //评论
            futureCommentTotal.get().stream().filter(p-> StrUtil.equals(article.getArticleId(),p.getArticleId())).limit(1).peek(p-> vo.setCommentNum(p.getTotal())).collect(Collectors.toList());

            vo.setViewNum(stringRedisTemplate.opsForHyperLogLog().size(KeyConstant.ARTICLE_V+article.getArticleId()));

            articleVos.add(vo);
        }
        data.setResult(articleVos);
        return  data;
    }

    @Transactional
    @Override
    public int delById(List<ArticleDelReq> params) {
        int i = articleMapper.delById(params);
        AssertUtils.assertTrue(i>0);

        for (ArticleDelReq param : params) {
            //发送消息删除es中的文章
            ArticleMq articleMq = new ArticleMq();
            articleMq.setArticleId(param.getId());
            articleMq.setFlag(3);
            sendMsgService.sendMsg(RabbitMqConfig.EXCHANGE_NAME,RabbitMqConfig.ROUTING_KEY, JSON.toJSONString(articleMq));
        }
        return i;
    }

    @Override
    public ArticleVo getById(ArticleInfoReq param) {
        Article article = articleMapper.load(param);
        ArticleInfoVo vo = new ArticleInfoVo();
        AssertUtils.assertTrue(article!=null,ResultCode.ARTICLE_NOT_EXIST);
        BeanUtil.copyProperties(article, vo);

        //获取作者的名称、头像
        ResultMsg<SysUser> res1 = userFeign.getByUserId(article.getAuthorId());
        if (ResultMsg.isSuccess(res1)&&Objects.nonNull(res1.getData())){
            vo.setAuthorName(res1.getData().getNickname());
            vo.setAuthorAvatar(res1.getData().getAvatar());
        }


        //获取分类的名称
        Type type = typeService.getById(article.getTypeId());
        if (Objects.nonNull(type)){
            vo.setTypeName(type.getName());
            vo.setTypeId(article.getTypeId());
        }

        //获取评论总数
        CommentListReq listReq = new CommentListReq();
        listReq.setArticleId(article.getArticleId());
        ResultMsg<Long> commentRes = commentFeign.total(listReq);
        if (ResultMsg.isSuccess(commentRes)){
            vo.setCommentNum(commentRes.getData());
        }

        //统计阅读量,这里对精度没要求，使用HyperLogLog，而不是使用SET了
        stringRedisTemplate.opsForHyperLogLog().add(KeyConstant.ARTICLE_V+article.getArticleId(), IpUtils.getIpAddress(RequestContextUtils.getRequest()));
        vo.setViewNum(stringRedisTemplate.opsForHyperLogLog().size(KeyConstant.ARTICLE_V+article.getArticleId()));
        return vo;
    }

    @Override
    public PageData<ArticleVo> search(BaseParam req) {
        PageRequest pageable = PageRequest.of(req.getPageNum()-1, req.getPageSize());
        //去除空格
        String whiteKeyword=StrUtil.replace(req.getKeyword()," ","");
        //高亮搜索
        PageData<ArticleEs> pages = searchHighLight(whiteKeyword, req.getPageNum(), req.getPageSize());
        List<ArticleVo> articleVos = new ArrayList<>();
        PageData<ArticleVo> data = new PageData<>();
        data.setPages(pages.getPages());
        data.setTotal(pages.getTotal());
        //无数据
        if (!CollectionUtil.isNotEmpty(pages.getResult())){
            data.setResult(null);
            return data;
        }

        for (ArticleEs article : pages.getResult()) {
            ArticleVo vo = new ArticleVo();
            BeanUtil.copyProperties(article, vo);

            //获取作者的名称、头像
            ResultMsg<SysUser> userRes = userFeign.getByUserId(article.getAuthorId());
            if (ResultMsg.isSuccess(userRes)&&Objects.nonNull(userRes.getData())){
                vo.setAuthorName(userRes.getData().getNickname());
                vo.setAuthorAvatar(userRes.getData().getAvatar());
            }


            //获取分类的名称
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
        //根据一个值查询多个字段  并高亮显示  这里的查询是取并集，即多个字段只需要有一个字段满足即可
        //需要查询的字段：标题，摘要，内容
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .should(QueryBuilders.matchQuery("title", keyword))
                .should(QueryBuilders.matchQuery("content", keyword))
                .should(QueryBuilders.matchQuery("describe", keyword));
        //构建高亮查询
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
