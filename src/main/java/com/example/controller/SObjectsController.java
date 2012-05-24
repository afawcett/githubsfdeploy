package com.example.controller;

import com.example.service.SObjectsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/sobjects")
public class SObjectsController {

    @Autowired
    private SObjectsService sobjectsService;

    @RequestMapping("")
    public String indexAllSObjects(Map<String, Object> map) {
        map.put("types", sobjectsService.listSObjectTypes());
        return "listSObjectTypes";
    }

    @RequestMapping("{type}")
    public String indexSObject(@PathVariable("type") String type, Map<String, Object> map) {
        map.put("type", sobjectsService.describeSObjectType(type));
        map.put("recentRecords", sobjectsService.getRecentItems(type));
        return "listRecentSObjectRecords";
    }

    @RequestMapping("{type}/{id}")
    public String readSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", sobjectsService.getSObject(type, id));
        return "readSObjectRecord";
    }
}
