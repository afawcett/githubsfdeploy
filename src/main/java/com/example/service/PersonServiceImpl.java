package com.example.service;

import com.force.api.ApiSession;
import com.force.api.ForceApi;
import com.force.api.QueryResult;
import com.force.sdk.oauth.context.ForceSecurityContextHolder;
import com.force.sdk.oauth.context.SecurityContext;
import org.springframework.stereotype.Service;

import com.example.model.Person;

import java.util.List;

@Service
public class PersonServiceImpl implements PersonService {
    
    private ForceApi getForceApi() {
        SecurityContext sc = ForceSecurityContextHolder.get();

        ApiSession s = new ApiSession();
        s.setAccessToken(sc.getSessionId());
        s.setApiEndpoint(sc.getEndPointHost());

        return new ForceApi(s);
    }
    
    public void addPerson(Person person) {
        getForceApi().createSObject("contact", person);
    }

    public List<Person> listPeople() {
        QueryResult<Person> res = getForceApi().query("SELECT Id, FirstName, LastName FROM contact", Person.class);
        return res.getRecords();
    }

    public void removePerson(String id) {
        getForceApi().deleteSObject("contact", id);
    }
    
}
