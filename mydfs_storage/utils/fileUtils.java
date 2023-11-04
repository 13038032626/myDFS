package com.example.mydfs_storage.utils;

import com.example.mydfs_storage.controller.OneController;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@Component
public class fileUtils {
    public Integer getNextSliceLocation(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        byte[] locationByte = new byte[1];
        inputStream.read(locationByte);
        String nextLocation = new String(locationByte);
        return Integer.parseInt(nextLocation);
    }

    public Integer getNextSliceIndex(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        byte[] locationByte = new byte[1];
        inputStream.read(locationByte, 1, 1);
        String nextLocation = new String(locationByte);
        return Integer.parseInt(nextLocation);
    }

    public void setNextSliceIndex(MultipartFile file,int start, Integer index) throws IOException {
        FileOutputStream outputStream = new FileOutputStream((File) file);
        byte[] bytes = new byte[1];
        bytes[0] = index.byteValue();
        outputStream.write(bytes, start * 4 * 1024 * 1024+1, 1);
    }

    public void setNextSliceIndex(OutputStream outputStream,Integer start, Integer index) throws IOException {
        byte[] bytes = new byte[1];
        bytes[0] = index.byteValue();
        outputStream.write(bytes, start * 4 * 1024 * 1024+1, 1);
    }
    public void setNextSliceIndex(Integer index,Integer nextSliceIndex) throws IOException{
        RandomAccessFile raf = new RandomAccessFile("1GBFile.dat","rw");
        raf.seek(index*4*1024*1024+3);
        raf.write(nextSliceIndex);
    }
    public void setLastSliceIndex(Integer index,Integer nextSliceIndex) throws IOException{
        RandomAccessFile raf = new RandomAccessFile("1GBFile.dat","rw");
        raf.seek(index*4*1024*1024+1);
        raf.write(nextSliceIndex);
    }

    public Integer getValidSize(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        byte[] sizeByte = new byte[4];
        inputStream.read(sizeByte, 2, 4);
        String size = new String(sizeByte);
        return Integer.parseInt(size);
    }

    public void changeNext(MultipartFile file, Integer nextLocation) throws IOException {
        FileOutputStream outputStream = new FileOutputStream((File) file);
        byte[] bytes = new byte[1];
        bytes[0] = nextLocation.byteValue();
        outputStream.write(bytes, 0, 1);
    }

    public void changeNext(OutputStream outputStream, int index, Integer nextLocation) throws IOException {
        byte[] bytes = new byte[1];
        bytes[0] = nextLocation.byteValue();
        outputStream.write(bytes, index, 1);
    }

    public void changeNext(MultipartFile file, int index, Integer nextLocation) throws IOException {
        byte[] bytes = new byte[1];
        bytes[0] = nextLocation.byteValue();
        FileOutputStream outputStream = new FileOutputStream((File) file);
        outputStream.write(bytes, index, 1);
    }
    public void changeNext(int index,int nextLocation) throws IOException {
        RandomAccessFile raf = new RandomAccessFile("1GBFile.dat","rw");
        raf.seek(index*4*1024*1024+2);
        raf.write(nextLocation);
    }
    public void changeLast(int index,int nextLocation) throws IOException {
        RandomAccessFile raf = new RandomAccessFile("1GBFile.dat","rw");
        raf.seek(index*4*1024*1024);
        raf.write(nextLocation);
    }
}
