package com.example.reggie.filter;

import com.alibaba.fastjson.JSONObject;
import com.example.reggie.common.BaseContext;
import com.example.reggie.common.Result;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
@Slf4j
@WebFilter(filterName = "LoginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1.获取请求URI
        String requestURI = request.getRequestURI();
        log.info("拦截到请求的URI:{}",requestURI);
        //定义不需要处理的请求路径
        String[] urls = new String[]{
                "/emp/login",
                "/emp/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login"
        };
        //2.判断请求是否需要处理
        boolean check = check(urls, requestURI);
        //3.如果不需要处理 ，放行
        if(check){
            log.info("本次请求{}不需要处理", requestURI);
            filterChain.doFilter(request,response);
            return;
        }
        //4-1.判断emp登录状态，如果已登录直接放行
        if(request.getSession().getAttribute("emp") != null){
            log.info("用户已登录,ID为{}",request.getSession().getAttribute("emp"));

            Long empId = (Long) request.getSession().getAttribute("emp");
            BaseContext.setCurrentId(empId); //当前用户ID存入

            filterChain.doFilter(request,response);
            return;
        }
        //4-2.判断user登录状态，如果已登录直接放行
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登录,ID为{}",request.getSession().getAttribute("user"));

            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId); //当前用户ID存入

            filterChain.doFilter(request,response);
            return;
        }
        log.info("用户未登录");
        //5.如果未登录则返回未登录结果，通过输出流方式向客户端页面响应数据
        response.getWriter().write(JSONObject.toJSONString(Result.error("NOTLOGIN")));//返回的NOTLOGIN与前端request.js对应
        return;
    }

    public boolean check(String[] urls, String reuqestURI){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, reuqestURI);
            if(match){
                return true;
            }
        }
        return false;
    }
}
