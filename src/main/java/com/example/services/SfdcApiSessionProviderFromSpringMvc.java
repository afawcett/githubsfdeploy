package com.example.services;

import com.force.sdk.oauth.context.ForceSecurityContextHolder;
import com.github.ryanbrainard.richsobjects.api.client.SfdcApiSessionProvider;

/**
 * @author Ryan Brainard
 */
@SuppressWarnings("UnusedDeclaration")
public class SfdcApiSessionProviderFromSpringMvc implements SfdcApiSessionProvider {

    private final com.force.sdk.oauth.context.SecurityContext sc;

    public SfdcApiSessionProviderFromSpringMvc() {
        sc = ForceSecurityContextHolder.get(true);
    }

    @Override
    public String getAccessToken() {
        return sc.getSessionId();
    }

    @Override
    public String getApiEndpoint() {
        return sc.getEndPointHost();
    }
}
