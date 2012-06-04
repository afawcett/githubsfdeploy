package com.example.config;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OAuthConfigChecker implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (System.getenv("SFDC_OAUTH_CLIENT_ID") == null || System.getenv("SFDC_OAUTH_CLIENT_SECRET") == null) {
            final String appName = (servletRequest.getServerName().contains(".herokuapp.com"))
                    ? servletRequest.getServerName().replace(".herokuapp.com", "")
                    : (servletRequest.getServerName() + ":" + servletRequest.getServerPort());
            ((HttpServletResponse)servletResponse).sendRedirect("https://agi.herokuapp.com/oauthConfig?app=" + appName + "&callbackUrl=/_auth");
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {}
}
