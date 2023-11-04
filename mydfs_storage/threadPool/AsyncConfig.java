package com.example.mydfs_storage.threadPool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public fileUploadExecutor fileUploadExecutor(){
        fileUploadExecutor executor = new fileUploadExecutor(
                10,
                20,
                50,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadPoolExecutor.DiscardPolicy()
        );
        return executor;
    }
    @Bean
    public fileCopyExecutor fileCopyExecutor(){

        fileCopyExecutor executor = new fileCopyExecutor(
                10,
                20,
                50,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadPoolExecutor.DiscardPolicy()
        );
        return executor;

    }
    @Bean
    public fileAlterExecutor fileAlterExecutor(){

        fileAlterExecutor executor = new fileAlterExecutor(
                10,
                20,
                50,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadPoolExecutor.DiscardPolicy()
        );
        return executor;

    }
}
