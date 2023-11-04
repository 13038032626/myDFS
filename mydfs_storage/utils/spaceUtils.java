package com.example.mydfs_storage.utils;

import org.springframework.beans.factory.annotation.Value;

import java.io.File;

public class spaceUtils {

    @Value("savePath")
    static String savePath;
    public static double getRestSpace(){
        File folder = new File(savePath);

        long size = calculateFolderSize(folder);
        double folderSizeMB = (double) size /(1024*1024);

        //假设每个节点的预设大小是1024MB
        return 1024 - folderSizeMB;
    }

    private static long calculateFolderSize(File folder){
        long size = 0;
        if(folder.isDirectory()){
            File[] files = folder.listFiles();
            if(files!=null){
                for (File file: files) {
                    if(file.isFile()){
                        size+=file.length();
                    }else if(file.isDirectory()){
                        size+=calculateFolderSize(file);
                    }
                }
            }
        }
        return size;
    }
}
