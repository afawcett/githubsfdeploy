package com.example.controller;

import com.example.service.SObjectsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/sobjects")
public class SObjectsController {

    @Autowired
    private SObjectsService sobjectsService;

    @RequestMapping("")
    public String listSObjects(Map<String, Object> map) {
        map.put("sobjects", sobjectsService.listSObjects());

        return "listSObjects";
    }

}
