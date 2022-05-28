package com.mugu.blog.article.controller.admin;

import com.mugu.blog.article.aspect.AvoidRepeatableCommit;
import com.mugu.blog.article.common.model.po.Type;
import com.mugu.blog.article.common.model.req.TypeAddReq;
import com.mugu.blog.article.common.model.req.TypeListReq;
import com.mugu.blog.article.common.model.vo.TypeVo;
import com.mugu.blog.article.service.TypeService;
import com.mugu.blog.common.annotation.RequiresRoles;
import com.mugu.blog.core.model.PageData;
import com.mugu.blog.core.model.ResultMsg;
import com.mugu.blog.mybatis.config.utils.PageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(value = "分类接口-后台")
@RestController
@RequestMapping("/admin/type")
public class TypeAdminController {

    @Autowired
    private TypeService typeService;

    @RequiresRoles
    @AvoidRepeatableCommit
    @ApiOperation("添加分类")
    @PostMapping("/add")
    public ResultMsg<Void> add(@RequestBody @Valid TypeAddReq param){
        typeService.add(param);
        return ResultMsg.resultSuccess();
    }

    @RequiresRoles
    @ApiOperation("分页查询分类")
    @PostMapping("/list")
    public ResultMsg<PageData<TypeVo>> list(@RequestBody TypeListReq param){
        PageData<TypeVo> page = PageUtils.getPage(param, p -> typeService.list(p));
        return ResultMsg.resultSuccess(page);
    }

    @RequiresRoles
    @ApiOperation("根据ID查询分类详情")
    @GetMapping("/getById")
    public ResultMsg<Type> getById(Long id){
        return ResultMsg.resultSuccess(typeService.getById(id));
    }
}
