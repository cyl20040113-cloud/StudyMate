package com.cyl.studymate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StudyMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyMateApplication.class, args);
    }

}
