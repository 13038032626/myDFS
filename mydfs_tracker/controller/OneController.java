package com.example.mydfs_tracker.controller;

import com.example.mydfs_tracker.MQ.mqUtils;
import com.example.mydfs_tracker.entity.MainMap;
import com.example.mydfs_tracker.entity.MetaData;
import com.example.mydfs_tracker.storage;
import com.google.gson.Gson;
import org.apache.logging.slf4j.SLF4JLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
public class OneController {

    @Autowired
    mqUtils mqUtils;
    @Autowired
    MainMap mainMap;

    private static final Logger logger = LoggerFactory.getLogger(OneController.class);

    Gson gson = new Gson();

    public List<Integer[]> getAlternativeStoragesAndIndex(long size) {
        int num = (int) size / (4 * 1024 * 1024) + 1;
        storage[] availableStorages = mqUtils.getAvailableStorages();
        List<Integer[]> ans = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            Integer[] integers = new Integer[2];
            integers[0] = i;
            Integer startIndex = availableStorages[i % num].getStartIndex();
            integers[1] = startIndex;
            ans.add(integers);
        }
        return ans;
    }
    public boolean responseCommit(MetaData metaData) {

        try {
            String jsonObj = gson.toJson(metaData);
            logger.info(jsonObj);
            mainMap.map.put(metaData.getFileName(), metaData.getLocation());
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
