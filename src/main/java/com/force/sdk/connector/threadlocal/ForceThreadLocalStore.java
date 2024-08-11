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

package com.force.sdk.connector.threadlocal;

import java.util.HashMap;

import com.force.sdk.connector.ForceConnectorConfig;

/**
 * Stores connector configs in a {@code ThreadLocal} cache.
 *
 * @author John Simone
 */
public final class ForceThreadLocalStore {

    // Thread Local for saved configs
    private static final ThreadLocal<ForceConnectorConfig> TL_CONNECTOR_CONFIG = new ThreadLocal<ForceConnectorConfig>();
    // Thread Local map for other classes
    private static final HashMap<Class<?>, ThreadLocal<?>> TL_MAP = new HashMap<Class<?>, ThreadLocal<?>>();
    
    private ForceThreadLocalStore() {  }
    
    /**
     * Retrieves the {@code ForceConnectorConfig} from the
     * {@code ForceThreadLocalStore} {@code ThreadLocal} cache.
     * 
     * @return the {@code ForceConnectorConfig} found in the {@code ThreadLocal}
     *         cache; {@code null} if the cache is empty
     */
    public static ForceConnectorConfig getConnectorConfig() {
        return TL_CONNECTOR_CONFIG.get();
    }

    /**
     * Sets the given {@code ForceConnectorConfig} in the
     * {@code ForceThreadLocalStore} {@code ThreadLocal} cache.
     * 
     * @param config the {@code ForceConnectorConfig} to be added
     *               to the {@code ThreadLocal} cache
     */
    public static void setConnectorConfig(ForceConnectorConfig config) {
        TL_CONNECTOR_CONFIG.set(config);
    }
    
    /**
     * Retrieves the object of type {@code clazz} from the
     * {@code ForceThreadLocalStore} {@code ThreadLocal} cache.
     * 
     * @param <T> describes the object type to be searched for in the {@code ThreadLocal} cache
     * @param clazz the object type to be searched for in the {@code ThreadLocal} cache
     * @return the object of type {@code clazz} found in the {@code ThreadLocal}
     *         cache; {@code null} if no such object can be found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getThreadLocal(Class<T> clazz) {
        ThreadLocal<T> tl = null;
        if (TL_MAP.containsKey(clazz)) {
            tl = (ThreadLocal<T>) TL_MAP.get(clazz);
        } else {
            tl = new ThreadLocal<T>();
            TL_MAP.put(clazz, tl);
        }
        
        return tl.get();
    }
    
    /**
     * Sets an object of type {@code clazz} in the
     * {@code ForceThreadLocalStore} {@code ThreadLocal} cache.
     * 
     * @param <T> describes the object type to be added to the {@code ThreadLocal} cache
     * @param clazz the object type to be added to the {@code ThreadLocal} cache
     * @param variable the object to be added to the {@code ThreadLocal} cache
     */
    @SuppressWarnings("unchecked")
    public static <T> void setThreadLocal(Class<T> clazz, T variable) {
        ThreadLocal<T> tl = null;
        if (TL_MAP.containsKey(clazz)) {
            tl = (ThreadLocal<T>) TL_MAP.get(clazz);
        } else {
            tl = new ThreadLocal<T>();
            TL_MAP.put(clazz, tl);
        }
        tl.set(variable);
    }
}
