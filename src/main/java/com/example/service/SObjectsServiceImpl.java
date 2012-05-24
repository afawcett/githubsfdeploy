package com.example.service;

import com.example.model.ImmutableRichSObject;
import com.example.model.RichSObject;
import com.force.api.ApiSession;
import com.force.api.DescribeSObject;
import com.force.api.ForceApi;
import com.force.sdk.oauth.context.ForceSecurityContextHolder;
import com.force.sdk.oauth.context.SecurityContext;
import org.springframework.stereotype.Service;

import java.util.*;

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
    public List<DescribeSObject> listSObjectTypes() {
        final List<DescribeSObject> describeSObjects = getForceApi().describeGlobal().getSObjects();
        Collections.sort(describeSObjects, new Comparator<DescribeSObject>() {
            @Override
            public int compare(DescribeSObject o1, DescribeSObject o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        return Collections.unmodifiableList(describeSObjects);
    }


    @Override
    public DescribeSObject describeSObjectType(String type) {
        return getForceApi().describeSObject(type);
    }

    @Override
    public RichSObject getSObject(String type, String id) {
        return new ImmutableRichSObject(describeSObjectType(type), getRawSObject(type, id));
    }

    private Map<String, Object> getRawSObject(String sobject, String id) {
        //noinspection unchecked
        return (Map<String, Object>) getForceApi().getSObject(sobject, id).asMap();
    }

    @Override
    public Iterator<RichSObject> getRecentItems(String type) {
        final DescribeSObject describeSObject = describeSObjectType(type);
        final Iterator<Map> rawRecentItems = getForceApi().getRecentItems(type, Map.class).iterator();

        return new Iterator<RichSObject>() {
            @Override
            public boolean hasNext() {
                return rawRecentItems.hasNext();
            }

            @Override
            public RichSObject next() {
                //noinspection unchecked
                return new ImmutableRichSObject(describeSObject, rawRecentItems.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Recent items cannot be removed");
            }
        };
    }
}
