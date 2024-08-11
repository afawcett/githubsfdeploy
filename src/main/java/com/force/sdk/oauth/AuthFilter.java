/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.sdk.oauth;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.*;
import javax.servlet.http.*;

import com.force.sdk.connector.ForceConnectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.force.sdk.connector.ForceConnectorConfig;
import com.force.sdk.connector.ForceServiceConnector;
import com.force.sdk.oauth.connector.ForceOAuthConnectionInfo;
import com.force.sdk.oauth.connector.ForceOAuthConnector;
import com.force.sdk.oauth.context.*;
import com.force.sdk.oauth.context.store.*;
import com.force.sdk.oauth.exception.ForceOAuthSessionExpirationException;
import com.force.sdk.oauth.userdata.*;
import com.sforce.ws.*;

/**
 * Filter to enable you to add Force.com OAuth Authentication to any web application. When configuring web.xml, all
 * requests that need to be authenticated should be sent through {@code AuthFilter}. The OAuth callback (usually _auth) must
 * also be sent through {@code AuthFilter}. To use the connector, add the following servlet filter to your application's web.xml
 * file:
 * <p>
 * {@code
 * 
 * <!-- Enables Security -->
 * <filter>
 *     <filter-name>AuthFilter</filter-name>
 *     <filter-class>com.force.sdk.oauth.AuthFilter</filter-class>
 *          <init-param>
 *             <param-name>connectionName</param-name>
 *             <param-value>nameOfConnectionToUse</param-value>
 *         </init-param>
 * </filter>
 * <filter-mapping>
 *     <filter-name>AuthFilter</filter-name>
 *     <url-pattern>/*</url-pattern>
 * </filter-mapping>
 * 
 * }
 * <p>
 * The OAuth Connector uses the Force.com API Connector to access the Force.com APIs. The connectionName is used to look
 * up OAuth properties defined in an environment variable, or a Java system property, or in a properties file on the
 * classpath. See @doclink connection-url for more information. Other init parameters that can be set are:
 * <ul>
 * <li>securityContextStorageMethod - valid values are "cookie" or "session". Defaults to "cookie". See @doclink
 * oauth-auth for more information on session management and security</li>
 * <li>secure-key-file - specify the location of the file where your AES secure key is stored.</li> For Cookie based
 * session management.
 * </ul>
 * 
 * @author Fiaz Hossain
 * @author John Simone
 */
public class AuthFilter implements Filter, SessionRenewer {

    static final String FILTER_ALREADY_VISITED = "__force_auth_filter_already_visited";
    static final String SECURITY_AUTH_SUBJECT = "javax.security.auth.subject";
    static final String SECURITY_CONFIG_NAME = "ForceLogin";
    static final String DEFAULT_USER_PROFILE = "myProfile";
    static final String CONTEXT_STORE_SESSION_VALUE = "session";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    private ForceOAuthConnector oauthConnector;
    private SecurityContextService securityContextService = null;
    
    //logout specific parameters
    private boolean logoutFromDatabaseCom = true;
    private String logoutUrl = "";
    
    /**
     * Initializes the filter from the init params.
     * {@inheritDoc} 
     */
    @Override
    public void init(FilterConfig config) throws ServletException {

    	LOGGER.info("Initializing AuthFilter...");
        SecurityContextServiceImpl securityContextServiceImpl = new SecurityContextServiceImpl();

        String customDataRetrieverName = config.getInitParameter("customDataRetriever");
        boolean storeUsername = true;

        if ("false".equals(config.getInitParameter("storeUsername"))) {
            storeUsername = false;
        }

        UserDataRetrievalService userDataRetrievalService = null;

        if (customDataRetrieverName != null) {
            try {
                Class<?> customDataRetrievalClass = Class.forName(customDataRetrieverName);
                Object customDataRetrievalObject = customDataRetrievalClass.newInstance();

                if (customDataRetrievalObject instanceof CustomUserDataRetriever) {
                    CustomUserDataRetriever<?> customDataRetriever = (CustomUserDataRetriever<?>) customDataRetrievalObject;
                    userDataRetrievalService = new CustomUserDataRetrievalService(customDataRetriever, storeUsername);
                }

            } catch (ClassNotFoundException e) {
                throw new ServletException("Custom user data retriever class not found: " + customDataRetrieverName, e);
            } catch (InstantiationException e) {
                throw new ServletException("Custom user data retriever class could not be instantiated: "
                        + customDataRetrieverName, e);
            } catch (IllegalAccessException e) {
                throw new ServletException("Custom user data retriever class could not be instantiated: "
                        + customDataRetrieverName, e);
            }
        } else {
            userDataRetrievalService = new UserDataRetrievalService(storeUsername);
        }

        securityContextServiceImpl.setUserDataRetrievalService(userDataRetrievalService);
        oauthConnector = new ForceOAuthConnector(userDataRetrievalService);

        // Build a ForceOAuthConnectionInfo object, if applicable
        ForceOAuthConnectionInfo connInfo = null;
        if (config.getInitParameter("endpoint") != null) {
            connInfo = new ForceOAuthConnectionInfo();
            connInfo.setEndpoint(config.getInitParameter("endpoint"));
            connInfo.setOauthKey(config.getInitParameter("oauthKey"));
            connInfo.setOauthSecret(config.getInitParameter("oauthSecret"));
            oauthConnector.setConnectionInfo(connInfo);
        } else if (config.getInitParameter("url") != null) {
            connInfo = new ForceOAuthConnectionInfo();

            String connectionUrl = config.getInitParameter("url");
            if (ForceConnectorUtils.isInjectable(connectionUrl)) {
                connectionUrl = ForceConnectorUtils.extractValue(connectionUrl);
                if (connectionUrl == null || connectionUrl.equals("")) {
                    throw new IllegalArgumentException("Unable to load ForceConnectorConfig from environment or system property "
                            + config.getInitParameter("url"));
                }
            }

            connInfo.setConnectionUrl(connectionUrl);
            oauthConnector.setConnectionInfo(connInfo);
        } else if (config.getInitParameter("connectionName") != null) {
            oauthConnector.setConnectionName(config.getInitParameter("connectionName"));
        } else {
            throw new IllegalArgumentException("Could not find any init state for AuthFilter. "
                    + "Please specify an endpoint, oauthKey and oauthSecret or a connection url or a connection name.");
        }

        //set cookie path
        String cookiePath = config.getInitParameter("cookiePath");
        if ( cookiePath == null || cookiePath.isEmpty()){
        	//default to context path
        	cookiePath = config.getServletContext().getContextPath();
        	if(cookiePath.isEmpty()) { //if in the root context set cookie path to "/"
        		cookiePath = "/";
        	}
        }

        LOGGER.info("Using " + cookiePath + " as path for session cookies");
        securityContextServiceImpl.setCookiePath(cookiePath);

        if (CONTEXT_STORE_SESSION_VALUE.equals(config.getInitParameter("securityContextStorageMethod"))) {
            securityContextServiceImpl.setSecurityContextStorageService(new SecurityContextSessionStore());
        } else {
            SecurityContextCookieStore cookieStore = new SecurityContextCookieStore();

            try {
            	if(config.getInitParameter("secure-key-config-var") != null) {
            		LOGGER.info("Setting encryption key based on config var: " + config.getInitParameter("secure-key-config-var"));
            		String key = ForceConnectorUtils.extractValue(config.getInitParameter("secure-key-config-var"));
            		cookieStore.setKey(key);
            	} else {
            		LOGGER.info("Setting encryption key based on file: " + config.getInitParameter("secure-key-file"));
            		cookieStore.setKeyFileName(config.getInitParameter("secure-key-file"));
            	}
            } catch (ForceEncryptionException e) {
                throw new ServletException(e);
            }

            cookieStore.setCookiePath(cookiePath);
            
            securityContextServiceImpl.setSecurityContextStorageService(cookieStore);
        }

        securityContextService = securityContextServiceImpl;
        
        //Logout specific parameters
        if ("false".equalsIgnoreCase(config.getInitParameter("logoutFromDatabaseDotCom"))) {
            logoutFromDatabaseCom = false;
        }
        
        logoutUrl = config.getInitParameter("logoutUrl");

        if (logoutUrl == null || "".equals(logoutUrl)) {
            logoutUrl = "/logout";
        }
    }

    /**
     * Handle the secured requests.
     * {@inheritDoc} 
     */
    @Override
    public void doFilter(ServletRequest sreq, ServletResponse sres, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest request = (HttpServletRequest) sreq;
        HttpServletResponse response = (HttpServletResponse) sres;

        if (request.getAttribute(FILTER_ALREADY_VISITED) != null) {
            // ensure we do not get into infinite loop here
            chain.doFilter(request, response);
            return;
        }

        SecurityContext sc = null;

        // if this isn't the callback from an OAuth handshake
        // get the security context from the session
        if (!ForceOAuthConnector.REDIRECT_AUTH_URI.equals(request.getServletPath())) {
            sc = securityContextService.getSecurityContextFromSession(request);
        }
        
        if (isLogoutUrl(request)) {
            if (sc != null) {
                logout(request, response, sc, chain);
            } else {
                chain.doFilter(request, response);
            }
            return;
        }

        // if there is no valid security context then initiate an OAuth handshake
        if (sc == null) {
            doOAuthLogin(request, response);
            return;
        } else {
            securityContextService.setSecurityContextToSession(request, response, sc);
        }
        
        ForceSecurityContextHolder.set(sc);
        
        ForceConnectorConfig cc = new ForceConnectorConfig();
        cc.setSessionId(sc.getSessionId());
        cc.setServiceEndpoint(sc.getEndPoint());
        cc.setSessionRenewer(this);

        try {
            ForceServiceConnector.setThreadLocalConnectorConfig(cc);
            request.setAttribute(FILTER_ALREADY_VISITED, Boolean.TRUE);
            chain.doFilter(new AuthenticatedRequestWrapper(request, sc), response);
        } catch (ForceOAuthSessionExpirationException e) {
            doOAuthLogin(request, response);
        } catch (SecurityException se) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, request.getRequestURI());
        } finally {
            try {
                request.removeAttribute(FILTER_ALREADY_VISITED);
            } finally {
                ForceSecurityContextHolder.release();
                ForceServiceConnector.setThreadLocalConnectorConfig(null);
            }
        }
    }

    /**
     * Sends the authentication redirect or saves the security context to the session depending
     * on which phase of the handshake we're in.
     * 
     * @param request
     * @param response
     * @throws IOException
     */
    private void doOAuthLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (ForceOAuthConnector.REDIRECT_AUTH_URI.equals(request.getServletPath())) {
            securityContextService.setSecurityContextToSession(
                    request,
                    response,
                    oauthConnector.getAccessToken(oauthConnector.getAccessCode(request),
                            oauthConnector.getRedirectUri(request)));
            response.sendRedirect(response.encodeRedirectURL(request.getParameter("state")));
        } else {
            response.sendRedirect(oauthConnector.getLoginRedirectUrl(request));
        }
    }

    /**
     * No resources to release.
     */
    @Override
    public void destroy() {  }

    public SecurityContextService getSecurityContextService() {
        return securityContextService;
    }

    /**
     * Wraps the request and provides methods to make the authenticated user information available.
     */
    private static final class AuthenticatedRequestWrapper extends HttpServletRequestWrapper {

        private final ForceUserPrincipal userP;
        private final ForceRolePrincipal roleP;

        public AuthenticatedRequestWrapper(HttpServletRequest request, SecurityContext sc) {
            super(request);
            this.userP = new ForceUserPrincipal(sc.getUserName(), sc.getSessionId());
            this.roleP = new ForceRolePrincipal(sc.getRole());
        }

        @Override
        public String getRemoteUser() {
            return userP != null ? userP.getName() : super.getRemoteUser();
        }

        @Override
        public Principal getUserPrincipal() {
            return userP != null ? userP : super.getUserPrincipal();
        }

        @Override
        public boolean isUserInRole(String role) {
            return roleP != null ? roleP.getName().endsWith(role) : super.isUserInRole(role);
        }
    }

    @Override
    public SessionRenewalHeader renewSession(ConnectorConfig config) throws ConnectionException {
        throw new ForceOAuthSessionExpirationException();
    }
    
    private void logout(
        ServletRequest request, ServletResponse response, SecurityContext sc, FilterChain chain)
        throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        
        ForceConnectorConfig config = new ForceConnectorConfig();
        try {
            config.setServiceEndpoint(sc.getEndPoint());
            config.setSessionId(sc.getSessionId());
            config.setSessionRenewer(this);
            ForceServiceConnector connector = new ForceServiceConnector();
            connector.setConnectorConfig(config);
            //logout from the partner API
            connector.getConnection().logout();
        } catch (ConnectionException e) {
            LOGGER.warn("Error logging out through API: ", e.getMessage());
            LOGGER.debug("Error logging out through API: ", e);
        }
        
        //clear the security context out of the security context holder
        ForceSecurityContextHolder.release();

        //Clear security context and cookies
        securityContextService.clearSecurityContext(req, res);

        if (logoutFromDatabaseCom) {
            String forceComLogoutUrl = getForceDotComLogoutUrl(req, sc, null);
            res.sendRedirect(res.encodeRedirectURL(forceComLogoutUrl));
        } else {
            chain.doFilter(request, response);
        }
    }
    
    private boolean isLogoutUrl(HttpServletRequest request) {
        
        if (logoutUrl != null
                && !"".equals(logoutUrl)
                && logoutUrl.equals(request.getServletPath())) {
            return true;
        }
        return false;
    }
    
    private String getForceDotComLogoutUrl(
            HttpServletRequest request, SecurityContext sc, String logoutTargetUrl) {
        
        return oauthConnector.getForceLogoutUrl(request, sc.getEndPoint(), logoutTargetUrl);
    }
}
