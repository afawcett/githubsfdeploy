package com.example.controller;

import com.example.model.PopulatedFieldsOnlyFilter;
import com.example.model.UpdateableFieldsOnlyFilter;
import com.example.service.RichSObjectsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Controller
@RequestMapping("/sobjects")
public class SObjectsController {

    @Autowired
    private RichSObjectsService sobjectsService;

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
        map.put("record", new PopulatedFieldsOnlyFilter(sobjectsService.getSObject(type, id)));
        return "viewSObjectRecord";
    }

    @RequestMapping("{type}/{id}/e")
    public String editSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", new UpdateableFieldsOnlyFilter(sobjectsService.getSObject(type, id)));
        return "editSObjectRecord";
    }

    @RequestMapping(method = RequestMethod.POST, value = "{type}/{id}/e")
    public String updateSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        return "redirect:../" + id;
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{type}/{id}")
    public String deleteSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        sobjectsService.deleteSObject(type, id);
        return "OK";
    }
}
