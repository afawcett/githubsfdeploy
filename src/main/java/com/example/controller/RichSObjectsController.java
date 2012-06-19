package com.example.controller;

import com.github.ryanbrainard.richsobjects.RichSObject;
import com.github.ryanbrainard.richsobjects.filters.FullCrudTypesOnlyFilter;
import com.github.ryanbrainard.richsobjects.RichSObjectsService;
import com.github.ryanbrainard.richsobjects.RichSObjectsServiceImpl;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

import static com.github.ryanbrainard.richsobjects.filters.FilterRichSObjectsByFields.*;

@Controller
@RequestMapping("/sobjects")
public class RichSObjectsController {

    private RichSObjectsService service = new RichSObjectsServiceImpl();

    @RequestMapping("")
    public String indexAllSObjects(Map<String, Object> map) {
        map.put("types", new FullCrudTypesOnlyFilter(service.types().iterator()));
        return "listSObjectTypes";
    }

    @RequestMapping("{type}")
    public String indexSObject(@PathVariable("type") String type, Map<String, Object> map) {
        map.put("type", service.describe(type));
        map.put("recentRecords", service.recentItems(type));
        return "listRecentSObjectRecords";
    }

    @RequestMapping("{type}/e")
    public String newSObjectRecord(@PathVariable("type") String type, Map<String, Object> map) {
        map.put("record", StringFieldsOnly(CreateableFieldsOnly(service.of(type))));
        return "editSObjectRecord";
    }

    @RequestMapping(method = RequestMethod.POST, value = "{type}/e")
    public String createSObjectRecord(@PathVariable("type") String type, HttpServletRequest request, Map<String, Object> map) throws IOException {
        final ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
        final Map<String,String> formData = new FormHttpMessageConverter().read(null, inputMessage).toSingleValueMap();
        final RichSObject record = service.of(type, formData);
        
        try {
            final RichSObject saved = service.insert(record);
            return "redirect:" + saved.getField("id").asString();
        } catch (RuntimeException e) {
            map.put("record", StringFieldsOnly(CreateableFieldsOnly(record)));
            map.put("error", e.getMessage()); // TODO: better looking error
            return "editSObjectRecord";
        }
    }

    @RequestMapping("{type}/{id}")
    public String readSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", StringFieldsOnly(PopulatedFieldsOnly(service.fetch(type, id))));
        return "viewSObjectRecord";
    }

    @RequestMapping("{type}/{id}/e")
    public String editSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", StringFieldsOnly(UpdateableFieldsOnly(service.fetch(type, id))));
        return "editSObjectRecord";
    }

    @RequestMapping(method = RequestMethod.POST, value = "{type}/{id}/e")
    public String updateSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, HttpServletRequest request, Map<String, Object> map) throws IOException {
        final ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
        final Map<String,String> formData = new FormHttpMessageConverter().read(null, inputMessage).toSingleValueMap();
        final RichSObject record = service.of(type, formData).getField("id").setValue(id);
        
        try {
            service.update(record);
            return "redirect:../" + id;
        } catch (RuntimeException e) {
            map.put("record", StringFieldsOnly(UpdateableFieldsOnly(record)));
            map.put("error", e.getMessage()); // TODO: better looking error
            return "editSObjectRecord";
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{type}/{id}")
    public String deleteSObjectRecord(@PathVariable("type") String type, @PathVariable("id") String id, Map<String, Object> map) {
        service.delete(service.of(type, id));
        return "OK";
    }
}
