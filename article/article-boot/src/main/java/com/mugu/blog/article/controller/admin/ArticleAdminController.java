package com.mugu.blog.article.controller.admin;

import com.mugu.blog.article.aspect.AvoidRepeatableCommit;
import com.mugu.blog.article.common.model.req.ArticleAddReq;
import com.mugu.blog.article.common.model.req.ArticleDelReq;
import com.mugu.blog.article.common.model.req.ArticleInfoReq;
import com.mugu.blog.article.common.model.req.ArticleListReq;
import com.mugu.blog.article.common.model.vo.ArticleVo;
import com.mugu.blog.article.service.ArticleService;
import com.mugu.blog.common.annotation.AuthInjection;
import com.mugu.blog.common.annotation.RequiresRoles;
import com.mugu.blog.core.model.BaseParam;
import com.mugu.blog.core.model.PageData;
import com.mugu.blog.core.model.ResultMsg;
import com.mugu.blog.core.model.oauth.OAuthConstant;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Api(value = "文章相关接口-后台")
@RestController
@RequestMapping("/admin/article")
@Slf4j
public class ArticleAdminController {

    @Autowired
    private ArticleService articleService;

    @RequiresRoles
    @AvoidRepeatableCommit
    @ApiOperation("添加文章")
    @PostMapping("/add")
    public ResultMsg<Void> add(@RequestBody @Valid ArticleAddReq req){
        articleService.add(req);
        return ResultMsg.resultSuccess();
    }

    @RequiresRoles
    @AuthInjection
    @ApiOperation("获取文章列表")
    @PostMapping("/list")
    public ResultMsg<PageData<ArticleVo>> list(@RequestBody @Valid ArticleListReq req){
        return ResultMsg.resultSuccess(articleService.list(req));
    }

    @RequiresRoles
    @ApiOperation("搜索文章")
    @PostMapping("/search")
    public ResultMsg<PageData<ArticleVo>> search(@RequestBody @Valid BaseParam param) {
        return ResultMsg.resultSuccess(articleService.search(param));
    }

    @RequiresRoles
    @AuthInjection
    @ApiOperation("删除文章")
    @PostMapping("/del")
    public ResultMsg<Void> delById(@RequestBody @Valid List<ArticleDelReq> params){
        articleService.delById(params);
        return ResultMsg.resultSuccess();
    }

    @RequiresRoles
    @AuthInjection
    @ApiOperation("根据ID获取文章详情")
    @PostMapping("/getById")
    public ResultMsg<ArticleVo> getById(@RequestBody @Valid ArticleInfoReq req){
        return ResultMsg.resultSuccess(articleService.getById(req));
    }
}
