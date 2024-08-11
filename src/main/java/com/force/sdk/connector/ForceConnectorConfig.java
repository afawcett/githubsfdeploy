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

import static com.force.sdk.connector.ForceConnectorUtils.LOGGER;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import com.force.sdk.connector.logger.ForceLoggerStream;
import com.force.sdk.connector.logger.ForceLoggerStream.StreamLogger;
import com.sforce.ws.ConnectorConfig;

/**
 * A Force.com SDK wrapper for the Force.com API {@code ConnectorConfig}.
 * <p>
 * This wrapper stores the same state as a Force.com API {@code ConnectorConfig}
 * and can itself be used for a Force.com SOAP connection.  However, it provides
 * extra functionality that is useful for the Force.com SDK.  This functionality
 * includes:
 * <p>
 * <ul>
 *   <li>A unique {@code cacheId} that can be used for config caching</li>
 *   <li>Message tracing that is subject to a log TRACE level</li>
 *   <li>Force.com connection URL support</li>
 *   <li>Automatic SOAP version binding</li>
 *   <li>A {@code clientId} state for conveniently setting a Force.com connection identifier</li>
 * </ul>
 *
 * @author Tim Kral
 * @author Fiaz Hossain
 */
public class ForceConnectorConfig extends ConnectorConfig implements Cloneable {
    
    // Represents the minimum number of properties that must appear
    // in a Force.com connection URL (i.e. endpoint, username, password)
    private static final int MIN_CONN_URL_PROPS = 3;
    
    private boolean namespaceInitialized;
    private String namespace;
    private String cacheId;
    private String clientId;
    private PrintStream loggerStream;
    
    static ForceConnectorConfig loadFromName(String connectionName) throws IOException {
        
        Map<ForceConnectionProperty, String> propMap = ForceConnectorUtils.loadConnectorPropsFromName(connectionName);
        if (propMap == null) return null;
        
        ForceConnectorConfig config = new ForceConnectorConfig();
        config.setPropsFromMap(propMap, connectionName);
        return config;
    }
    
    /**
     * Initializes a {@code ForceConnectorConfig} object that contains no
     * connection properties.
     * <p>
     * This constructor will set the trace message flag to {@code true}, which means
     * that messaging tracing is controlled by setting the {@code com.force.sdk.connector}
     * log level to TRACE.
     * 
     * @see ForceConnectorConfig#setTraceMessage(boolean)
     */
    public ForceConnectorConfig() {
        setTraceMessage(true); // this is still subject to having logging level to trace
    }

    /**
     * Sets the Force.com authentication endpoint.
     * <p>
     * The authEndpoint parameter may be a fully qualified, 
     * versioned Force.com API endpoint.  If it is not, this
     * {@code ForceConnectorConfig} will attempt to build
     * a qualified Force.com API endpoint and bind the version
     * to the API version used in the Force.com SDK.
     * <p>
     * 
     * @param authEndpoint representation of a Force.com authentication endpoint
     *                     (This endpoint may be a fully qualified Force.com API
     *                     endpoint, but need not be)
     *  
     */
    @Override
    public void setAuthEndpoint(String authEndpoint) {
        // We allow non-fully qualified API endpoints. In that
        // case, we should automatically bind to the API version
        // found in the API dependencies 
        super.setAuthEndpoint(ForceConnectorUtils.buildForceApiEndpoint(authEndpoint));
        initCacheId();
    }

    private void initCacheId() {
        String username = getUsername();
        String endpoint = getAuthEndpoint();
        
        if (username != null && endpoint != null) {
            this.cacheId = username + endpoint;
        } else {
            this.cacheId = null;
        }
    }

    String getCacheId() {
        return cacheId;
    }

    /**
     * Returns the Force.com connection client id.
     * <p>
     * The client id is a {@code String} identifier which
     * will be set on the Force.com connection created
     * from this {@code ForceConnectorConfig}
     *
     * @return the Force.com connection client id
     */
    public String getClientId() {
        return this.clientId;
    }

    /**
     * Sets the Force.com connection client id.
     * <p>
     * The client id is a {@code String} identifier which
     * will be set on the Force.com connection created
     * from this {@code ForceConnectorConfig}
     *
     * @param clientId any non {@code null}, non empty {@code String} that is
     *                 to be used as a Force.com connection identifier
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    /**
     * Parses a Force.com connection URL and  sets the connection
     * properties found within.
     * 
     * @param connectionUrl A Force.com connection URL
     * @throws IllegalArgumentException if the Force.com connection URL does not
     *                                  contain a parseable endpoint, username, and password
     * @throws IllegalArgumentException if any of the connection property values are not valid
     * @see ForceConnectionProperty
     */
    public void setConnectionUrl(String connectionUrl) {
        Map<ForceConnectionProperty, String> propMap = ForceConnectorUtils.loadConnectorPropsFromUrl(connectionUrl);
        if (propMap == null || propMap.size() < MIN_CONN_URL_PROPS) {
            throw new IllegalArgumentException("The connection url (" + connectionUrl + ") must contain at least three parts. "
                                                + "It should be in the form force://<endPoint>?user=<user>&password=<password>");
        }
        
        setPropsFromMap(propMap, connectionUrl);
    }
    
    boolean isNamespaceInitialized() {
        return this.namespaceInitialized;
    }

    void setNamespace(String namespace) {
        this.namespace = namespace;
        this.namespaceInitialized = true;
    }

    String getNamespace() {
        return this.namespace;
    }

    private void setPropsFromMap(Map<ForceConnectionProperty, String> propMap, String propSource) {
        String errorMessage = "Could not load valid ForceConnectorConfig properties from " + propSource + ".";
        
        String endpointValue = propMap.get(ForceConnectionProperty.ENDPOINT);
        ForceConnectionProperty.ENDPOINT.validateValue(endpointValue, errorMessage);
        setAuthEndpoint(endpointValue);
        
        String userValue = propMap.get(ForceConnectionProperty.USER);
        ForceConnectionProperty.USER.validateValue(userValue, errorMessage);
        setUsername(userValue);
        
        String passwordValue = propMap.get(ForceConnectionProperty.PASSWORD);
        ForceConnectionProperty.PASSWORD.validateValue(passwordValue, errorMessage);
        setPassword(passwordValue);

        String clientIdValue;
        if ((clientIdValue = propMap.get(ForceConnectionProperty.CLIENTID)) != null) {
            ForceConnectionProperty.CLIENTID.validateValue(clientIdValue, errorMessage);
            setClientId(clientIdValue);
        }
        
        String timeoutValue;
        if ((timeoutValue = propMap.get(ForceConnectionProperty.TIMEOUT)) != null) {
            ForceConnectionProperty.TIMEOUT.validateValue(timeoutValue, errorMessage);
            int timeout = Integer.valueOf(timeoutValue);
            if (timeout > 0) setReadTimeout(timeout);
        }
    }
    
    /**
     * Turns on the ability for this {@code ForceConnectorConfig} to
     * trace messages.
     * <p>
     * Ultimately, message tracing is controlled by setting the {@code com.force.sdk.connector}
     * log level to TRACE. However, this granular control can be removed by setting
     * the trace message flag to {@code false} here.
     * <p>
     * When a {@code ForceConnectorConfig} object is constructed, this flag is
     * set to {@code true}
     * 
     * @param traceMessage the trace message flag for this {@code ForceConnectorConfig}
     */
    @Override
    public void setTraceMessage(boolean traceMessage) {
        // Check if we are at tracing level and only allow if true
        if (traceMessage && LOGGER.isTraceEnabled()) {
            this.loggerStream = new PrintStream(new ForceLoggerStream(new StreamLogger() {
                @Override
                public void log(String msg) {
                    LOGGER.trace(msg);
                } }));
        } else {
            loggerStream = null;
        }
        super.setTraceMessage(loggerStream != null);
    }
    
    /**
     * Sets the Force.com connection username.
     * <p>
     * This is overridden here because the Force.com connection
     * username is used in part to generate the unique cache id
     * for this {@code ForceConnectorConfig}
     * 
     * @param username the username to be used in a Force.com connection
     */
    @Override
    public void setUsername(String username) {
        super.setUsername(username);
        initCacheId();
    }

    /**
     * Returns the {@code PrintStream} to which the
     * Force.com connection will print traced API messages.
     * 
     * @return a {@code PrintStream} to a {@code com.force.sdk.connector}
     *         log if that log is set to the TRACE level; otherwise,
     *         a {@code PrintStream} to a trace file set on this
     *         {@code ForceConnectorConfig}; otherwise {@code System.out}
     */
    @Override
    public PrintStream getTraceStream() {
        if (loggerStream != null) {
            return loggerStream;
        }
        return super.getTraceStream();
    }
    
    /**
     * Returns a hash code for this {@code ForceConnectorConfig}.
     * <p>
     * The hash code is computed by finding the hash code
     * of this {@code ForceConnectorConfig}'s unique cacheId.
     * 
     * @return a hash code for this {@code ForceConnectorConfig}
     */
    @Override
    public int hashCode() {
        return cacheId.hashCode();
    }
    
    /**
     * Compares this {@code ForceConnectorConfig} to the specified object.
     * <p>
     * The result is {@code true} if and only if the argument 
     * is not {@code null} and is a {@code ForceConnectorConfig} object
     * with the same unique cacheId as this {@code ForceConnectorConfig}.
     * 
     * @param obj the object to compare this {@code ForceConnectorConfig} against
     * @return {@code true} if the {@code ForceConnectorConfig}s are equal; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        
        if (obj == null) return false;
        if (!(obj instanceof ForceConnectorConfig)) return false;
        
        return cacheId.equals(((ForceConnectorConfig) obj).cacheId);
    }
    
    /**
     * Makes a copy of this {@code ForceConnectorConfig}.
     * 
     * @return a non {@code null} copy of this {@code ForceConnectorConfig}
     * @throws RuntimeException if a {@code CloneNotSupportedException} is caught from
     *                          {@code java.lang.Object}
     */
    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
