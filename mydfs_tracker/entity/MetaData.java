package com.example.mydfs_tracker.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class MetaData implements Serializable {

    String fileName;

    Integer[] location;
}
