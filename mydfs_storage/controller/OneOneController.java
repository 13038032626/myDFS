package com.example.mydfs_storage.controller;

import com.example.mydfs_storage.spaceController.FileSelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

@RestController
public class OneOneController {

    @Autowired
    FileSelf fileSelf;

    public Boolean uploadSlice(InputStream inputStream) throws IOException {
        if(fileSelf.getStartIndex()>=fileSelf.getTotalSize()){
            return false;
        }
        fileSelf.addFile();
        Integer startIndex = fileSelf.getStartIndex();
        RandomAccessFile randomAccessFile = new RandomAccessFile(new File("1GBFile.dat"),"rw");
        randomAccessFile.seek(startIndex);
        randomAccessFile.write(inputStream.readAllBytes());
        return true;
    }

}
