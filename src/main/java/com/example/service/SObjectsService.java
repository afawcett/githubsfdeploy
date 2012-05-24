package com.example.service;


import java.util.List;
import java.util.Map;

public interface SObjectsService {
    
    public List<String> listSObjects();

    public List<Map<String,String>> getRecentItems(String sobject);
}
