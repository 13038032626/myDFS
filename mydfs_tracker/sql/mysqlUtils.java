package com.example.mydfs_tracker.sql;


import com.example.mydfs_tracker.k_vEntitys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class mysqlUtils {

    @Autowired
    filesMapper mapper;

    public List<k_vEntitys> getAllFiles(){
        return mapper.allFiles();
    }
    public boolean isExist(String key){
        List<k_vEntitys> existFiles = mapper.isExist(key);

        if(existFiles.size()>0){
            return true;
        }
        return false;
    }

    public boolean insert(k_vEntitys entity){
        return mapper.insert(entity);
    }
    public k_vEntitys getFile(String hash){
        return mapper.isExist(hash).get(0);
    }
}
