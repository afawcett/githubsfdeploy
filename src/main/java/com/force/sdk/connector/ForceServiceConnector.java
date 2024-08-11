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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.force.sdk.connector.threadlocal.ForceThreadLocalStore;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.CallOptions_element;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.SessionHeader_element;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.SessionRenewer;

/**
 * A connector to the Force.com service.
 * <p>
 * This connector will manage and provide connections to the Force.com service. It can
 * provide the following connection types:
 * <p>
 * <ul>
 *   <li>PartnerConnection - Used for SOAP API calls</li>
 *   <li>MetadataConnection - Used for the Force.com Metadata API</li>
 *   <li>BulkConnection - Used for the Force.com Bulk API</li>
 * </ul>
 * Connectors require a {@link ForceConnectorConfig} to provide connection
 * properties.  They can handle {@code ForceConnectorConfig}s from the following
 * sources (Note that sources will be checked in this order):
 * <p>
 * <ol>
 *   <li>An injected {@code ForceConnectorConfig}</li>
 *   <li>A named source (see {@link ForceConnectorUtils#loadConnectorPropsFromName(String)})</li>
 *   <li>A {@code ForceConnectorConfig} stored in the connector's {@code ThreadLocal} cache</li>
 * </ol>
 * Connectors are capable of caching {@code ForceConnectorConfig} objects both in a {@code ThreadLocalCache}
 * and an in-memory cache.  The {@code ThreadLocalCache} is directly controlled by the caller.  The in-memory
 * cache is controlled internally by the connector.  By default, a connector will cache a {@code ForceConnectorConfig}
 * in memory whenever it constructs a connection.  However, this can be turned off by the caller.
 * <p>
 * A {@code ForceServiceConnector} provides several additional features beyond the traditional
 * Force.com API {@code Connector}.  Namely:
 * <p>
 * <ul>
 *   <li>
 *   Force.com connection endpoint building with automatic version binding 
 *   (see {@link ForceConnectorConfig#setAuthEndpoint(String)} and {@link ForceConnectorUtils#buildForceApiEndpoint(String)})
 *   </li>
 *   <li>Automatic session renewal</li>
 *   <li>Convenient setting of Force.com connection client identifiers (clientId)</li>
 * </ul>
 *
 * @author Tim Kral
 * @author Fiaz Hossain
 */
public class ForceServiceConnector implements ForceConnector, SessionRenewer {

    /**
     * The qualified name for session SOAP requests.
     */
    public static final javax.xml.namespace.QName SESSION_HEADER_QNAME =
        new javax.xml.namespace.QName("urn:partner.soap.sforce.com", "SessionHeader");
    
    // This is used to create a Metadata API uri (i.e. */services/Soap/u/* -> */services/Soap/m/*)
    private static final Pattern METADATA_URI_PATTERN = Pattern.compile("(.*/Soap)/./(.*)");
    
    // This is used to create a Bulk (REST) API uri (i.e. */services/Soap/u/*/<orgId> -> */services/async/*)
    // The Bulk API was the first "REST API", hence the var name. This is different from the REST API, which came later.
    private static final Pattern RESTAPI_URI_PATTERN = Pattern.compile("(.*)/Soap/./(.*)/(.*)$");
    
    // Version under which we will get Metadata describe results
    static final double DESCRIBE_METADATA_VERSION = 16.0;
    
    private static final Proxy DEFAULT_PROXY;

    /**
     * The version of the sdk e.g. sdk-22.0.1-BETA that will be set in the User-Agent header for API requests
     */
    public static final String API_USER_AGENT = "andyinthecloud-sdk-61";

    static {
        if (PROXY_HOST != null) {
            SocketAddress addr = new InetSocketAddress(PROXY_HOST, PROXY_PORT);
            DEFAULT_PROXY = new Proxy(Proxy.Type.HTTP, addr);
        } else {
            DEFAULT_PROXY = null;
        }

        /**
        String sdkVersion = null;
        Properties projectProps = new Properties();
        try {
            projectProps.load(ForceServiceConnector.class.getClassLoader().getResource("sdk.properties").openStream());
            sdkVersion = projectProps.getProperty("force.sdk.version");
        } catch (IOException e) {
            LOGGER.error("Unable to load project properties. Logs will not include sdk version!");
        }
        if (sdkVersion == null) sdkVersion = "";
        API_USER_AGENT = String.format("sdk-%s", sdkVersion);
         */
    }
    
    /**
     * Retrieves the {@code ForceConnectorConfig} found in the
     * {@code ForceServiceConnector} {@code ThreadLocal} cache.
     * 
     * @return the {@code ForceConnectorConfig} from the {@code ThreadLocal};
     *         {@code null} if no such {@code ForceConnectorConfig} exists
     */
    public static ForceConnectorConfig getThreadLocalConnectorConfig() {
        return ForceThreadLocalStore.getConnectorConfig();
    }

    /**
     * Sets the {@code ForceConnectorConfig} in the {@code ForceServiceConnector}
     * {@code ThreadLocal}.
     * 
     * @param config the {@code ForceConnectorConfig} to be set in the {@code ThreadLocal}
     */
    public static void setThreadLocalConnectorConfig(ForceConnectorConfig config) {
        ForceThreadLocalStore.setConnectorConfig(config);
    }
    
    // Cache for saved configs
    private static final Map<String, ForceConnectorConfig> CACHED_CONFIGS =
        new ConcurrentHashMap<String, ForceConnectorConfig>();
    // map a connection name to a config id
    private static final Map<String, String> CONN_NAME_TO_CACHED_CONFIGS = new ConcurrentHashMap<String, String>();
    private boolean skipCache = false; // Flag which tells us whether to check the config cache or not
    
    // The ForceConnectorConfig used to construct a connection
    private ForceConnectorConfig config;
    
    // State that can be used to construct config
    private String connectionName;               // Named connections can look up connection construction state        
    private ForceConnectorConfig externalConfig; // ForceConnectorConfig injected from an external source

    // Extra state used to initialize config (and the connection)
    private String clientId;
    private String externalClientId; // A clientId coming from an external source (see getConfig)

    private int timeout;
    
    // Saved connection state for multiple getConnection calls
    private PartnerConnection connection;
    private MetadataConnection metadataConnection;
    private BulkConnection bulkConnection;
    
    /**
     * Initializes a {@code ForceServiceConnector} with no {@code ForceConnectorConfig} source.
     * <p>
     * If using this constructor, the caller must provider a {@code ForceConnetorConfig} source
     * via setters before getting a Force.com connection.
     * 
     * @see ForceServiceConnector#setConnectorConfig(ForceConnectorConfig)
     * @see ForceServiceConnector#setConnectionName(String)
     * @see ForceServiceConnector#setThreadLocalConnectorConfig(ForceConnectorConfig)
     */
    public ForceServiceConnector() {  }
    
    /**
     * Initializes a {@code ForceServiceConnector} with a named {@code ForceConnectorConfig} source .
     * 
     * @param connectionName the named {@code ForceConnectorConfig} source from which a valid {@code ForceConnectorConfig}
     *                       can be constructed and used to get Force.com connections
     * @see ForceConnectorUtils#loadConnectorPropsFromName(String)
     */
    // Constructor that will retrieve saved connection construction state
    public ForceServiceConnector(String connectionName) {
        this.connectionName = connectionName;
    }

    /**
     * Initializes a {@code ForceServiceConnector} that uses the given {@code ForceConnectorConfig}
     * to get Force.com connections.
     * 
     * @param config the {@code ForceConnectorConfig} to be used when getting Force.com connections
     * @throws IllegalArgumentException if the given {@code ForceConnectorConfig} is {@code null}
     * @throws ConnectionException if the given {@code ForceConnectorConfig} is incomplete (i.e.
     *                             cannot be used to get a Force.com connection)
     */
    public ForceServiceConnector(ForceConnectorConfig config) throws ConnectionException {
        if (config == null) {
            throw new IllegalArgumentException("Cannot construct ForceServiceConnector with null ConnectorConfig.");
        }
        
        // Fail early if the ConnectorConfig is not valid
        validateConnectorConfig(config);
        this.externalConfig = config;
    }
    
    /**
     * Returns a Force.com API {@code PartnerConnection}.
     * <p>
     * This connection type can be use to make SOAP API calls to the Force.com
     * service.  The {@code PartnerConnection} will be lazily constructed and
     * stored in this {@code ForceServiceConnector}'s state.  This state
     * can be cleared with a call to {@code close}
     * 
     * @return a Force.com API {@code PartnerConnection} from the {@code ForceConnectorConfig}
     *         state found in this {@code ForceServiceConnector}
     * @throws ConnectionException if the {@code ForceConnectorConfig} state cannot be found
     *                             or otherwise constructed
     * @throws ConnectionException if this {@code ForceServiceConnector} cannot get a connection
     *                             to the Force.com service with its {@code ForceConnectorConfig} state
     * @see ForceServiceConnector#renewSession(ConnectorConfig)
     * @see ForceServiceConnector#close
     */
    public PartnerConnection getConnection() throws ConnectionException {
        if (this.connection == null) {
            initConnection();
        }
        
        return this.connection;
    }

    private void initConnection() throws ConnectionException {
        if (config == null) {
            config = getConfig();
            initConfig();
        }

        config.setRequestHeader("User-Agent", API_USER_AGENT);
        this.connection = Connector.newConnection(config);

        CallOptions_element co = new CallOptions_element();
        
        // Give the connection a client id if we have one
        if (this.clientId != null) {
            co.setClient(this.clientId);
        } else if (this.externalClientId != null) {
            // Check for any external client id we might
            // have come across (see getConfig)
            co.setClient(this.externalClientId);
        } else {
            co.setClient(API_USER_AGENT); //just default it to the version of the sdk
        }

        this.connection.__setCallOptions(co);
    }
    
    // Constructs and returns config.
    private ForceConnectorConfig getConfig() throws ConnectionException {
        
        // First, look for construction state in this object's state.
        if (externalConfig != null) {
             // Save the client id for possible later use (see initConnection)
            externalClientId = externalConfig.getClientId();
            // Clone the config in case it gets cached.  This will prevent
            // the caller from modifying the cached object.
            return checkConfigCache((ForceConnectorConfig) externalConfig.clone());
        }
        
        // Next, try to retrieve saved connection construction state using the connection name
        if (connectionName != null) {
            final ForceConnectorConfig cachedConfig = getCachedConfig(getCacheIdForConnectionName(connectionName));
            if (cachedConfig != null) {
                return cachedConfig;
            }

            ForceConnectorConfig loadedConfig;
            try {
                loadedConfig = ForceConnectorConfig.loadFromName(connectionName);
            } catch (IOException e) {
                throw new ConnectionException("Unable to load ForceConnectorConfig for name " + connectionName, e);
            }
            
            if (loadedConfig != null) {
                // Save the client id for possible later use (see initConnection)
                externalClientId = loadedConfig.getClientId();
                return checkConfigCache(loadedConfig, connectionName);
            }
        }
        
        // Finally, try to get config from ThreadLocal
        if ((config = ForceThreadLocalStore.getConnectorConfig()) != null) return config;
        
        // Out of options.  There's not enough here to construct config.
        StringBuffer errorMsg = new StringBuffer();
        errorMsg.append("No state was found to construct a connection.")
                .append(" Please provide a ForceConnectorConfig.");
        
        if (connectionName != null) {
            errorMsg.append(" Or create a classpath properties file, environment variable or java property for the name '")
                    .append(connectionName).append("'");
        }

        throw new ConnectionException(errorMsg.toString());
    }

    private String getCacheIdForConnectionName(String cachedConnectionName) {
        return CONN_NAME_TO_CACHED_CONFIGS.get(cachedConnectionName);
    }

    private void setCacheIdForConnectionName(String cacheConnectionName, String cacheId) {
        if (cacheConnectionName != null && cacheId != null) {
            LOGGER.trace("ForceServiceConnector Cache: Mapping connectionName: "
                    + cacheConnectionName + " to cacheId: " + cacheId);
            CONN_NAME_TO_CACHED_CONFIGS.put(cacheConnectionName, cacheId);
        }
    }

    private ForceConnectorConfig checkConfigCache(ForceConnectorConfig configToCheck) throws ConnectionException {
        return checkConfigCache(configToCheck, null);
    }

    private ForceConnectorConfig checkConfigCache(ForceConnectorConfig configToCheck, String cachedConnectionName)
            throws ConnectionException {
        validateConnectorConfig(configToCheck);

        String cacheId = configToCheck.getCacheId();
        if (cacheId != null && !skipCache) {
            LOGGER.trace("ForceServiceConnector Cache: Checking for id: " + cacheId);
            
            ForceConnectorConfig cachedConfig = getCachedConfig(cacheId);
            setCacheIdForConnectionName(cachedConnectionName, cacheId);
            if (cachedConfig  != null) {
                LOGGER.trace("ForceServiceConnector Cache: HIT for id: " + cacheId);
                return cachedConfig;
            }

            LOGGER.trace("ForceServiceConnector Cache: MISS for id: " + cacheId);
            CACHED_CONFIGS.put(cacheId, configToCheck);
        }
        
        return configToCheck;
    }

    private void validateConnectorConfig(ForceConnectorConfig configToValidate) throws ConnectionException {
        if (configToValidate.getSessionId() == null && configToValidate.getAuthEndpoint() == null) {
            throw new ConnectionException("ForceConnectorConfig must have an AuthEndpoint");
        }
        
        if (configToValidate.getSessionId() == null && configToValidate.getUsername() == null) {
            throw new ConnectionException("ForceConnectorConfig must have a Username");
        }
    }

    // Initiates config.  We assume that config is non-null.
    private void initConfig() {
        if (config.getSessionRenewer() == null) {
            config.setSessionRenewer(this);
        }
        
        // Add proxy information to config
        if (DEFAULT_PROXY != null && (config.getProxy() != Proxy.NO_PROXY || config.getProxy().type() == Type.DIRECT)) {
            config.setProxy(DEFAULT_PROXY);
        }
        
        // Transfer any extra state we have to config
        if (this.timeout > 0) config.setReadTimeout(this.timeout);
    }

    /**
     * Returns a Force.com API {@code MetadataConnection}.
     * <p>
     * This connection type can be use to make Force.com Metadata API calls.
     * The {@code MetadataConnection} will be lazily constructed and
     * stored in this {@code ForceServiceConnector}'s state.  This state
     * can be cleared with a call to {@code close}
     * 
     * @return a Force.com API {@code MetadataConnection} from the {@code ForceConnectorConfig}
     *         state found in this {@code ForceServiceConnector}
     * @throws ConnectionException if the {@code ForceConnectorConfig} state cannot be found
     *                             or otherwise constructed
     * @throws ConnectionException if this {@code ForceServiceConnector} cannot get a connection
     *                             to the Force.com service with its {@code ForceConnectorConfig} state
     * @see ForceServiceConnector#renewSession(ConnectorConfig)
     * @see ForceServiceConnector#close
     */
    public MetadataConnection getMetadataConnection() throws ConnectionException {
        if (this.metadataConnection == null) {
            initMetadataConnection();
        }
        return this.metadataConnection;
    }

    private void initMetadataConnection() throws ConnectionException {
        if (this.connection == null) {
            initConnection();
        }
        
        ConnectorConfig configNew = new ConnectorConfig();
        configNew.setSessionId(config.getSessionId());
        configNew.setServiceEndpoint(METADATA_URI_PATTERN.matcher(config.getServiceEndpoint()).replaceFirst("$1/m/$2"));

        this.metadataConnection = new MetadataConnection(configNew);

        // Give the metadata connection a client id if we have one
        if (this.clientId != null) {
            this.metadataConnection.setCallOptions(this.clientId);
        } else if (this.externalClientId != null) {
            // If we've constructed config from ForceServiceConnectionInfo state
            // then we've saved any client id for use here (see getConfig)
            this.metadataConnection.setCallOptions(this.externalClientId);
        } else {
            this.metadataConnection.setCallOptions(API_USER_AGENT); //just default to sdk version
        }
        this.metadataConnection.getConfig().setRequestHeader("User-Agent", API_USER_AGENT);
    }

    /**
     * Returns a Force.com Bulk API {@code BulkConnection}.
     * <p>
     * This connection type can be use to make Force.com Bulk API calls.
     * The {@code BulkConnection} will be lazily constructed and
     * stored in this {@code ForceServiceConnector}'s state.  This state
     * can be cleared with a call to {@code close}
     * 
     * @return a Force.com API {@code BulkConnection} from the {@code ForceConnectorConfig}
     *         state found in this {@code ForceServiceConnector}
     * @throws ConnectionException if the {@code ForceConnectorConfig} state cannot be found
     *                             or otherwise constructed
     * @throws ConnectionException if this {@code ForceServiceConnector} cannot get a connection
     *                             to the Force.com service with its {@code ForceConnectorConfig} state
     * @throws AsyncApiException if this {@code ForceServiceConnector} cannot load a {@code BulkConnection}
     *                           to the Force.com service with its {@code ForceConnectorConfig} state
     * @see ForceServiceConnector#renewSession(ConnectorConfig)
     * @see ForceServiceConnector#close
     */
    public BulkConnection getBulkConnection() throws ConnectionException, AsyncApiException {
        if (this.bulkConnection == null) {
            initBulkConnection();
        }
        return this.bulkConnection;
    }

    private void initBulkConnection() throws ConnectionException, AsyncApiException {
        if (this.connection == null) {
            initConnection();
        }
        
        ConnectorConfig configNew = new ConnectorConfig();
        configNew.setSessionId(config.getSessionId());
        configNew.setServiceEndpoint(config.getServiceEndpoint());
        configNew.setRestEndpoint(RESTAPI_URI_PATTERN.matcher(config.getServiceEndpoint()).replaceFirst("$1/async/$2/"));
        this.bulkConnection = new BulkConnection(configNew);
    }

    /**
     * Returns the namespace of the Force.com store to which this {@code ForceServiceConnector}
     * is getting a connection.
     * <p>
     * A namespace uniquely identifies a Force.com store on the Force.com service.  This {@code ForceServiceConnector}
     * will retrieve the namespace by getting a Force.com connection with its {@code ForceConnectorConfig} state.
     * 
     * @return the unique Force.com store namespace; {@code null} is no such namespace exists 
     * @throws ConnectionException if the {@code ForceConnectorConfig} state cannot be found
     *                             or otherwise constructed
     * @throws ConnectionException if this {@code ForceServiceConnector} cannot get a connection
     *                             to the Force.com service with its {@code ForceConnectorConfig} state
     * @throws ConnectionException if the namespace cannot be retrieved over the Force.com connection
     */
    public String getNamespace() throws ConnectionException {
        if (metadataConnection == null) {
            initMetadataConnection();
        }
        if (!config.isNamespaceInitialized()) {
            DescribeMetadataResult result = metadataConnection.describeMetadata(DESCRIBE_METADATA_VERSION);
            String organizationNamespace = result.getOrganizationNamespace();
            config.setNamespace(organizationNamespace != null && organizationNamespace.length() > 0
                    ? organizationNamespace : null);
        }
        return config.getNamespace();
    }

    /**
     * Clears this {@code ForceServiceConnector}'s local state.
     * <p>
     * The connections from a {@code ForceServiceConnector} are lazily
     * constructed and stored in the {@code ForceServiceConnector}'s local state.  Thus
     * multiple calls to get a connection will return the same connection object without
     * re-establishing a connection to the Force.com service.  The {@code close} method
     * forces this {@code ForceServiceConnector} to re-establish a connection with the 
     * Force.com service by clearing its local state.
     * 
     * @see ForceServiceConnector#getConnection()
     * @see ForceServiceConnector#getMetadataConnection()
     * @see ForceServiceConnector#getBulkConnection()
     * @see ForceServiceConnector#renewSession(ConnectorConfig)
     */
    public void close() {
        this.config = null;
        
        this.connectionName = null;
        
        this.externalConfig = null;

        this.clientId = null;
        this.externalClientId = null;

        this.timeout = 0;
        
        this.connection = null;
        this.metadataConnection = null;
        this.bulkConnection = null;
    }
    
    static void clearCache() {
        CACHED_CONFIGS.clear();
        CONN_NAME_TO_CACHED_CONFIGS.clear();
    }

    static Map<String, ForceConnectorConfig> getCachedConfigs() {
        return CACHED_CONFIGS;
    }
    
    static ForceConnectorConfig getCachedConfig(String cacheId) {
        if (cacheId == null) return null;
        return CACHED_CONFIGS.get(cacheId);
    }

    /**
     * Automatically renews Force.com timed out sessions.
     * <p>
     * The connections from a {@code ForceServiceConnector} are lazily
     * constructed and stored in the {@code ForceServiceConnector}'s local state.  Thus
     * multiple calls to get a connection will return the same connection object without
     * re-establishing a connection to the Force.com service.  However, this presents
     * a problem should the Force.com session expire.  In that case, stored connections
     * will have their session automatically renewed by the {@code renewSession}.
     * 
     * @param connectorConfig the {@code ForceConnectorConfig} to be used to re-establish a
     *               Force.com connection
     * @return a session renewal SOAP header
     * @throws ConnectionException if the Force.com connection cannot be re-established
     * @see ForceServiceConnector#getConnection()
     * @see ForceServiceConnector#getMetadataConnection()
     * @see ForceServiceConnector#getBulkConnection()
     * @see ForceServiceConnector#close()
     */
    @Override
    public SessionRenewalHeader renewSession(ConnectorConfig connectorConfig) throws ConnectionException {
        if (connectorConfig.getPassword() != null) {
            connectorConfig.setSessionId(null);
            
            close();
            setConnectorConfig((ForceConnectorConfig) connectorConfig);
            getConnection();

            SessionRenewalHeader ret = new SessionRenewalHeader();
            ret.name = SESSION_HEADER_QNAME;
            SessionHeader_element se = new SessionHeader_element();
            se.setSessionId(connectorConfig.getSessionId());
            ret.headerElement = se;
            return ret;
        }
        return null;
    }


    /**
     * Sets the Force.com connection client id.
     * <p>
     * The client id is a {@code String} identifier that is set on the Force.com
     * connection in this {@code ForceServiceConnector}.  Note that the client
     * id set here will override the client id in this {@code ForceServiceConnector}'s
     * {@code ForceConnectorConfig} state.
     *
     * @param clientId any non {@code null}, non empty {@code String} that is
     *                 to be used as a Force.com connection identifier
     */

    public void setClientId(String clientId) {

        this.clientId = clientId;

    }

    /**
     * Returns the named {@code ForceConnectorConfig} source in
     * this {@code ForceServiceConnector}.
     * <p>
     * A named {@code ForceConnectorConfig} source specifies connection
     * properties in a named location.  These properties can be used to construct
     * a {@code ForceConnectorConfig}.
     * 
     * @return a named {@code ForceConnectorConfig} source
     * @see ForceConnectorUtils#loadConnectorPropsFromName(String)
     */
    public String getConnectionName() {
        return connectionName;
    }
    
    /**
     * Sets the named {@code ForceConnectorConfig} source in
     * this {@code ForceServiceConnector}.
     * <p>
     * A named {@code ForceConnectorConfig} source specifies connection
     * properties in a named location.  These properties can be used to construct
     * a {@code ForceConnectorConfig}.  Note that a named connection {@code ForceConnectorConfig} 
     * source can be overridden by directly injecting the {@code ForceConnectorConfig} state.
     * 
     * @param connectionName the name of a {@code ForceConnectorConfig} source
     * @see ForceConnectorUtils#loadConnectorPropsFromName(String)
     * @see ForceServiceConnector#setConnectorConfig(ForceConnectorConfig)
     */
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    /**
     * Sets the {@code ForceConnectorConfig} state in this {@code ForceServiceConnector}.
     * <p>
     * The {@code ForceConnectorConfig} state is used by a {@code ForceServiceConnector}
     * to get connections to the Force.com service.  Note that setting this state
     * will override any named {@code ForceConnectorConfig} source set
     * in this {@code ForceServiceConnector}.
     * 
     * @param connectorConfig the {@code ForceConnectorConfig} be to used to get Force.com connection
     *               within this {@code ForceServiceConnector}
     * @see ForceConnectorUtils#loadConnectorPropsFromName(String)
     * @see ForceServiceConnector#setConnectionName(String)
     */
    public void setConnectorConfig(ForceConnectorConfig connectorConfig) {
        this.externalConfig = connectorConfig;
    }
    
    /**
     * Indicates whether or not this {@code ForceServiceConnector} should skip
     * in-memory {@code ForceConnectorConfig} cache reads and writes.
     * <p>
     * By default, a {@code ForceServiceConnector} will cache {@code ForceConnectorConfig}
     * objects when getting a connection to the Force.com service.   This state allows
     * the caller to control whether that cache is used or not.
     * 
     * @param skipCache boolean to indicate whether this {@code ForceServiceConnector} should
     *                  ignore the {@code ForceConnectorConfig} in memory cache
     */
    public void setSkipCache(boolean skipCache) {
        this.skipCache = skipCache;
    }
    
    /**
     * Sets the read timeout for all Force.com connections using this {@code ForceServiceConnector}.
     * <p>
     * Note that this timeout value will override any read timeout value set in
     * a {@code ForceConnectorConfig}.
     * 
     * @param timeout the Force.com connection read timeout in milliseconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
