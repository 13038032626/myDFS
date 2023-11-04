package com.example.mydfs_storage.controller;

import com.example.mydfs_storage.spaceController.FileSelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@RestController
public class ThreeController {

    /*
        用于找文件
     */

    @Autowired
    FileSelf fileSelf;
    public Integer findFileIndex(String hash){
        List<String> hashes = fileSelf.getHashes();
        int index = hashes.indexOf(hash);
        return index;
    }
    public byte[] getFile(Integer index) throws IOException {
        int startIndex = 4*1024*1024*index;
        byte[] bytes = new byte[4*1024*1024];
        FileInputStream inputStream = new FileInputStream("1GBFile.dat");
        inputStream.read(bytes,startIndex,bytes.length);
        return bytes;
    }
}
