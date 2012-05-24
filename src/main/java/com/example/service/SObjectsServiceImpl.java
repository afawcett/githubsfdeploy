package com.example.service;

import com.force.api.ApiSession;
import com.force.api.DescribeSObject;
import com.force.api.ForceApi;
import com.force.sdk.oauth.context.ForceSecurityContextHolder;
import com.force.sdk.oauth.context.SecurityContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class SObjectsServiceImpl implements SObjectsService {
    
    private ForceApi getForceApi() {
        SecurityContext sc = ForceSecurityContextHolder.get();

        ApiSession s = new ApiSession();
        s.setAccessToken(sc.getSessionId());
        s.setApiEndpoint(sc.getEndPointHost());

        return new ForceApi(s);
    }

    @Override
    public List<String> listSObjects() {
        final List<DescribeSObject> sObjectDescs = getForceApi().describeGlobal().getSObjects();
        final List<String> sObjectDescsAsString = new ArrayList<String>(sObjectDescs.size());
        for (DescribeSObject sObject : sObjectDescs) {
            sObjectDescsAsString.add(sObject.getName());
        }
        return Collections.unmodifiableList(sObjectDescsAsString);
    }

    @Override
    public List<Map<String, String>> getRecentItems(String sobject) {
        final List<Map> rawRecentItems = getForceApi().getRecentItems(sobject, Map.class);
        final List<Map<String, String>> recentItems = new ArrayList<Map<String, String>>(rawRecentItems.size());
        for (Map item : rawRecentItems) {
            //noinspection unchecked
            recentItems.add((Map<String, String>) item);
        }
        return Collections.unmodifiableList(recentItems);
    }
}
