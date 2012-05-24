package com.example.service;

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
    public List<DescribeSObject> listSObjects() {
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
    public List<Map<String, String>> getRecentItems(String sobject) {
        final List<Map> rawRecentItems = getForceApi().getRecentItems(sobject, Map.class);
        final List<Map<String, String>> recentItems = new ArrayList<Map<String, String>>(rawRecentItems.size());
        for (Map item : rawRecentItems) {
            //noinspection unchecked
            recentItems.add((Map<String, String>) item);
        }
        return Collections.unmodifiableList(recentItems);
    }

    @Override
    public Map<DescribeSObject.Field, Object> getSObject(String sobject, String id) {
        @SuppressWarnings("unchecked")
        final Map<String,Object> rawRecord = (Map<String, Object>) getForceApi().getSObject(sobject, id).asMap();
        final DescribeSObject describeSObject = describeSObject(sobject);
        final List<DescribeSObject.Field> labelSortedFields = sortByLabel(describeSObject.getFields());
        return makeRichRecord(stripToPopulatedFields(labelSortedFields, rawRecord), rawRecord);
    }

    @Override
    public DescribeSObject describeSObject(String sobject) {
        return getForceApi().describeSObject(sobject);
    }

    private List<DescribeSObject.Field> sortByLabel(List<DescribeSObject.Field> fields) {
        List<DescribeSObject.Field> sortedFields = new ArrayList<DescribeSObject.Field>(fields);
        Collections.sort(sortedFields, new Comparator<DescribeSObject.Field>() {
            @Override
            public int compare(DescribeSObject.Field o1, DescribeSObject.Field o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        return Collections.unmodifiableList(sortedFields);
    }

    private List<DescribeSObject.Field> stripToPopulatedFields(List<DescribeSObject.Field> fields, Map<String, Object> record) {
        List<DescribeSObject.Field> strippedFields = new ArrayList<DescribeSObject.Field>();
        for (DescribeSObject.Field field : fields) {
            if (record.containsKey(field.getName()) && record.get(field.getName()) != null) {
                strippedFields.add(field);
            }
        }
        return Collections.unmodifiableList(strippedFields);
    }
    
    private Map<DescribeSObject.Field, Object> makeRichRecord(List<DescribeSObject.Field> fields, Map<String, Object> record) {
        Map<DescribeSObject.Field, Object> richRecord = new LinkedHashMap<DescribeSObject.Field, Object>(record.size());
        for (DescribeSObject.Field field : fields) {
            richRecord.put(field, record.get(field.getName()));
        }
        return Collections.unmodifiableMap(richRecord);
    }
    
}
