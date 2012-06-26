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
@RequestMapping("/persons")
public class PersonController {

    private RichSObjectsService service = new RichSObjectsServiceImpl();

    @RequestMapping("")
    public String listPersons(Map<String, Object> map) {
     	map.put("personList", service.query("select Id,FirstName,LastName,Email FROM Contact"));
    	return "persons";
    }

    @RequestMapping("/{id}")
    public String getPersonDetail(@PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", StringFieldsOnly(PopulatedFieldsOnly(service.fetch("Contact", id))));
        return "personDetail";
    }
    
    @RequestMapping("/{id}/e")
    public String editPerson(@PathVariable("id") String id, Map<String, Object> map) {
        map.put("record", StringFieldsOnly(PopulatedFieldsOnly(service.fetch("Contact", id))));
        return "editPerson";
    }
    
    @RequestMapping(method = RequestMethod.POST, value = "/{id}/e")
    public String updatePerson(@PathVariable("id") String id, HttpServletRequest request, Map<String, Object> map) throws IOException {
        final ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
        final Map<String,String> formData = new FormHttpMessageConverter().read(null, inputMessage).toSingleValueMap();
        final RichSObject record = service.of("Contact", formData).getField("id").setValue(id);
        
        try {
            service.update(record);
            return "redirect:../" + id;
        } catch (RuntimeException e) {
            map.put("record", StringFieldsOnly(UpdateableFieldsOnly(record)));
            map.put("error", e.getMessage()); // TODO: better looking error
            return "editSObjectRecord";
        }
    }


    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
    public String deleteSObjectRecord(@PathVariable("id") String id, Map<String, Object> map) {
        service.delete(service.of("Contact", id));
        return "OK";
    }


  
}
