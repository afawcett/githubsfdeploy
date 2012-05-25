package com.example.service;


import com.example.model.RichSObject;
import com.force.api.DescribeSObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface RichSObjectsService {

    List<DescribeSObject> listSObjectTypes();

    Iterator<RichSObject> getRecentItems(String type);

    RichSObject getSObject(String type, String id);

    DescribeSObject describeSObjectType(String type);

    RichSObject newSObject(String type);

    RichSObject existingSObject(String type, Map<String, ?> record);

    void updateSObject(String type, String id, Map<String, ?> record);

    String createSObject(String type, Map<String, String> record);

    void deleteSObject(String type, String id);

}
