package com.example.service;


import com.example.model.RichSObject;
import com.force.api.DescribeSObject;

import java.util.Iterator;
import java.util.List;

public interface RichSObjectsService {
    
    List<DescribeSObject> listSObjectTypes();

    Iterator<RichSObject> getRecentItems(String type);

    RichSObject getSObject(String type, String id);

    DescribeSObject describeSObjectType(String type);

    void deleteSObject(String type, String id);
}
