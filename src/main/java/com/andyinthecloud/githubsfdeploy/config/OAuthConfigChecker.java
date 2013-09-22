package com.andyinthecloud.githubsfdeploy.config;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OAuthConfigChecker implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        //If the environment variables are not set, send them to a "error" page that will tell them to add this to 
    	//their environment
    	if (System.getenv("SFDC_OAUTH_CLIENT_ID") == null || System.getenv("SFDC_OAUTH_CLIENT_SECRET") == null) {
            ((HttpServletResponse)servletResponse).sendRedirect("/sfdcSetup.html");
            return;
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {}
}
