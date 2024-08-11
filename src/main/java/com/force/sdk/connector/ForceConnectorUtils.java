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

package com.force.sdk.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared utilities for Force.com connectors.
 *
 * @author Tim Kral
 */
public final class ForceConnectorUtils {

    /**
     * The file in which the cliforce client stores Force.com connection information.
     */
    static File cliforceConnFile =
        new File(System.getProperty("cliforce.home", System.getProperty("user.home")) + "/.force/cliforce_urls");

    static final Logger LOGGER = LoggerFactory.getLogger("com.force.sdk.connector");

    static final String FORCE_API_ENDPOINT_PATH;

    static final Map<String, Map<ForceConnectionProperty, String>> PROPERTIES_CACHE =
        new ConcurrentHashMap<String, Map<ForceConnectionProperty, String>>();

    static {
        try {
            FORCE_API_ENDPOINT_PATH = new URL(com.sforce.soap.partner.Connector.END_POINT).getPath();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to load Force.com API Endpoint path", e);
        }
    }

    private ForceConnectorUtils() {  }

    /**
     * Builds a valid Force.com API endpoint.
     * <p>
     * This method will pass through fully qualified, versioned Force.com
     * API endpoints. If the endpoint parameter is found not to be a
     * fully qualified Force.com API endpoint, then one will be constructed
     * using the API version bound to the Force.com SDK.
     * <p>
     * All built endpoints will be forced to use the https:// protocol unless
     * the host is {@code localhost} or contains the keyword "internal".
     *
     * @param endpoint a non {@code null}, non empty {@code String} representation
     *                 of a Force.com endpoint
     * @return a fully qualified, versioned Force.com API endpoint
     */
    public static String buildForceApiEndpoint(String endpoint) {

        // Strip off the protocol, if it exists
        String[] parsedEndpoint = endpoint.split("://");
        String endpointNoProtocol = parsedEndpoint[parsedEndpoint.length - 1];

        // This is the endpoint that will be supplied to the API
        StringBuffer apiEndpoint = new StringBuffer(endpointNoProtocol);

        // Append the web services path, if necessary
        if (!endpointNoProtocol.contains("/services/Soap/u")) {
            // Remove ending slash ('/') if necessary
            if (endpointNoProtocol.charAt(endpointNoProtocol.length() - 1) == '/') {
                apiEndpoint.deleteCharAt(endpointNoProtocol.length() - 1);
            }

            // Append the path (/services/Soap/u/<API version>) of the Connector endpoint
            apiEndpoint.append(FORCE_API_ENDPOINT_PATH);
        }

        // Prepend the protocol
        if (endpointNoProtocol.startsWith("localhost") || endpointNoProtocol.contains("internal")) {

            // Allow localhost and internal servers to keep the original protocol
            if (parsedEndpoint.length == 2) {
                apiEndpoint.insert(0, "://");
                apiEndpoint.insert(0, parsedEndpoint[0]);

                // Default localhost to http://
            } else {
                apiEndpoint.insert(0, "http://");
            }
        } else {
            // Default everything else to https://
            apiEndpoint.insert(0, "https://");
        }

        return apiEndpoint.toString();
    }

    /**
     * Returns true if var has some value enclosed in ${ and }; which means that user intended the value to be injected from
     * system variables or environment variables.
     * @param var any string.
     * @return true if var is enclosed by ${ and }
     */
    public static boolean isInjectable(String var) {
        return var != null && var.startsWith("${") && var.endsWith("}");
    }

    /**
     * This method will fetch and return the value of 'var' from system variables and if not found in system variables
     * then in environment variables.
     * @param var name of var enclosed by ${ and }
     * @return value of var
     */
    public static String extractValue(String var) {
        if (var != null)  {
            if (!var.startsWith("${") || !var.endsWith("}")) {
                return null;
            }

            var = var.replace("${" , "");
            var = var.substring(0, var.length() - 1);

            if (var != null) {
                if (System.getProperty(var) != null) {
                    LOGGER.info("Connection : loading " + var + " from Java System Properties");
                    return System.getProperty(var);
                }

                if (System.getenv(var) != null) {
                    LOGGER.info("Connection : loading " + var + " from Environment Variables");
                    return System.getenv(var);
                }
            }
        }

        return null;
    }

    /**
     * Loads Force.com connection properties from a connection name.
     * <p>
     * A connection name can represent connection properties in several
     * locations (and different formats). Connection properties will be
     * searched for in the following order:
     * <p>
     * <ol>
     *   <li>
     *   Force.com connection URL in an environment variable of the form FORCE_<CONNECTIONNAME>_URL 
     *   (case insensitive match)
     *   </li>
     *   <li>
     *   Force.com connection URL in a Java System property of the form force.<connectionName>.url 
     *   (case sensitive match)
     *   </li>
     *   <li>
     *   Force.com connection URL in a properties file on the classpath of the form <connectionName>.properties 
     *   (case sensitive match)
     *   </li>
     *   <li>
     *   key=value connection properties in a properties file on the classpath of the form <connectionName>.properties 
     *   (case sensitive match)
     *   </li>
     *   <li>
     *   Force.com connection URL in the cliforce connection file of the form <connectionName>=<connection URL> 
     *   (case sensitive match)
     *   </li> 
     * </ol>
     * 
     * @param connectionName a name representing one of the locations above
     * @return a {@link Map} which maps {@link ForceConnectionProperty} enum values to connection property values
     * @throws IOException if an attempt to interact with a classpath properties file or the cliforce connection
     *                     file results in a thrown {@code IOException}
     * @see ForceConnectionProperty
     */
    public static Map<ForceConnectionProperty, String> loadConnectorPropsFromName(String connectionName)
            throws IOException {

        if (connectionName == null) return null;

        if (PROPERTIES_CACHE.containsKey(connectionName)) {
            LOGGER.info("Connection : loading " + connectionName + " from cache");
            return PROPERTIES_CACHE.get(connectionName);
        }

        String connectionUrl;
        
        // First, try getting a connection url from an environment variable
        // Note: This is a case insensitive match
        String envVarName = "FORCE_" + connectionName.toUpperCase() + "_URL";
        if ((connectionUrl = System.getenv(envVarName)) != null) {
            LOGGER.info("Connection : Creating " + connectionName + " from environment variable: " + envVarName);
            return cache(connectionName, loadConnectorPropsFromUrl(connectionUrl));
        }

        // Next, try getting a connection url from a java system property
        // Note: This is a case sensitive match
        String sysPropName = "force." + connectionName + ".url";
        if ((connectionUrl = System.getProperty(sysPropName)) != null) {
            LOGGER.info("Connection : Creating " + connectionName + " from Java system property: " + sysPropName);
            return cache(connectionName, loadConnectorPropsFromUrl(connectionUrl));
        }

        // Next, look for a properties file on the classpath
        // Note: This is a case sensitive match
        URL propsFileUrl;
        if ((propsFileUrl = ForceConnectorUtils.class.getResource("/" + connectionName + ".properties")) != null) {
            LOGGER.info("Connection : Creating " + connectionName + " from classpath properties file: " + propsFileUrl);
            return cache(connectionName, loadConnectorPropsFromFile(propsFileUrl));
        }

        // Finally, look for a connection url in the cliforce connections file
        // Note: This is a case sensitive match
        if (cliforceConnFile.canRead()) {
            InputStream is = null;
            Properties cliforceConnUrls = new Properties();
            try {
                // Load up the connection urls from the cliforce connections file
                // These should be in the form: [connectionName]=[connectionUrl]
                is = new FileInputStream(cliforceConnFile);
                cliforceConnUrls.load(is);
            } finally {
                if (is != null) is.close();
            }

            if (cliforceConnUrls.containsKey(connectionName)) {
                LOGGER.info("Connection : Creating " + connectionName + " from cliforce connections file: " + cliforceConnFile);
                return cache(connectionName, loadConnectorPropsFromUrl(cliforceConnUrls.getProperty(connectionName)));
            }
        }

        return null;
    }

    static Map<ForceConnectionProperty, String> loadConnectorPropsFromFile(URL fileUrl) throws IOException {
        if (fileUrl == null) throw new IllegalArgumentException("Connector property file cannot be null.");

        Properties connectorProps = new Properties();
        InputStream is = null;
        try {
            is = fileUrl.openStream();
            connectorProps.load(is);
        } finally {
            if (is != null) is.close();
        }

        if (connectorProps.containsKey("url")) {
            return loadConnectorPropsFromUrl(connectorProps.getProperty("url"));
        }

        Map<ForceConnectionProperty, String> connectorPropMap =
                new HashMap<ForceConnectionProperty, String>(connectorProps.size());

        for (String propName : connectorProps.stringPropertyNames()) {
            ForceConnectionProperty connProp = ForceConnectionProperty.fromPropertyName(propName);

            if (connProp != null) {
                connectorPropMap.put(connProp, connectorProps.getProperty(propName));
            }
        }

        return connectorPropMap;
    }

    /**
     * Loads Force.com connection properties from a Force.com connection url
     * <p>
     * A Force.com connection url must start with the force:// protocol followed
     * by a non-empty endpoint string.  Connection properties are specified with
     * key=value pairs and delimited by a semi-colon (';') after the endpoint.
     * Any connection property keys not recognized (i.e. that are not in the 
     * {@link ForceConnectionProperty} enum) are ignored.
     * 
     * @param connectionUrl a well qualified Force.com connection URL
     * @return a {@link Map} that maps {@link ForceConnectionProperty} enum values to connection property values
     * @throws IllegalArgumentException if connectionUrl is {@code null}
     * @throws IllegalArgumentException if connectionUrl does not start with force://
     * @throws IllegalArgumentException if the endpoint within connectionUrl is empty
     * @throws IllegalArgumentException if the endpoint within connectionUrl is not valid
     * @see ForceConnectionProperty
     */
    public static Map<ForceConnectionProperty, String> loadConnectorPropsFromUrl(String connectionUrl) {

        if (connectionUrl == null) throw new IllegalArgumentException("Connection url cannot be null.");

        // Parse the connection url
        URI connectionUri;
        try {
            connectionUri = new URI(connectionUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to parse connection url (" + connectionUrl + ")", e);
        }

        // Basic validation of connection url
        if (!"force".equals(connectionUri.getScheme())) {
            throw new IllegalArgumentException("Illegal prefix for connection url (" + connectionUrl + "). "
                    + "It must start with force://");
        }
        
        Map<ForceConnectionProperty, String> connectorPropMap = new HashMap<ForceConnectionProperty, String>(3);

        // In a connection url, the endpoint won't be marked with a property key
        // so add it here first
        String endpoint = connectionUri.getHost();
        if (endpoint != null) {
            StringBuffer sb = new StringBuffer(endpoint);
            if (connectionUri.getPort() > -1) sb.append(":").append(connectionUri.getPort());
            if (connectionUri.getPath() != null) sb.append(connectionUri.getPath());
            
            endpoint = sb.toString();
        }
        
        ForceConnectionProperty.ENDPOINT.validateValue(endpoint, "Illegal connection url (" + connectionUrl + ").");
        connectorPropMap.put(ForceConnectionProperty.ENDPOINT, endpoint);

        String queryString = connectionUri.getQuery();
        if (queryString == null) return connectorPropMap;
        
        String[] parsedQueryString = connectionUri.getQuery().split("&");
        for (String queryParam : parsedQueryString) {
            String[] parsedUrlProperty = queryParam.split("=", 2);

            if (parsedUrlProperty.length == 2) {
                ForceConnectionProperty connProp = ForceConnectionProperty.fromPropertyName(parsedUrlProperty[0]);

                if (connProp != null) {
                    connectorPropMap.put(connProp, parsedUrlProperty[1]);
                }
            }
        }

        return connectorPropMap;
    }

    static int parsePortNumber(String port, int defaultPort) {
        int proxyPort = defaultPort;
        if (port != null) {
            try {
                proxyPort = Integer.parseInt(port);
            } catch (NumberFormatException ne) {
                LOGGER.warn("Unable to parse port '" + port + "'. Using default port number " + defaultPort, ne);
            }
        }

        return proxyPort;
    }

    /*
     * Adds properties to cache.  This is a workaround to using propertiesCache.put(K,V) directly.
     * When using put directly, it doesn't return the value to the caller.
     */
    private static Map<ForceConnectionProperty, String> cache(String connectionName, Map<ForceConnectionProperty, String> props) {
        PROPERTIES_CACHE.put(connectionName, props);
        return props;
    }

    /**
     * Empties the named connection's cache.
     */
    public static void clearCache() {
        PROPERTIES_CACHE.clear();
    }
}
