package com.example.mydfs_tracker;

import com.example.mydfs_tracker.MQ.mqUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
public class mainController {

    @Autowired
    com.example.mydfs_tracker.sql.mysqlUtils mysqlUtils;
    @Autowired
    mqUtils mqUtils;

    @GetMapping("fileAttempt")
    //上传小文件时访问此方法，记录到数据库，返回可用的节点  小文件——> 小于50MB
    public storage fileAttempt(String hash, Integer size) {
        boolean exist = mysqlUtils.isExist(hash);
        if (!exist) {
            k_vEntitys kVEntitys = new k_vEntitys(hash, false, size);

            storage[] availableStorages = mqUtils.getAvailableStorages();
            storage targetStorage = availableStorages[0];
            //TODO 选择一个storage进行存储
            kVEntitys.setValue(targetStorage.toString());
            mysqlUtils.insert(kVEntitys);
            return targetStorage;
        } else {
            return null;
        }
    }

    @GetMapping("largeFileAttempt")
    public storage[] largeFileAttempt(String hash, Integer totalSize, Integer sliceSize) {
        //上传大文件时访问此方法，记录到数据库，返回多个可用节点，由client将每片依次存储
        final Integer THRESHOLD = 50 * 1024 * 1024;  //单位是byte,这里限制每个分片最大50MB
        int sliceNum = totalSize / sliceSize + 1;   //有几片
        boolean exist = mysqlUtils.isExist(hash);
        if (!exist) {
            k_vEntitys kVEntitys = new k_vEntitys(hash, true, totalSize);
            storage[] storages = mqUtils.getAvailableStorages();
            ArrayList<Integer> targetStorages = new ArrayList<>();
            int length = storages.length;  //其实就是5
            //此处的思路是：storages获取到几个有空间的storage，遍历大文件分片依次填充到storages中
            //怎么解决填充时检查甚于空间呢？由于tracker和storage是异步的，在此只能模拟进行，每遍历过一次storage相当于空间少THRESHOLD，而遍历了几次信息在i中
            for (int i = 0; i < sliceNum; i++) {
                storage storage = storages[i % length];
                if (storage.startIndex < (THRESHOLD * i / length + 1)) {
                    targetStorages.add(storage.getStorageNum());
                }
            }
            kVEntitys.setValue(targetStorages.toString());
            mysqlUtils.insert(kVEntitys);
            return storages;
        } else {
            return null;
        }
    }

    @GetMapping("findFile")
    public String findFile(String hash) {
        //TODO 简化策略——如果要查找整个大文件，在client就按照其大小进行分片，对tracker只进行分片的查询
        k_vEntitys file = mysqlUtils.getFile(hash);
        return file.value;
    }
}
