package com.kebe.sample;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@RestController
@SpringBootApplication
public class SampleApplication {

    private int random = (int)(Math.random()*10)+1;

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    @RequestMapping("/api")
    public String api(){
        log.info("SampleApplication#api :::: current time = {}", LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
        return "new api ! - "+random;
    }
}
