package com.example.controller;

import com.example.model.FilterRichSObjectsByFields;
import com.example.model.FullCrudTypesOnlyFilter;
import com.example.service.RichSObjectsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/sobjects")
public class SObjectsController {

    @Autowired
    private RichSObjectsService sobjectsService;

    @RequestMapping("")
    public String indexAllSObjects(Map<String, Object> map) {
        map.put("types", new FullCrudTypesOnlyFilter(sobjectsService.listSObjectTypes().iterator()));
        return "listSObjectTypes";
    }

    @RequestMapping("{type}")
    public String indexSObject(@PathVariable("type") String type, Map<String, Object> map) {
        map.put("type", sobjectsService.describeSObjectType(type));
        map.put("recentRecords", sobjectsService.getRecentItems(type));
        return "listRecentSObjectRecords";
    }

    @RequestMapping("{type}/e")
    public String newSObjectRecord(@PathVariable("type") String type, Map<String, Object> map) {
        map.put("record", FilterRichSObjectsByFields.STRING_FIELDS_ONLY(FilterRichSObjectsByFields.CREATEABLE_FIELDS_ONLY(sobjectsService.newSObject(type))));
        return "editSObjectRecord";
    }

    @RequestMapping(method = RequestMethod.POST, value = "{type}/e")
    public String createSObjectRecord(@PathVariable("type") String type, HttpServletRequest request, Map<String, Object> map) throws IOException {
        final ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
        final Map<String,String> formData = new FormHttpMessageConverter().read(null, inputMessage).toSingleValueMap();

        try {
            final String id = sobjectsService.createSObject(type, formData);
            return "redirect:" + id;
        } catch (RuntimeException e) {
            map.put("record", FilterRichSObjectsByFields.STRING_FIELDS_ONLY(FilterRichSObjectsByFields.CREATEABLE_FIELDS_ONLY(sobjectsService.existingSObject(type, formData))));
            map.put("error", e.getMessage()); // TODO: better looking error
            return "editSObjectRecord";
        }
    }

    @RequestMapping("{type}/{id}")
    public String readSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", FilterRichSObjectsByFields.POPULATED_FIELDS_ONLY(sobjectsService.getSObject(type, id)));
        return "viewSObjectRecord";
    }

    @RequestMapping("{type}/{id}/e")
    public String editSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", FilterRichSObjectsByFields.STRING_FIELDS_ONLY(FilterRichSObjectsByFields.UPDATEABLE_FIELDS_ONLY(sobjectsService.getSObject(type, id))));
        return "editSObjectRecord";
    }

    @RequestMapping(method = RequestMethod.POST, value = "{type}/{id}/e")
    public String updateSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, HttpServletRequest request, Map<String, Object> map) throws IOException {
        final ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
        final Map<String,String> formData = new FormHttpMessageConverter().read(null, inputMessage).toSingleValueMap();

        try {
            sobjectsService.updateSObject(type, id, formData);
            return "redirect:../" + id;
        } catch (RuntimeException e) {
            map.put("record", FilterRichSObjectsByFields.STRING_FIELDS_ONLY(FilterRichSObjectsByFields.UPDATEABLE_FIELDS_ONLY(sobjectsService.existingSObject(type, formData))));
            map.put("error", e.getMessage()); // TODO: better looking error
            return "editSObjectRecord";
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{type}/{id}")
    public String deleteSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        sobjectsService.deleteSObject(type, id);
        return "OK";
    }
}
