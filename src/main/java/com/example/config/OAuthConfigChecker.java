package com.example.config;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;

public class OAuthConfigChecker implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (System.getenv("SFDC_OAUTH_CLIENT_ID") == null || System.getenv("SFDC_OAUTH_CLIENT_SECRET") == null) {
            final String appName = servletRequest.getRemoteHost().replace(".herokuapp.com", "");
            ((HttpServletResponse)servletResponse).sendRedirect("https://agi.herokuapp.com/oauthConfig?app=" + URLEncoder.encode(appName, "UTF-8") + "&callbackUrl=/_auth");
        }
    }

    @Override
    public void destroy() {}
}
