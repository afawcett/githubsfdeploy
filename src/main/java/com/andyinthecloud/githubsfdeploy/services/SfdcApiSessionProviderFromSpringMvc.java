package com.andyinthecloud.githubsfdeploy.services;

import com.force.sdk.oauth.context.ForceSecurityContextHolder;
import com.github.ryanbrainard.richsobjects.api.client.SfdcApiSessionProvider;

/**
 * @author Ryan Brainard
 */
@SuppressWarnings("UnusedDeclaration")
public class SfdcApiSessionProviderFromSpringMvc implements SfdcApiSessionProvider {

    @Override
    public String getAccessToken() {
        return ForceSecurityContextHolder.get(true).getSessionId();
    }

    @Override
    public String getApiEndpoint() {
        return ForceSecurityContextHolder.get(true).getEndPointHost();
    }
}
