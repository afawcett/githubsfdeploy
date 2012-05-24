package com.example.controller;

import com.example.service.SObjectsService;
import com.force.api.DescribeSObject;
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
        map.put("sobjects", sobjectsService.listSObjects());
        return "indexAllSObjects";
    }

    @RequestMapping("{sobject}")
    public String indexSObject(@PathVariable("sobject") String sobject, Map<String, Object> map) {
        map.put("sobject", sobjectsService.describeSObject(sobject));
        map.put("recentItems", sobjectsService.getRecentItems(sobject));
        return "indexSObject";
    }

    @RequestMapping("{sobject}/{id}")
    public String readSObjectRecord(@PathVariable("sobject") String sobject, @PathVariable("id") String id, Map<String, Object> map) {
        final DescribeSObject describeSObject = sobjectsService.describeSObject(sobject);
        map.put("sobject", describeSObject);
        map.put("record", sobjectsService.getSObject(sobject, id));
        return "readSObjectRecord";
    }
}
