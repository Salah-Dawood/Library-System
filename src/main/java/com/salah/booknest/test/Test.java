package com.salah.booknest.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path ="/api")
public class Test {
    @GetMapping("/hello")
    public String hello(){
        System.out.println("Test triggered");
        return "Hello, all good here";
    }
}
