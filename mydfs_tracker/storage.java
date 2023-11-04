package com.example.mydfs_tracker;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
public class storage implements Comparable<storage> {
    Integer storageNum;
    String IP;
    String port;
    boolean isOK;
    Integer startIndex;
    long lastTime = 0;

    public storage(String ip, String port) {
        this.IP = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return "storage{" +
                "storageNum=" + storageNum +
                ", IP='" + IP + '\'' +
                ", port='" + port + '\'' +
                ", isOK=" + isOK +
                ", restSpace=" + startIndex +
                ", lastTime=" + lastTime +
                '}';
    }

    @Override
    public int compareTo(storage o) {
        return o.startIndex-this.startIndex;
    }
}
