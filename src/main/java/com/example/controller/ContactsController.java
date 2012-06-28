package com.example.controller;

import static com.github.ryanbrainard.richsobjects.filters.FilterRichSObjectsByFields.PopulatedFieldsOnly;
import static com.github.ryanbrainard.richsobjects.filters.FilterRichSObjectsByFields.StringFieldsOnly;
import static com.github.ryanbrainard.richsobjects.filters.FilterRichSObjectsByFields.UpdateableFieldsOnly;

import com.github.ryanbrainard.richsobjects.RichSObject;
import com.github.ryanbrainard.richsobjects.RichSObjectsService;
import com.github.ryanbrainard.richsobjects.RichSObjectsServiceImpl;

import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;


@Controller
@RequestMapping("/contacts")
public class ContactsController {

    private RichSObjectsService salesforceService = new RichSObjectsServiceImpl();

    @RequestMapping("")
    public String listContacts(Map<String, Object> map) {
     	map.put("contactList", salesforceService.query("select Id,FirstName,LastName,Email FROM Contact"));
    	return "contacts";
    }

    @RequestMapping("/{id}")
    public String getContactDetail(@PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", StringFieldsOnly(PopulatedFieldsOnly(salesforceService.fetch("Contact", id))));
        return "contactDetail";
    }
    
    @RequestMapping("/{id}/e")
    public String editContact(@PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", StringFieldsOnly(PopulatedFieldsOnly(salesforceService.fetch("Contact", id))));
        return "editContact";
    }
    
    @RequestMapping(method = RequestMethod.POST, value = "/{id}/e")
    public String updateContact(@PathVariable("id") String id, HttpServletRequest request, Map<String, Object> map) throws IOException {
        final ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
        final Map<String,String> formData = new FormHttpMessageConverter().read(null, inputMessage).toSingleValueMap();
        final RichSObject record = salesforceService.of("Contact", formData).getField("id").setValue(id);
        
        try {
        	salesforceService.update(record);
            return "redirect:../" + id;
        } catch (RuntimeException e) {
            map.put("record", StringFieldsOnly(UpdateableFieldsOnly(record)));
            map.put("error", e.getMessage()); // TODO: better looking error
            return "editContact";
        }
    }


    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
    public String deleteSObjectRecord(@PathVariable("id") String id, Map<String, Object> map) {
    	salesforceService.delete(salesforceService.of("Contact", id));
        return "OK";
    }


  
}
