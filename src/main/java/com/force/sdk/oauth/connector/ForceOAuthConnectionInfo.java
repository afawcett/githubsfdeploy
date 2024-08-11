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
import java.util.Map;

import com.force.sdk.connector.ForceConnectionProperty;
import com.force.sdk.connector.ForceConnectorUtils;

/**
 * Bean that stores Force.com oauth connection information.
 *
 * @author Tim Kral
 */
public class ForceOAuthConnectionInfo {

    // Represents the minimum number of properties that must appear
    // in a Force.com connection URL (i.e. endpoint, oauth_key, oauth_secret)
    private static final int MIN_CONN_URL_PROPS = 3;
    
    private String endpoint;
    private String oauthKey;
    private String oauthSecret;
    
    static ForceOAuthConnectionInfo loadFromName(String connectionName) throws IOException {
        
        Map<ForceConnectionProperty, String> propMap = ForceConnectorUtils.loadConnectorPropsFromName(connectionName);
        if (propMap == null) return null;
        
        ForceOAuthConnectionInfo connInfo = new ForceOAuthConnectionInfo();
        connInfo.setPropsFromMap(propMap, connectionName);
        return connInfo;
    }
    
    /**
     * Appends the oauth key parameter to the url.
     * 
     * @param url 
     * @return url
     */
    StringBuffer appendOauthKeyParam(StringBuffer url) {
        return url.append("&client_id=").append(getOauthKey());
    }
    
    /**
     * Appends the oauth secret parameter to the url.
     * 
     * @param url
     * @return url
     */
    StringBuffer appendOauthSecretParam(StringBuffer url) {
        return url.append("&client_secret=").append(getOauthSecret());
    }
    
    /**
     * Parses the connection url and sets the values pulled from it.
     * 
     * @param connectionUrl String
     */
    public void setConnectionUrl(String connectionUrl) {
        Map<ForceConnectionProperty, String> propMap = ForceConnectorUtils.loadConnectorPropsFromUrl(connectionUrl);
        if (propMap == null || propMap.size() < MIN_CONN_URL_PROPS) {
            throw new IllegalArgumentException("The connection url (" + connectionUrl + ") must contain at least three parts. "
                                               + "It should be in the form "
                                               + "force://<endPoint>?oauth_key=<oauthKey>&oauth_secret=<oauthSecret>");
        }
        
        setPropsFromMap(propMap, connectionUrl);
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getOauthKey() {
        return oauthKey;
    }
    
    public void setOauthKey(String oauthKey) {
        this.oauthKey = oauthKey;
    }
    
    public String getOauthSecret() {
        return oauthSecret;
    }
    
    public void setOauthSecret(String oauthSecret) {
        this.oauthSecret = oauthSecret;
    }
    
    private void setPropsFromMap(Map<ForceConnectionProperty, String> propMap, String propSource) {
        String errorMessage = "Could not load valid ForceOAuthConnectionInfo properties from " + propSource + ".";
        
        String endpointValue = propMap.get(ForceConnectionProperty.ENDPOINT);
        ForceConnectionProperty.ENDPOINT.validateValue(endpointValue, errorMessage);
        setEndpoint(endpointValue);
        
        String oauthKeyValue = propMap.get(ForceConnectionProperty.OAUTH_KEY);
        ForceConnectionProperty.OAUTH_KEY.validateValue(oauthKeyValue, errorMessage);
        setOauthKey(oauthKeyValue);
        
        String oauthSecretValue = propMap.get(ForceConnectionProperty.OAUTH_SECRET);
        ForceConnectionProperty.OAUTH_SECRET.validateValue(oauthSecretValue, errorMessage);
        setOauthSecret(oauthSecretValue);
    }
    
    /**
     * Ensures that the data represents a valid OAuth connection.
     */
    public void validate() {
        ForceConnectionProperty.ENDPOINT.validateValue(getEndpoint());
        ForceConnectionProperty.OAUTH_KEY.validateValue(getOauthKey());
        ForceConnectionProperty.OAUTH_SECRET.validateValue(getOauthSecret());
    }
}
