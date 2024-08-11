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

package com.force.sdk.oauth.connector;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.force.sdk.connector.ForceConnector;
import com.force.sdk.connector.ForceConnectorUtils;
import com.force.sdk.oauth.context.SecurityContext;
import com.force.sdk.oauth.userdata.UserDataRetrievalService;
import com.sforce.ws.ConnectionException;

/**
 * 
 * Main actor in the OAuth handshake. The other pieces of the OAuth integration
 * rely on the {@code ForceOAuthConnector} to handle the details of the OAuth flow.
 * <p>
 * In addition to the usual OAuth 2.0 protocol a {@code UserDataRetrievalService} is passed in and used to retrieve
 * data about the authenticated user after the handshake completes. This service can either be the standard 
 * service or an application specific one. This allows user data to be accessible to the application.
 * <p>
 * This object is used from a per application singleton. DO NOT store per request values with this class.
 *
 * @author Fiaz Hossain
 * @author Tim Kral
 */
public class ForceOAuthConnector implements ForceConnector {

    /**
     * OAuth spec version.
     *
     * @author Fiaz Hossain
     */
    public enum OAuthVersion {
        /**
         * OAuth version 2.0.
         */
        VERSION_2_0;
        
        private static OAuthVersion parse(String version) {
            if ("2.0".equals(version)) return VERSION_2_0;
            throw new UnsupportedOperationException("Unsupported OAuth version: " + version);
        }
    }
    
    /**
     * The url that is used for the authentication code callback.
     */
    public static final String REDIRECT_AUTH_URI = "/_auth";
    /**
     * The request attribute that the login redirect url will be read from.
     */
    public static final String LOGIN_REDIRECT_URL_ATTRIBUTE = "__login_redirect_url_attrbute__";

    private static final String LOCAL_HOST_PORT = "https://localhost:8443";
    
    private static final String ACCESS_TOKEN_JSON_KEY = "access_token";
    private static final String REFRESH_TOKEN_JSON_KEY = "refresh_token";
    private static final String INSTANCE_URL_JSON_KEY = "instance_url";
    
    // these two are application specific.
    private OAuthVersion version;
    
    // The connection info used to construct a connection
    private ForceOAuthConnectionInfo connInfo;
    
    // State that can be used to construct connInfo
    private String connectionName;               // Named connections can look up connection construction state        
    private ForceOAuthConnectionInfo externalConnInfo; // ForceOAuthConnectionInfo injected from an external source
    private UserDataRetrievalService userDataRetrievalService; // Service for retrieving data about the authenticated user
    private TokenRetrievalService tokenRetrievalService;
    
    /**
     * Default constructor. Sets OAuth version to 2.0. Uses standard
     * data retrieval service.
     */
    public ForceOAuthConnector() {
        this.version = OAuthVersion.VERSION_2_0;
        this.userDataRetrievalService = new UserDataRetrievalService();
        this.tokenRetrievalService = new TokenRetrievalServiceImpl();
    }
    
    /**
     * Sets OAuth version to 2.0. Takes in a {@code UserDataRetrievalService} so that
     * an extension can be used. 
     * 
     * @param userDataRetrievalService UserDataRetrievalService
     */
    public ForceOAuthConnector(UserDataRetrievalService userDataRetrievalService) {
        this.version = OAuthVersion.VERSION_2_0;
        this.userDataRetrievalService = userDataRetrievalService;
        this.tokenRetrievalService = new TokenRetrievalServiceImpl();
    }
    
    /**
     * Gets the access token for user. This gets called after an access code has been obtained.
     * That access code will now be exchanged for an access token or "auth token". Once an 
     * access token is obtained, it will immediately be used to retrieve data about the user to
     * populate a {@code SecurityContext}.
     * 
     * @param accessCode String
     * @param redirectUri String
     * @return a SecurityContext containing data about the authenticated user
     * @throws IOException i/o error
     */
    public SecurityContext getAccessToken(String accessCode, String redirectUri) throws IOException {
        StringBuffer urlParams = new StringBuffer("grant_type=authorization_code")
        .append("&code=").append(accessCode)
        .append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"));

        getConnInfo().appendOauthKeyParam(urlParams);
        getConnInfo().appendOauthSecretParam(urlParams);
        
        return createTokenInternal(urlParams.toString(), null /* refresh token */);
    }
    
    /**
     * Uses the refresh token to obtain a new auth token for the user.
     * 
     * @param refreshToken String
     * @return a SecurityContext containing data about the authenticated user
     * @throws IOException i/o error
     */
    public SecurityContext refreshAccessToken(String refreshToken) throws IOException {
        StringBuffer urlParams = new StringBuffer("grant_type=refresh_token")
        .append("&refresh_token=").append(refreshToken);

        getConnInfo().appendOauthKeyParam(urlParams);
        getConnInfo().appendOauthSecretParam(urlParams);
        
        return createTokenInternal(urlParams.toString(), refreshToken);
    }
    
    /**
     * Obtains an access token by calling the OAuth authentication endpoint and either trading an 
     * access code or refresh token for it.
     * 
     * The {@code UserDataRetrievalService} is called to retrieve data about the user
     * after the access token is obtained.
     * 
     * @param params String
     * @param refreshToken String
     * @return SecurityContext containing data about the authenticated user
     * @throws IOException i/o error
     */
    @SuppressWarnings("unchecked")
    private SecurityContext createTokenInternal(String params, String refreshToken) throws IOException {
        String instanceUrl = null;
        String session = null;
        String refresh = refreshToken;
        try {
            
            String responsePayload = tokenRetrievalService.retrieveToken(
                    getHostPort(connInfo.getEndpoint(), null), params, refreshToken, getConnInfo());
            
            JSONParser parser = new JSONParser();
            Map<String, String> map = (Map<String, String>) parser.parse(responsePayload);
            
            if ((session = map.get(ACCESS_TOKEN_JSON_KEY)) == null) {
                throw new IOException("Missing access token on response");
            }
            
            if (refresh == null) {
                if ((refresh = map.get(REFRESH_TOKEN_JSON_KEY)) == null) {
                    throw new IOException("Missing refresh token on response");
                }
            }
            
            
            if ((instanceUrl = map.get(INSTANCE_URL_JSON_KEY)) == null) {
                throw new IOException("Missing instance url on response");
            }
            
            SecurityContext sc =
                userDataRetrievalService.retrieveUserData(
                        session, ForceConnectorUtils.buildForceApiEndpoint(instanceUrl), refresh);
            
            return sc;
        } catch (ConnectionException ce) {
            throw new IOException("Unable to create token due to connection exception", ce);
        } catch (ParseException pe) {
            throw new IOException("Unable to create token due to parse exception", pe);
        }
    }
    
    /**
     * Closes the connection.
     */
    public void close() {
        this.connInfo = null;
        
        this.connectionName = null;
        
        this.externalConnInfo = null;
    }

    /**
     * Parses the access code in the servlet request.
     * 
     * @param request HttpServletRequest
     * @return OAuth access code
     */
    public String getAccessCode(HttpServletRequest request) {
        return request.getParameter("code");
    }

    /**
     * Builds the logout url.
     * 
     * @param request HttpServletRequest
     * @param forceEndpoint String
     * @param localLogoutSuccessfulPath String
     * @return the logout url
     */
    public String getForceLogoutUrl(HttpServletRequest request, String forceEndpoint, String localLogoutSuccessfulPath) {
        
        // Replace the force endpoint path with the logout path
        StringBuffer forceLogoutUrl =
            new StringBuffer(forceEndpoint.substring(0, forceEndpoint.indexOf('/', 9)) + "/secur/logout.jsp");
        
        //TODO: This doesn't work for multiple reasons. First the param is retURL. However fixing that makes
        //things worse because it won't let you redirect to a non-salesforce.com site.
        if (localLogoutSuccessfulPath != null && localLogoutSuccessfulPath.length() > 0) {
            try {
                forceLogoutUrl.append("?retUrl=").append(getHostPort(request))
                        .append(request.getContextPath())
                        .append(URLEncoder.encode(localLogoutSuccessfulPath, "UTF-8"));
                
                getConnInfo().appendOauthKeyParam(forceLogoutUrl);
            } catch (IOException e) {
                // log the error
            }
        }
        
        return forceLogoutUrl.toString();
    }

    /**
     * Returns the host and port of the endpoint.
     * 
     * @param request HttpServletRequest
     * @return the host and port string
     */
    public String getHostPort(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host == null) {
            return LOCAL_HOST_PORT;
        }
        
        return getHostPort(host, request.getScheme());
    }
    
    /**
     * Returns the host and port of the endpoint.
     * 
     * @param endpoint String
     * @param protocol String
     * @return the host and port string
     */
    public String getHostPort(String endpoint, String protocol) {
        String[] parsedEndpoint = endpoint.split("://", 2);
        String endpointWithoutProtocol = parsedEndpoint[parsedEndpoint.length - 1];
        
        if (protocol == null && parsedEndpoint.length == 2) {
            protocol = parsedEndpoint[0];
        }
        
        if (endpointWithoutProtocol.startsWith("localhost") || endpointWithoutProtocol.contains("internal")) {
            if (protocol != null) return protocol + "://" + endpointWithoutProtocol;
            
            return "http://" + endpointWithoutProtocol;
        }
        
        return "https://" + endpointWithoutProtocol;
    }
    
    /**
     * Gets the url that the user will be redirected to for authentication. This url should send the user to a login
     * screen so that they can enter their credentials. Once the user successfully authenticates, a callback will 
     * be received with the access code.
     * 
     * @param request HttpServletRequest
     * @return the url to redirect the user to
     * @throws IOException i/o error
     */
    public String getLoginRedirectUrl(HttpServletRequest request) throws IOException {
        String redirectAttribute = (String) request.getAttribute(LOGIN_REDIRECT_URL_ATTRIBUTE);
        
        // Build the state url parameter
        StringBuffer state;
        if (redirectAttribute != null) {
            state = new StringBuffer(redirectAttribute);
        } else {
            
            // host:post/contextPath/servletPath
            state = new StringBuffer(getHostPort(request))
                    .append(request.getContextPath())
                    .append(request.getServletPath());
            
            if (request.getPathInfo() != null) {
                state.append(request.getPathInfo());
            }

            if (request.getQueryString() != null) {
                state.append("?" + request.getQueryString());
            }
        }
        
        // Build the login redirect url
        StringBuffer loginRedirectUrl =
            new StringBuffer(getHostPort(getConnInfo().getEndpoint(), null)).append("/services/oauth2/authorize")
                .append("?response_type=code")
                .append("&redirect_uri=").append(URLEncoder.encode(getRedirectUri(request), "UTF-8"))
                .append("&state=").append(URLEncoder.encode(state.toString(), "UTF-8"));
    
        // Add the oauth key parameter
        getConnInfo().appendOauthKeyParam(loginRedirectUrl);
        
        return loginRedirectUrl.toString();
    }

    /**
     * Gets the URI to redirect to for user authentication.
     * 
     * @param request HttpServletRequest
     * @return the redirection URI
     */
    public String getRedirectUri(HttpServletRequest request) {
        return getHostPort(request) + request.getContextPath() + REDIRECT_AUTH_URI;
    }
    
    /**
     * Creates a connection info object. This returns the same output regardless
     * of whether the connector was supplied with a connection name that must be
     * resolved or directly given the oauth key, secret, and endpoint.
     * 
     * @return The force oauth connection info
     * @throws IOException i/o error
     */
    ForceOAuthConnectionInfo getConnInfo() throws IOException {
        if (connInfo == null) {
            if (this.externalConnInfo != null) {
                connInfo = this.externalConnInfo;
            } else if (this.connectionName != null) {
                connInfo = ForceOAuthConnectionInfo.loadFromName(this.connectionName);
            }
            
            // Out of options.  There's not enough here to construct theConnInfo.
            if (connInfo == null) {
                StringBuffer errorMsg = new StringBuffer();
                errorMsg.append("No state was found to construct an oauth connection.")
                        .append(" Please provide an endpoint, key and secret or connection url.");
            
                if (this.connectionName != null) {
                    errorMsg
                        .append(" Or create a classpath properties file, environment variable or java property for the name '")
                        .append(this.connectionName).append("'");
                }
            
                throw new IOException(errorMsg.toString());
            }
            
            connInfo.validate();
        }
        
        return connInfo;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }
    
    String getConnectionName() {
        return connectionName;
    }
    
    public void setConnectionInfo(ForceOAuthConnectionInfo connectionInfo) {
        this.externalConnInfo = connectionInfo;
    }

    public void setOAuthVersion(String oauthVersion) {
        this.version = OAuthVersion.parse(oauthVersion);
    }
    
    public OAuthVersion getOAuthVersion() {
        return this.version;
    }
    
    public void setUserDataRetrievalService(UserDataRetrievalService userDataRetrievalService) {
        this.userDataRetrievalService = userDataRetrievalService;
    }
    
    public void setTokenRetrievalService(TokenRetrievalService tokenRetrievalService) {
        this.tokenRetrievalService = tokenRetrievalService;
    }
}
