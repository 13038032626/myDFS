package com.example.mydfs_storage.controller;

import com.example.mydfs_storage.spaceController.FileSelf;
import com.example.mydfs_storage.threadPool.fileAlterExecutor;
import com.example.mydfs_storage.utils.LockUtils;
import com.example.mydfs_storage.utils.fileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class TwoController {

    /*
        用于修改文件

        思路：
        上层传来更改的slice编号，以及更改后的File，如果新文件超过slice最大值，将超过的部分设置成新slice加在此storage后
        每个slice头部又元数据 --
            1. 下一个分片的位置
            2. 下一个分片的index
            3. 有效信息的长度（垃圾回收时使用）
        -- slice根据头部信息找到下一个分片

        Attention：
        解决并发写 -- 乐观锁 + 写时复制技术
     */

    @Autowired
    fileUtils fileUtils;

    @Autowired
    FileSelf fileSelf;

    @Autowired
    OneController oneController;

    @Autowired
    fileAlterExecutor fileAlterExecutor;

    @Autowired
    LockUtils lockUtils;

    Boolean canCommit = false;

    @Transactional
    public boolean alter(Integer num, MultipartFile newFile) throws ExecutionException, InterruptedException, IOException { //上层切记：修改的file可能有无效区域，增加内容优先占用无效区域
        long size = newFile.getSize();
        Object lock = lockUtils.getLocks().get(num);
        synchronized (lock) {
            Integer lastStartIndex = fileSelf.getStartIndex();
            AtomicReference<Integer> addedStartIndex = null;
            Future<Boolean> future = fileAlterExecutor.submit(() -> {
                try {
                    if (size < 4 * 1024 * 1024) {
                        InputStream inputStream = newFile.getInputStream();
                        RandomAccessFile raf = new RandomAccessFile("1GBFile.dat", "rw");
                        raf.seek(fileSelf.getStartIndex());
                        byte[] bytes = new byte[1024];
                        int readLen = -1;
                        while ((readLen = inputStream.read(bytes)) != -1) {
                            raf.write(bytes);
                        }
                        fileSelf.addFile();
                        return true;
                    } else {
                        if (fileSelf.getStartIndex() + 4 * 1024 * 1024 < fileSelf.getTotalSize()) {
//                                throw new ArrayIndexOutOfBoundsException("存不下了");
                            return false;
                        }
                        Integer nextSliceLocation = fileUtils.getNextSliceLocation(newFile);
                        Integer nextSliceIndex = fileUtils.getNextSliceIndex(newFile);
                        InputStream inputStream = newFile.getInputStream();
                        RandomAccessFile raf = new RandomAccessFile("1GBFile.dat", "rw");
                        byte[] bytes = new byte[1024];
                        byte[] helpBytes = new byte[1018];

                        int readLen = -1;
                        int times = 0;

                        //流中读取接下来的slice，和第一个不同，slice设置到storage末尾
                        ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream(4 * 1024 * 1024);
                        while ((readLen = inputStream.read(bytes)) != -1) {
                            if (times % (4 * 1024) == 4 * 1023 && inputStream.available() != 0) {
                                //读到最后一个片，应该读1018了
                                inputStream.read(helpBytes);
                                raf.write(helpBytes);
                                fileUtils.changeNext(fileSelf.getStartIndex(), fileSelf.getStorageNum());
                                fileUtils.setNextSliceIndex(fileSelf.getStartIndex(), fileSelf.getStartIndex());
                                fileSelf.addFile();
                                byte[] byteArray = outputStream1.toByteArray();
                                oneController.uploadOneSlice(new ByteArrayInputStream(byteArray));
                                outputStream1.reset();
                            } else {    // 最终出口
                                inputStream.read(helpBytes);
                                raf.write(helpBytes);
                                fileUtils.changeNext(fileSelf.getStartIndex(), nextSliceLocation);
                                fileUtils.setNextSliceIndex(fileSelf.getStartIndex(), nextSliceIndex);
                                fileSelf.addFile();
                                byte[] byteArray = outputStream1.toByteArray();
                                oneController.uploadOneSlice(new ByteArrayInputStream(byteArray));
                                outputStream1.reset();
                                addedStartIndex.set(fileSelf.getStartIndex());
                            }
                            outputStream1.write(bytes, 0, readLen);
                            times++;
                        }
                        InputStream inputStream2 = newFile.getInputStream();
                        RandomAccessFile raf2 = new RandomAccessFile("1GBFile.dat", "rw");
                        raf2.seek(fileSelf.getStartIndex());
                        byte[] bytes2 = new byte[1024];
                        int readLen2 = -1;
                        while ((readLen2 = inputStream.read(bytes)) != -1) {
                            raf.write(bytes);
                        }
                        fileSelf.addFile();
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });
            Boolean aBoolean = future.get();
            if (aBoolean) { //阻塞，等待commit / rollback指令
                lock.wait(3000); //等待被唤醒
                if (canCommit) { //执行到此有两种可能 - 被唤醒canCommit / 超时 !canCommit
                    //执行提交逻辑
                    if (size < 4 * 1024 * 1024) {
                        InputStream inputStream = newFile.getInputStream();
                        RandomAccessFile raf = new RandomAccessFile("1GBFile.dat", "rw");
                        raf.seek(num * 4 * 1024 * 1024);
                        byte[] bytes = new byte[1024];
                        int readLen = -1;
                        while ((readLen = inputStream.read(bytes)) != -1) {
                            raf.write(bytes);
                        }
                        fileSelf.setStartIndex(addedStartIndex.get());
                        return true;
                    } else {
                        InputStream inputStream = newFile.getInputStream();
                        RandomAccessFile raf = new RandomAccessFile("1GBFile.dat", "rw");
                        byte[] bytes = new byte[1024];
                        byte[] helpBytes = new byte[1018];
                        int readLen = -1;
                        int times = 0;
                        raf.seek(num * 4 * 1024 * 1024);
                        //流中读取第一个slice，并将第一个slice的next设置为自身，nextIndex设置为末尾
                        while (times < 4 * 1023) {
                            inputStream.read(bytes);
                            raf.write(bytes);
                            times++;
                        }
                        inputStream.read(helpBytes);
                        raf.write(helpBytes);
                        times = 0;
                        fileUtils.changeNext(num, fileSelf.getStorageNum());
                        fileUtils.setNextSliceIndex(num, fileSelf.getStorageNum());
                        fileSelf.setStartIndex(lastStartIndex);
                        return true;
                    }
                } else {
                    //执行回滚逻辑
                    if (size >= 4 * 1024 * 1024) {
                        fileSelf.setStartIndex(lastStartIndex);
                    }
                    return false;
                }
            } else { //自身有问题 -> 回滚
                if (size >= 4 * 1024 * 1024) {
                    fileSelf.setStartIndex(lastStartIndex);
                }
                return false;
            }
        }
    }

    public void commit(Integer num) {
        lockUtils.getLocks().get(num).notifyAll();
        canCommit = true;
    }
}

