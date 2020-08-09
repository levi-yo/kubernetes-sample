package com.kebe.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class SampleApplication {

    private int random = (int)(Math.random()*10)+1;

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    @RequestMapping("/api")
    public String api(){
        return "new api ! - "+random;
    }
}
