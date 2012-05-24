package com.example.service;


import com.force.api.DescribeSObject;

import java.util.List;
import java.util.Map;

public interface SObjectsService {
    
    List<DescribeSObject> listSObjects();

    List<Map<String,String>> getRecentItems(String sobject);

    Map<DescribeSObject.Field, Object> getSObject(String sobject, String id);

    DescribeSObject describeSObject(String sobject);
}
