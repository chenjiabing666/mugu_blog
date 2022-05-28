package com.mugu.blog.gateway.config;

import cn.hutool.core.util.ArrayUtil;
import com.mugu.blog.gateway.exception.RequestAccessDeniedHandler;
import com.mugu.blog.gateway.exception.RequestAuthenticationEntryPoint;
import com.mugu.blog.gateway.model.WhiteUrls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.web.server.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * @author 公众号：码猿技术专栏
 * 网关的OAuth2.0资源的配置类
 * 由于spring cldou gateway使用的Flux，因此需要使用@EnableWebFluxSecurity注解开启，而不是平常的web应用了
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * JWT的鉴权管理器
     */
    @Autowired
    private ReactiveAuthorizationManager<AuthorizationContext> accessManager;

    /**
     * token过期的异常处理
     */
    @Autowired
    private RequestAuthenticationEntryPoint requestAuthenticationEntryPoint;

    /**
     * 权限不足的异常处理
     */
    @Autowired
    private RequestAccessDeniedHandler requestAccessDeniedHandler;

    /**
     * token校验管理器
     */
    @Autowired
    private ReactiveAuthenticationManager tokenAuthenticationManager;

    //    @Autowired
//    private CorsFilter corsFilter;

    @Bean
    public CorsWebFilter corsWebFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //1.配置跨域
        //允许哪种请求头跨域
        corsConfiguration.addAllowedHeader("*");
        //允许哪种方法类型跨域 get post delete put
        corsConfiguration.addAllowedMethod("*");
        // 允许哪些请求源跨域
        corsConfiguration.addAllowedOrigin("*");
        // 是否携带cookie跨域
        corsConfiguration.setAllowCredentials(true);

        //允许跨域的路径
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsWebFilter(source);
    }

    @Autowired
    private WhiteUrls whiteUrls;

    @Bean
    SecurityWebFilterChain webFluxSecurityFilterChain(ServerHttpSecurity http) throws Exception {
        //认证过滤器，放入认证管理器tokenAuthenticationManager
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(tokenAuthenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(new ServerBearerTokenAuthenticationConverter());

        http
                .httpBasic().disable()
                .csrf().disable()
                .authorizeExchange()
                //白名单直接放行
                .pathMatchers(ArrayUtil.toArray(whiteUrls.getUrls(), String.class)).permitAll()
                //其他的请求必须鉴权，使用鉴权管理器
                .anyExchange().permitAll()
                //鉴权的异常处理，权限不足，token失效
                .and().exceptionHandling()
                .authenticationEntryPoint(requestAuthenticationEntryPoint)
                .accessDeniedHandler(requestAccessDeniedHandler)
                .and()
                // 跨域过滤器
                .addFilterAt(corsWebFilter(), SecurityWebFiltersOrder.CORS)
                //token的认证过滤器，用于校验token和认证
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }
}