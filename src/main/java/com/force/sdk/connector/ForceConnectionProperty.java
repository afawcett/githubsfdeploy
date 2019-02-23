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

import java.util.regex.Pattern;

/**
 * Property used for a Force.com connection URL.
 * <p>
 * These enum values represent available Force.com connection URL
 * properties.  Each value contains two parts: 
 * <p>
 * <ol>
 *   <li>The property key name</li>
 *   <li>A validation regular expression</li>
 * </ol>
 * <p>
 * A Force.com connection URL is written using the property key name
 * in key=value pairs which are separated by a semi-colon (';') delimiter.
 * The exception is the Force.com endpoint property which is interpreted
 * as the string which immediately follows the force:// protocol.
 * For example, a Force.com connection URL with a username and password
 * would look like this:
 * <pre>
 * {@code force://<endpoint value>;user=<username value>;password=<password value>}
 * </pre>  
 * The validation regular expression allows for the validation of
 * property values within a Force.com connection URL.
 * 
 * @author Tim Kral
 */
public enum ForceConnectionProperty {
    /**
     * The Force.com connection endpoint.
     */
    // The endpoint can begin with nothing, http://, or https://
    // It can end with nothing, / or /services/Soap/u/<API version number>
    ENDPOINT("endpoint", Pattern.compile("^(http://|https://)?([A-Za-z0-9_.\\-:]+)(/|/services/Soap/u/(\\d+|\\d+.\\d+))?$")),
    /**
     * The username to be used in a Force.com connection.
     */
    USER("user", null), 
    /**
     * The password to be used in a Force.com connection.
     */
    PASSWORD("password", null),
    /**
     * The OAuth key to be used in a Force.com OAuth handshake.
     */
    OAUTH_KEY("oauth_key", null),
    /**
     * The OAuth secret to be used in a Force.com OAuth handshake.
     */
    OAUTH_SECRET("oauth_secret", null),
    /**
     * The API client id to be used in a Force.com connection.
     */
    CLIENTID("clientid", null),
    /**
     * The timeout to be used in a Force.com connection.
     */
    TIMEOUT("timeout", Pattern.compile("(\\d+)")),
    ;
    
    private String propertyName;
    private Pattern validationRegex;
    
    private ForceConnectionProperty(String propertyName, Pattern validationRegex) {
        this.propertyName = propertyName;
        this.validationRegex = validationRegex;
    }
    
    /**
     * Returns the connection property name.
     * <p>
     * The property name (with the exception of the endpoint
     * property) is used as the key in a key=value pair within
     * a Force.com connection URL.
     * 
     * @return the connection property name
     */
    public String getPropertyName() { return this.propertyName; }
    
    /**
     * Returns the connection property validation regular expression
     * <p>
     * The validation regular expression is used to validate
     * a property value that is set in a key=value pair within
     * a Force.com connection URL.
     * 
     * @return the connection property validation regular expression
     */
    public Pattern getValidationRegex() { return this.validationRegex; }
    
    /**
     * Validates a connection property value.
     * <p>
     * Ensures that the connection property value is not {@code null}, has a length
     * greater than zero and matches the validation regular expression.
     * 
     * @param propertyValue the connection property value
     * @throws IllegalArgumentException if the given {@code propertyValue} is {@code null} or zero length or
     *                                  does not match the validation regular expression 
     */
    public void validateValue(String propertyValue) {
        validateValue(propertyValue, "");
    }
    
    /**
     * Validates a connection property value.
     * <p>
     * Ensures that the connection property value is not {@code null}, has a length
     * greater than zero and matches the validation regular expression.
     * 
     * @param propertyValue the connection property value
     * @param customErrorMessage an error message to be included in the thrown exception message
     *                           should validation fail
     * @throws IllegalArgumentException if the given {@code propertyValue} is {@code null} or zero length or
     *                                  does not match the validation regular expression 
     */
    public void validateValue(String propertyValue, String customErrorMessage) {
        if ("endpoint".equals(this.propertyName) && propertyValue != null && propertyValue.contains("localhost")) {
            return;
        }

        if (propertyValue == null || propertyValue.length() == 0) {
            StringBuffer errorMessage = new StringBuffer(customErrorMessage);
            errorMessage.append(" The ForceConnectionProperty (" + this.propertyName + ") must have a value");
            
            throw new IllegalArgumentException(errorMessage.toString());
        }
        
        if (this.validationRegex == null) return;
        
        if (!validationRegex.matcher(propertyValue).matches()) {
            StringBuffer errorMessage = new StringBuffer(customErrorMessage);
            errorMessage.append(" Illegal value (" + propertyValue + ") for ForceConnectionProperty (" + this.propertyName + ")");
            
            throw new IllegalArgumentException(errorMessage.toString());
        }
    }
    
    /**
     * Loads the {@code ForceConnectionProperty} enum value from a connection 
     * property name.
     * 
     * @param propertyName A connection property name
     * @return The {@code ForceConnectionProperty} enum value matching the given
     *         propertyName or {@code null} if no such value exists
     */
    public static ForceConnectionProperty fromPropertyName(String propertyName) {
        if (propertyName == null) return null;
        
        for (ForceConnectionProperty connProp : values()) {
            if (connProp.getPropertyName().equals(propertyName.toLowerCase()))
                return connProp;
        }
        
        return null;
    }
}