package com.example.mydfs_storage.controller;

import com.example.mydfs_storage.spaceController.FileSelf;
import com.example.mydfs_storage.threadPool.fileUploadExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@RestController
public class OneController {

    /*
        用于上传文件

        思路：
        链表化，上传的每个slice跟到后面

        Attention：
        解决并发写问题 写时复制
     */

    @Autowired
    FileSelf fileSelf;
    @Autowired
    fileUploadExecutor uploadExecutor;
    List<byte[]> addList = new ArrayList<>();
    int queuedNum = 0;
    Integer startIndex = fileSelf.getStartIndex();

    public Boolean uploadOneSlice(InputStream inputStream) throws IOException { //核心是解决并发写
        queuedNum++;
        if (startIndex + queuedNum * 4 * 1024 * 1024 > fileSelf.getTotalSize()) {
            return false;
        }
        uploadExecutor.execute(() -> {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(4 * 1024 * 1024);
                byte[] bytes = new byte[1024];
                int readLen = -1;
                while ((readLen = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, readLen);
                }
                byte[] totalBytes = outputStream.toByteArray();
                addList.add(totalBytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        if (uploadExecutor.getActiveCount() > uploadExecutor.getCorePoolSize()) {
            this.sumUpAdd();
        }
        return true;
    }
    @PostMapping("upload")
    public Boolean uploadOneSlice(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        return uploadOneSlice(inputStream);
    }

    public synchronized void sumUpAdd() throws IOException {
        for (byte[] bytes : addList) {
            writeFile(bytes);
        }
        queuedNum = 0;
    }

    public void writeFile(byte[] bytes) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        FileOutputStream outputStream = new FileOutputStream(new File("1GBFile.dat"),true);
        int readLen = -1;
        while ((readLen = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes);
        }
    }

    @PostMapping("append")
    public boolean append(MultipartFile file) throws IOException {
        return uploadOneSlice(file);
    }

}

