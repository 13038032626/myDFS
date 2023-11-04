package com.example.mydfs_tracker.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Data
public class MainMap {
    public ConcurrentHashMap<String,Integer[]> map = new ConcurrentHashMap<>();
}
