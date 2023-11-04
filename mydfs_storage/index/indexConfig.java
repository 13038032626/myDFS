package com.example.mydfs_storage.index;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.util.Map;

@Configuration
public class indexConfig {

    @Bean
    public Map<String, Map<String, String>> normalIndex() throws IOException, ClassNotFoundException {

//        ObjectOutputStream oss = new ObjectOutputStream(new FileOutputStream("/normalIndex.dat"))
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("/normalIndex.dat"));
        Map<String, Map<String, String>> stringMapMap = (Map<String, Map<String, String>>) ois.readObject();
        return stringMapMap;
    }
}
