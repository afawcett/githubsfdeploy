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
}
