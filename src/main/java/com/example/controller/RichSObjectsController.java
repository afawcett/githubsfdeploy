package com.example.controller;

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

import static com.example.model.FilterRichSObjectsByFields.*;

@Controller
@RequestMapping("/sobjects")
public class RichSObjectsController {

    @Autowired
    private RichSObjectsService service;

    @RequestMapping("")
    public String indexAllSObjects(Map<String, Object> map) {
        map.put("types", new FullCrudTypesOnlyFilter(service.listSObjectTypes().iterator()));
        return "listSObjectTypes";
    }

    @RequestMapping("{type}")
    public String indexSObject(@PathVariable("type") String type, Map<String, Object> map) {
        map.put("type", service.describeSObjectType(type));
        map.put("recentRecords", service.getRecentItems(type));
        return "listRecentSObjectRecords";
    }

    @RequestMapping("{type}/e")
    public String newSObjectRecord(@PathVariable("type") String type, Map<String, Object> map) {
        map.put("record", StringFieldsOnly(CreateableFieldsOnly(service.newSObject(type))));
        return "editSObjectRecord";
    }

    @RequestMapping(method = RequestMethod.POST, value = "{type}/e")
    public String createSObjectRecord(@PathVariable("type") String type, HttpServletRequest request, Map<String, Object> map) throws IOException {
        final ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
        final Map<String,String> formData = new FormHttpMessageConverter().read(null, inputMessage).toSingleValueMap();

        try {
            final String id = service.createSObject(type, formData);
            return "redirect:" + id;
        } catch (RuntimeException e) {
            map.put("record", StringFieldsOnly(CreateableFieldsOnly(service.existingSObject(type, formData))));
            map.put("error", e.getMessage()); // TODO: better looking error
            return "editSObjectRecord";
        }
    }

    @RequestMapping("{type}/{id}")
    public String readSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", PopulatedFieldsOnly(service.getSObject(type, id)));
        return "viewSObjectRecord";
    }

    @RequestMapping("{type}/{id}/e")
    public String editSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", StringFieldsOnly(UpdateableFieldsOnly(service.getSObject(type, id))));
        return "editSObjectRecord";
    }

    @RequestMapping(method = RequestMethod.POST, value = "{type}/{id}/e")
    public String updateSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, HttpServletRequest request, Map<String, Object> map) throws IOException {
        final ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
        final Map<String,String> formData = new FormHttpMessageConverter().read(null, inputMessage).toSingleValueMap();

        try {
            service.updateSObject(type, id, formData);
            return "redirect:../" + id;
        } catch (RuntimeException e) {
            map.put("record", StringFieldsOnly(UpdateableFieldsOnly(service.existingSObject(type, formData))));
            map.put("error", e.getMessage()); // TODO: better looking error
            return "editSObjectRecord";
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{type}/{id}")
    public String deleteSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        service.deleteSObject(type, id);
        return "OK";
    }
}
