package com.example.mydfs_storage.utils;

import com.example.mydfs_storage.spaceController.FileSelf;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LockUtils {


    @Autowired
    FileSelf fileSelf;

    Integer currentSlicesNum = fileSelf.getStartIndex() / (6 * 1024 * 1024);

    List<Object> locks = new ArrayList<>();

    @PostConstruct
    public void init(){
        for (int i = 0; i < currentSlicesNum; i++) {
            locks.add(new Object());
        }
    }

    public List<Object> getLocks(){
        return locks;
    };
}
