package com.azheng.boot.user.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 用户上下文拦截器
     */
    private final UserContextInterceptor userContextInterceptor;

   /**
    * 注册 Sa-Token 拦截器，打开注解式鉴权功能
    * 分析：这是干什么的？
    * ——
    * @param registry
    */
   @Override
   public void addInterceptors(InterceptorRegistry registry) {
      // 注册 SaToken 登录拦截器
      registry.addInterceptor(new SaInterceptor(handler -> {
                 // 异步调度请求跳过登录检查（SSE 完成回调会触发 asyncDispatch，此时 SaToken 上下文已丢失）
                 ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                 if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    // 判断是否为异步调度请求，如果是则跳过登录检查
                    if (request.getDispatcherType() == DispatcherType.ASYNC) {
                       return;
                    }
                    // 预检请求直接放行，避免 CORS 被拦截
                    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                       return;
                    }
                 }
                 // 执行登录检查
                 StpUtil.checkLogin();
              }))
              // 拦截所有路径
              .addPathPatterns("/**")
              // 排除认证相关路径和错误页面
              .excludePathPatterns("/api/auth/**", "/error");


       // 注册用户上下文拦截器
       registry.addInterceptor(userContextInterceptor)
               // 拦截所有路径
               .addPathPatterns("/**")
               // 排除认证相关路径和错误页面
               .excludePathPatterns("/api/auth/**", "/error");
   }


}
