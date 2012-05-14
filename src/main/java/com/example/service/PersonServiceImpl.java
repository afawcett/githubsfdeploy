package com.example.service;

import com.force.api.ApiConfig;
import com.force.api.ForceApi;
import com.force.api.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.model.Person;

import java.util.List;

@Service
public class PersonServiceImpl implements PersonService {

    @Autowired
    private ApiConfig apiConfig;
    
    public void addPerson(Person person) {
        new ForceApi(apiConfig).createSObject("contact", person);
    }

    public List<Person> listPeople() {
        QueryResult<Person> res = new ForceApi(apiConfig).query("SELECT Id, FirstName, LastName FROM contact", Person.class);
        return res.getRecords();
    }

    public void removePerson(String id) {
        new ForceApi(apiConfig).deleteSObject("contact", id);
    }
    
}
