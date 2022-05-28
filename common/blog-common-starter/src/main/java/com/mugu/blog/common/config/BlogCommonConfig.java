package com.mugu.blog.common.config;

import cn.hutool.core.util.ArrayUtil;
import com.mugu.blog.common.annotation.AuthInjectionAspect;
import com.mugu.blog.common.annotation.PreAuthorizeAspect;
import com.mugu.blog.common.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan(basePackages = {"com.mugu.blog.common.filter","com.mugu.blog.common.exception","com.mugu.blog.common.interceptor"},
    basePackageClasses = {AuthInjectionAspect.class, PreAuthorizeAspect.class}
)
public class BlogCommonConfig implements WebMvcConfigurer {
    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //Swagger需要排除的uri
        final String[] swaggerExclude = {"/swagger-resources/**", "/webjars/**", "/v2/**", "/swagger-ui.html/**", "/", "/csrf"};
        registry.addInterceptor(authInterceptor).excludePathPatterns(ArrayUtil.addAll(swaggerExclude));
    }
}
