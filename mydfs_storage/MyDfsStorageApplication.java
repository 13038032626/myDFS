package com.example.mydfs_storage;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableRabbit
@SpringBootApplication
public class MyDfsStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyDfsStorageApplication.class, args);
    }

}
