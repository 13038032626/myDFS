package com.example.mydfs_tracker;

import lombok.Data;

@Data
public class k_vEntitys {

    String key; //文件的唯一hash
    String value; //地址吧
    Boolean isBig;
    Integer totalSize;

    public k_vEntitys(String key, boolean isBig, Integer totalSize) {
        this.key = key;
        this.isBig = isBig;
        this.totalSize = totalSize;
    }

    public k_vEntitys() {
    }
}
