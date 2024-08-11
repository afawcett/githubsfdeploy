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

package com.force.sdk.springsecurity.config;

import java.util.List;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.w3c.dom.*;

import com.force.sdk.oauth.connector.ForceOAuthConnectionInfo;
import com.force.sdk.oauth.connector.ForceOAuthConnector;
import com.force.sdk.oauth.context.SecurityContextServiceImpl;
import com.force.sdk.oauth.context.store.SecurityContextCookieStore;
import com.force.sdk.oauth.context.store.SecurityContextSessionStore;
import com.force.sdk.oauth.userdata.CustomUserDataRetrievalService;
import com.force.sdk.oauth.userdata.UserDataRetrievalService;
import com.force.sdk.springsecurity.*;

/**
 * Parses the OAuth namespace and creates the required Spring configuration.
 * 
 * @author Fiaz Hossain
 * @author John Simone
 */
public class OAuthBeanDefinitionParser implements BeanDefinitionParser {

    // These are attributes of child nodes (oauthInfo, connectionUrl, connectionName)
    // that are used to build a ForceOAuthConnector
    private static final String ENDPOINT_ATTR = "endpoint";
    private static final String OAUTH_KEY_ATTR = "oauth-key";
    private static final String OAUTH_SECRET_ATTR = "oauth-secret";
    private static final String CONNECTION_URL_ATTR = "url";
    private static final String CONNECTION_NAME_ATTR = "name";

    private static final String DEFAULT_LOGIN_SUCCESS_ATTR = "default-login-success";
    private static final String DEFAULT_LOGOUT_SUCCESS_ATTR = "default-logout-success";
    private static final String LOGIN_URL_ATTR = "login-url";
    private static final String LOGOUT_URL_ATTR = "logout-url";
    private static final String LOGOUT_FROM_FORCE_DOT_COM_ATTR = "logout-from-force-dot-com";
    private static final String STORE_DATA_IN_SESSION = "store-data-in-session";
    private static final String SECURE_KEY_FILE = "secure-key-file";
    private static final String SECURE_KEY = "secure-key";
    private static final String STORE_USER_NAME = "store-user-name";

    private static final String OAUTH_CONNECTION_INFO_BEAN_NAME = "oauthConnectionInfo";
    private static final String OAUTH_CONNECTOR_BEAN_NAME = "oauthConnector";
    private static final String AUTH_PROVIDER_BEAN_NAME = "oauthAuthenticationProvider";
    private static final String AUTH_MANAGER_BEAN_NAME = "authenticationManager";
    private static final String AUTH_MANAGER_BEAN_ALIAS = AUTH_MANAGER_BEAN_NAME;
    private static final String AUTH_PROCESSING_ENTRY_POINT_BEAN_NAME = "authenticationProcessingFilterEntryPoint";
    private static final String AUTH_PROCESSING_FILTER_BEAN_NAME = "authenticationFilter";
    private static final String AUTH_LOGOUT_FILTER_BEAN_NAME = "logoutFilter";
    private static final String USER_DATA_RETRIEVAL_SERVICE_BEAN_NAME = "userDataRetrievalService";
    private static final String REMEMBER_ME_FILTER_BEAN_NAME = "rememberMeFilter";
    private static final String REMEMBER_ME_SERVICES_BEAN_NAME = "forceRememberMeServices";
    private static final String CONTEXT_STORAGE_SERVICE_NAME = "securityContextStorageService";
    private static final String CONTEXT_SERVICE_NAME = "securityContextService";
    private static final String CONNECTION_STORAGE_FILTER_BEAN_NAME = "connectionStorageFilter";

    private static final String ATT_ENTRY_POINT_REF = "entry-point-ref";
    private static final String ATT_POSITION = "position";
    private static final String ATT_AFTER = "after";
    private static final String ATT_REF = "ref";
    private static final String ELEM_CUSTOM_FILTER = "custom-filter";
    private static final String CREATE_SESSION = "create-session";

    private static final String NAME_OAUTH_INFO_ELEMENT = "oauthInfo";
    private static final String NAME_CONN_URL_ELEMENT = "connectionUrl";
    private static final String NAME_CONN_NAME_ELEMENT = "connectionName";
    private static final String NAME_CUSTOM_DATA_RETRIEVER_ELEMENT = "customUserDataRetriever";

    /**
     * Default constructor. No values are defaulted.
     */
    public OAuthBeanDefinitionParser() {
        
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {

        // Find the connection information child (either oauthInfo, connectionUrl or connectionName)
        NodeList children = element.getElementsByTagNameNS(element.getNamespaceURI(), "*");

        // validate that exactly one connection info element is defined
        validateConfiguration(children);

        parserContext.pushContainingComponent(new CompositeComponentDefinition(element.getTagName(), parserContext
                .extractSource(element)));

        // Create OAuthConnector bean first
        BeanDefinition oauthConnector = createOAuthConnector((Element) getConnectionNode(children), parserContext);
        // If there is a custom data retriever defined then create the necessary beans and
        // set in the OAuthConnector
        Element customDataRetrieverElement = (Element) getCustomDataRetrieverNode(children);
        if (customDataRetrieverElement != null) {
            parserContext.getRegistry().registerBeanDefinition(USER_DATA_RETRIEVAL_SERVICE_BEAN_NAME,
                    createCustomUserDataRetrievalService(customDataRetrieverElement, element));
            // set service in OAuthConnector
            oauthConnector.getPropertyValues().add("userDataRetrievalService",
                    new RuntimeBeanReference(USER_DATA_RETRIEVAL_SERVICE_BEAN_NAME));
        } else {
            parserContext.getRegistry().registerBeanDefinition(USER_DATA_RETRIEVAL_SERVICE_BEAN_NAME,
                    createUserDataRetrievalService(element));
        }
        parserContext.getRegistry().registerBeanDefinition(OAUTH_CONNECTOR_BEAN_NAME, oauthConnector);

        // Create OAuthAuthenticationProvider next
        parserContext.getRegistry().registerBeanDefinition(AUTH_PROVIDER_BEAN_NAME,
                createOAuthAuthenticationProvider(parserContext));

        // Create AuthenticationManager next
        String[] knownAlias = parserContext.getRegistry().getAliases(AUTH_MANAGER_BEAN_ALIAS);
        if (knownAlias == null || knownAlias.length == 0) {
            parserContext.getRegistry().registerBeanDefinition(BeanIds.AUTHENTICATION_MANAGER,
                    createAuthenticationManager(element, parserContext));
        }

        // Create AuthenticationProcessingFilterEntryPoint next
        parserContext.getRegistry().registerBeanDefinition(AUTH_PROCESSING_ENTRY_POINT_BEAN_NAME,
                createAuthenticationProcessingEntryPoint(parserContext));

        // Create SecurityContextStorage bean
        // this handles the storage of the security information into either a cookie or session
        // Create AuthenticationProcessingFilterEntryPoint next
        parserContext.getRegistry().registerBeanDefinition(CONTEXT_STORAGE_SERVICE_NAME,
                createSecurityContextStorageService(element));

        // Create SecurityContextService bean
        // this handles management of security information. It is a service facade to the userDataRetrieval
        // and securityContextStorage services
        parserContext.getRegistry().registerBeanDefinition(CONTEXT_SERVICE_NAME, createSecurityContextService());

        // Create RememberMeServices bean
        // This implements the remember me functionality which will handle the population of security
        // information from a browser cookie or server side session
        parserContext.getRegistry().registerBeanDefinition(REMEMBER_ME_SERVICES_BEAN_NAME, createRememberMeServices());

        // Create AuthenticationProcessingFilter
        // This filter is invoked after OAuth login to exchange token for an access token
        parserContext.getRegistry().registerBeanDefinition(AUTH_PROCESSING_FILTER_BEAN_NAME,
                createAuthenticationProcessingFilter(element, parserContext));

        // This filter is invoked during logout
        // sets this as the authentication filter over the default
        parserContext.getRegistry().registerBeanDefinition(AUTH_LOGOUT_FILTER_BEAN_NAME, createLogoutFilter(element));

        // This filter is invoked during the login process to process remember me tokens, which is
        // how the cookie based security information is stored
        parserContext.getRegistry().registerBeanDefinition(REMEMBER_ME_FILTER_BEAN_NAME, createRememberMeFilter());

        // This filter is invoked after the remember me filter. It ensures that the thread local connection variables
        // are set.
        parserContext.getRegistry().registerBeanDefinition(CONNECTION_STORAGE_FILTER_BEAN_NAME,
                createConnectionStorageFilter(element));

        // Now we configure http element if it's present. HTTP element should be a sibling so look for it.
        if (element.getParentNode() != null) {
            NodeList nodes = element.getParentNode().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getLocalName() != null && "http".equals(node.getLocalName().toLowerCase())) {
                    if (node.getAttributes().getNamedItem(ATT_ENTRY_POINT_REF) == null) {
                        // Add entry-point-ref since it's missing
                        addNodeAttribute(node, ATT_ENTRY_POINT_REF, AUTH_PROCESSING_ENTRY_POINT_BEAN_NAME);
                    }
                    if (node.getAttributes().getNamedItem(CREATE_SESSION) == null) {
                        // Add create-session="never" attribute
                        addNodeAttribute(node, CREATE_SESSION, "never");
                    }

                    /**
                     * Sets this as the authentication filter over the default <security:custom-filter
                     * position="FORM_LOGIN_FILTER" ref="authenticationFilter"/> <security:custom-filter
                     * position="LOGOUT_FILTER" ref="logoutFilter"/>
                     **/
                    setCustomFilterIfMissing(node, "FORM_LOGIN_FILTER", AUTH_PROCESSING_FILTER_BEAN_NAME, ATT_POSITION);
                    setCustomFilterIfMissing(node, "LOGOUT_FILTER", AUTH_LOGOUT_FILTER_BEAN_NAME, ATT_POSITION);
                    setCustomFilterIfMissing(node, "REMEMBER_ME_FILTER", REMEMBER_ME_FILTER_BEAN_NAME, ATT_POSITION);
                    setCustomFilterIfMissing(node, "REMEMBER_ME_FILTER", CONNECTION_STORAGE_FILTER_BEAN_NAME, ATT_AFTER);
                    break;
                }
            }
        }
        return null;
    }

    private void addNodeAttribute(Node node, String name, String value) {
        Attr newNode = node.getOwnerDocument().createAttributeNS(node.getNamespaceURI(), name);
        newNode.setNodeValue(value);
        node.getAttributes().setNamedItem(newNode);
    }

    private void setCustomFilterIfMissing(Node httpNode, String positionUpper, String ref, String attribute) {
        if (httpNode.hasChildNodes()) {
            NodeList children = httpNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                Node nn;
                if (node.getLocalName() != null && ELEM_CUSTOM_FILTER.equals(node.getLocalName().toLowerCase())
                        && (nn = node.getAttributes().getNamedItem(attribute)) != null && nn.getNodeValue() != null
                        && positionUpper.equals(nn.getNodeValue().toUpperCase())) {
                    // The filter already exists
                    return;
                }
            }
        }

        // Add custom-filter since it's missing
        int prefix = httpNode.getNodeName().indexOf(':');
        Node newNode = httpNode.getOwnerDocument().createElementNS(httpNode.getNamespaceURI(),
                prefix > 0 ? httpNode.getNodeName().substring(0, prefix + 1) + ELEM_CUSTOM_FILTER : ELEM_CUSTOM_FILTER);
        httpNode.appendChild(newNode);
        addNodeAttribute(newNode, attribute, positionUpper);
        addNodeAttribute(newNode, ATT_REF, ref);
    }

    private BeanDefinition createOAuthConnector(Element connectionInfo, ParserContext parser) {
        // Build a ForceOAuthConnectionInfo bean, if applicable
        BeanDefinition oauthConnInfo = null;
        if (NAME_OAUTH_INFO_ELEMENT.equals(connectionInfo.getLocalName())) {
            // Create a new ForceOAuthConnectionInfo bean and populate it with OAuth Info
            oauthConnInfo = new RootBeanDefinition(ForceOAuthConnectionInfo.class);
            oauthConnInfo.getPropertyValues().add("endpoint", connectionInfo.getAttribute(ENDPOINT_ATTR));
            oauthConnInfo.getPropertyValues().add("oauthKey", connectionInfo.getAttribute(OAUTH_KEY_ATTR));
            oauthConnInfo.getPropertyValues().add("oauthSecret", connectionInfo.getAttribute(OAUTH_SECRET_ATTR));
        } else if (NAME_CONN_URL_ELEMENT.equals(connectionInfo.getLocalName())) {
            // Create a new ForceOAuthConnectionInfo bean and populate it with Connection Url
            oauthConnInfo = new RootBeanDefinition(ForceOAuthConnectionInfo.class);
            oauthConnInfo.getPropertyValues().add("connectionUrl", connectionInfo.getAttribute(CONNECTION_URL_ATTR));
        }

        BeanDefinition oauthConnector = new RootBeanDefinition(ForceOAuthConnector.class);

        if (oauthConnInfo != null) {
            // Register ForceOAuthConnectionInfo bean and give it to the connector
            parser.getRegistry().registerBeanDefinition(OAUTH_CONNECTION_INFO_BEAN_NAME, oauthConnInfo);
            oauthConnector.getPropertyValues().add("connectionInfo", oauthConnInfo);
        } else if (NAME_CONN_NAME_ELEMENT.equals(connectionInfo.getLocalName())) {
            oauthConnector.getPropertyValues().add("connectionName", connectionInfo.getAttribute(CONNECTION_NAME_ATTR));
        } else {
            throw new RuntimeException("Unrecognized oauth connection information child element: "
                    + connectionInfo.getLocalName());
        }

        return oauthConnector;
    }

    private BeanDefinition createCustomUserDataRetrievalService(Element customDataRetrieverElement, Element mainElement) {
        String customDataRetrieverBeanName = customDataRetrieverElement.getAttribute("ref");
        String storeUsername = mainElement.getAttribute(STORE_USER_NAME);
        BeanDefinition customUserdataRetrievalService = new RootBeanDefinition(CustomUserDataRetrievalService.class);
        
        if ("false".equalsIgnoreCase(storeUsername)) {
            customUserdataRetrievalService.getPropertyValues().add("storeUsername", false);
        } else {
            customUserdataRetrievalService.getPropertyValues().add("storeUsername", true);
        }
        
        // set the custom user data retriever onto the service
        customUserdataRetrievalService.getPropertyValues().add("customDataRetriever",
                new RuntimeBeanReference(customDataRetrieverBeanName));
        return customUserdataRetrievalService;
    }

    private BeanDefinition createUserDataRetrievalService(Element mainElement) {
        BeanDefinition userdataRetrievalService = new RootBeanDefinition(UserDataRetrievalService.class);
        String storeUsername = mainElement.getAttribute(STORE_USER_NAME);

        if ("false".equalsIgnoreCase(storeUsername)) {
            userdataRetrievalService.getPropertyValues().add("storeUsername", false);
        } else {
            userdataRetrievalService.getPropertyValues().add("storeUsername", true);
        }
        
        return userdataRetrievalService;
    }

    private BeanDefinition createOAuthAuthenticationProvider(ParserContext parserContext) {
        BeanDefinition authProvider = new RootBeanDefinition(OAuthAuthenticationProvider.class);
        authProvider.getPropertyValues().add("oauthConnector", new RuntimeBeanReference(OAUTH_CONNECTOR_BEAN_NAME));
        return authProvider;
    }

    private BeanDefinition createAuthenticationManager(Element element, ParserContext parserContext) {
        BeanDefinition authManager = new RootBeanDefinition(ProviderManager.class);
        ConstructorArgumentValues constructor = authManager.getConstructorArgumentValues();
        List<BeanMetadataElement> providers = new ManagedList<BeanMetadataElement>();
        providers.add(new RuntimeBeanReference(AUTH_PROVIDER_BEAN_NAME));
        constructor.addIndexedArgumentValue(0, providers);
        parserContext.registerBeanComponent(new BeanComponentDefinition(authManager, AUTH_MANAGER_BEAN_NAME));
        // Add the alias
        parserContext.getRegistry().registerAlias(AUTH_MANAGER_BEAN_NAME, AUTH_MANAGER_BEAN_ALIAS);
        parserContext.getReaderContext().fireAliasRegistered(AUTH_MANAGER_BEAN_NAME, AUTH_MANAGER_BEAN_ALIAS,
                parserContext.extractSource(element));

        return authManager;
    }

    private BeanDefinition createAuthenticationProcessingEntryPoint(ParserContext parserContext) {
        BeanDefinition authEntyPoint = new RootBeanDefinition(AuthenticationProcessingFilterEntryPoint.class);
        authEntyPoint.getPropertyValues().add("oauthConnector", new RuntimeBeanReference(OAUTH_CONNECTOR_BEAN_NAME));
        return authEntyPoint;
    }

    private BeanDefinition createAuthenticationProcessingFilter(Element element, ParserContext parserContext) {
        BeanDefinition authFilter = new RootBeanDefinition(AuthenticationProcessingFilter.class);
        authFilter.getPropertyValues().add("authenticationManager", new RuntimeBeanReference("authenticationManager"));
        authFilter.getPropertyValues().add("authenticationSuccessHandler", createAuthenticationSuccessHandler(element));
        authFilter.getPropertyValues().add("oauthConnector", new RuntimeBeanReference(OAUTH_CONNECTOR_BEAN_NAME));
        String value = element.getAttribute(LOGIN_URL_ATTR);
        authFilter.getPropertyValues().add("filterProcessesUrl", StringUtils.hasText(value) ? value : "/spring/login");
        authFilter.getPropertyValues().add("authenticationEntryPoint",
                new RuntimeBeanReference(AUTH_PROCESSING_ENTRY_POINT_BEAN_NAME));
        authFilter.getPropertyValues().add("rememberMeServices",
                new RuntimeBeanReference(REMEMBER_ME_SERVICES_BEAN_NAME));
        return authFilter;
    }

    private BeanDefinition createAuthenticationSuccessHandler(Element element) {
        BeanDefinition authSuccess = new RootBeanDefinition(AuthenticationSuccessHandler.class);
        String value = element.getAttribute(DEFAULT_LOGIN_SUCCESS_ATTR);
        authSuccess.getPropertyValues().add("defaultTargetUrl", StringUtils.hasText(value) ? value : "/");
        return authSuccess;
    }

    private BeanDefinition createLogoutFilter(Element element) {
        BeanDefinition logout = new RootBeanDefinition(LogoutFilter.class);
        BeanDefinition logoutSuccessHandler = new RootBeanDefinition(LogoutSuccessHandler.class);
        String value = element.getAttribute(DEFAULT_LOGOUT_SUCCESS_ATTR);
        logoutSuccessHandler.getPropertyValues().add("defaultTargetUrl",
                StringUtils.hasText(value) ? value : "/spring/logoutSuccess");
        value = element.getAttribute(LOGOUT_FROM_FORCE_DOT_COM_ATTR);
        logoutSuccessHandler.getPropertyValues().add("logoutFromForceDotCom",
                StringUtils.hasText(value) ? value : "false");
        logoutSuccessHandler.getPropertyValues().add("oauthConnector",
                new RuntimeBeanReference(OAUTH_CONNECTOR_BEAN_NAME));
        logoutSuccessHandler.getPropertyValues().add("securityContextService",
                new RuntimeBeanReference(CONTEXT_SERVICE_NAME));
        List<BeanMetadataElement> logoutHandlers = new ManagedList<BeanMetadataElement>();
        logoutHandlers.add(new RootBeanDefinition(ForceLogoutHandler.class));
        logoutHandlers.add(new RootBeanDefinition(SecurityContextLogoutHandler.class));
        logout.getConstructorArgumentValues().addIndexedArgumentValue(0, logoutSuccessHandler);
        logout.getConstructorArgumentValues().addIndexedArgumentValue(1, logoutHandlers);
        value = element.getAttribute(LOGOUT_URL_ATTR);
        logout.getPropertyValues().add("filterProcessesUrl", StringUtils.hasText(value) ? value : "/spring/logout");
        return logout;
    }

    private BeanDefinition createRememberMeFilter() {
        BeanDefinition rememberMeFilter = new RootBeanDefinition(RememberMeAuthenticationFilter.class);
        ConstructorArgumentValues values = rememberMeFilter.getConstructorArgumentValues();
        values.addIndexedArgumentValue(0, new RuntimeBeanReference("authenticationManager"));
        values.addIndexedArgumentValue(1, new RuntimeBeanReference(REMEMBER_ME_SERVICES_BEAN_NAME));
        return rememberMeFilter;
    }

    private BeanDefinition createRememberMeServices() {
        BeanDefinition rememberMeServices = new RootBeanDefinition(ForceRememberMeServices.class);
        rememberMeServices.getPropertyValues().add("securityContextService",
                new RuntimeBeanReference(CONTEXT_SERVICE_NAME));
        return rememberMeServices;
    }

    private BeanDefinition createSecurityContextService() {
        BeanDefinition securityContextService = new RootBeanDefinition(SecurityContextServiceImpl.class);
        securityContextService.getPropertyValues().add("securityContextStorageService",
                new RuntimeBeanReference(CONTEXT_STORAGE_SERVICE_NAME));
        securityContextService.getPropertyValues().add("userDataRetrievalService",
                new RuntimeBeanReference(USER_DATA_RETRIEVAL_SERVICE_BEAN_NAME));
        return securityContextService;
    }

    private BeanDefinition createConnectionStorageFilter(Element element) {
        String storeDataInSession = element.getAttribute(STORE_DATA_IN_SESSION);
        BeanDefinition connectionStorageFilter = new RootBeanDefinition(ForceConnectionStorageFilter.class);
        if ("true".equalsIgnoreCase(storeDataInSession)) {
            connectionStorageFilter.getPropertyValues().add("useSession", Boolean.TRUE);
        } else {
            connectionStorageFilter.getPropertyValues().add("useSession", Boolean.FALSE);
        }
        connectionStorageFilter.getPropertyValues().add("oauthConnector",
                new RuntimeBeanReference(OAUTH_CONNECTOR_BEAN_NAME));
        return connectionStorageFilter;
    }

    private BeanDefinition createSecurityContextStorageService(Element element) {
        String storeDataInSession = element.getAttribute(STORE_DATA_IN_SESSION);
        String secureKeyFileName = element.getAttribute(SECURE_KEY_FILE);
        String secureKey = element.getAttribute(SECURE_KEY);
        BeanDefinition securityContextStorageService = null;
        if ("true".equalsIgnoreCase(storeDataInSession)) {
            securityContextStorageService = new RootBeanDefinition(SecurityContextSessionStore.class);
        } else {
            securityContextStorageService = new RootBeanDefinition(SecurityContextCookieStore.class);
            if(secureKey != null) {
            	securityContextStorageService.getPropertyValues().add("key", secureKey);
            } else {
            	securityContextStorageService.getPropertyValues().add("keyFileName", secureKeyFileName);
        	}
        }
        return securityContextStorageService;
    }

    private void validateConfiguration(NodeList children) {
        if (children.getLength() != 1 && children.getLength() != 2) { throw new RuntimeException(
                "<oauth> must specify exactly one of: <oauthInfo>, <connectionUrl> or <connectionName>");
        }

        int conectionElementCount = 0;

        for (int i = 0; i < children.getLength(); i++) {
            String name = children.item(i).getLocalName();

            if (isConnectionElementName(name)) {
                conectionElementCount++;
            }
        }

        if (conectionElementCount != 1) { throw new RuntimeException(
                "<oauth> must specify exactly one of: <oauthInfo>, <connectionUrl> or <connectionName>");
        }
    }

    private Node getConnectionNode(NodeList children) {
        for (int i = 0; i < children.getLength(); i++) {
            String name = children.item(i).getLocalName();

            if (isConnectionElementName(name)) { return children.item(i); }
        }
        return null;
    }

    private Node getCustomDataRetrieverNode(NodeList children) {
        for (int i = 0; i < children.getLength(); i++) {
            String name = children.item(i).getLocalName();

            if (isCustomDataRetrieverElementName(name)) { return children.item(i); }
        }
        return null;
    }

    private boolean isConnectionElementName(String name) {
        return (NAME_CONN_NAME_ELEMENT.equals(name) || NAME_OAUTH_INFO_ELEMENT.equals(name) || NAME_CONN_URL_ELEMENT
                .equals(name));
    }

    private boolean isCustomDataRetrieverElementName(String name) {
        return NAME_CUSTOM_DATA_RETRIEVER_ELEMENT.equals(name);
    }

}
