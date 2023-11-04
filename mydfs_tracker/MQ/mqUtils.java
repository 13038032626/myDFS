package com.example.mydfs_tracker.MQ;

import com.example.mydfs_tracker.storage;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class mqUtils {

    @Autowired
    AmqpAdmin amqpAdmin;
    storage[] currentStorages;


    @PostConstruct
    public void autoHeartBeat(){
        currentStorages = new storage[5];
        for (int i = 0; i < currentStorages.length; i++) {
            storage storage = new storage("127.0.0.1",String.valueOf(10000+i));
            currentStorages[i] = storage;
        }
        new Thread(()->{
            while (true){
                for (storage s:currentStorages) {
                    detectHeartBeat(s);
                }
            }
        }).start();
    }

    /*
    思路：
    由于listener都是被动接收，为了避免轮询检测是否宕机，如果设计成每次心跳检查和上次心跳的时间间隔，一台storage下线将永远检查不出来
    我将每次心跳时都检查一遍在当时和其余机器上次心跳的间隔
     */

    @RabbitListener(queues = {"1heartBeatQueue"})
    public synchronized void receiveStorage1(String message){
        long currentTimeMillis = System.currentTimeMillis();
        storage currentStorage = currentStorages[0];
        currentStorage.setLastTime(currentTimeMillis);
        String[] split = message.split(":");
        Integer startIndex = Integer.parseInt(split[1]);
        currentStorage.setStartIndex(startIndex);

    }

    @RabbitListener(queues = {"2heartBeatQueue"})
    public synchronized void receiveStorage2(String message){
        long currentTimeMillis = System.currentTimeMillis();
        storage currentStorage = currentStorages[1];
        currentStorage.setLastTime(currentTimeMillis);
        String[] split = message.split(":");
        Integer startIndex = Integer.parseInt(split[1]);
        currentStorage.setStartIndex(startIndex);

    }
    @RabbitListener(queues = {"3heartBeatQueue"})
    public synchronized void receiveStorage3(String message){
        long currentTimeMillis = System.currentTimeMillis();
        storage currentStorage = currentStorages[2];
        currentStorage.setLastTime(currentTimeMillis);
        String[] split = message.split(":");
        Integer startIndex = Integer.parseInt(split[1]);
        currentStorage.setStartIndex(startIndex);

    }
    @RabbitListener(queues = {"4heartBeatQueue"})
    public synchronized void receiveStorage4(String message){
        long currentTimeMillis = System.currentTimeMillis();
        storage currentStorage = currentStorages[3];
        currentStorage.setLastTime(currentTimeMillis);
        String[] split = message.split(":");
        Integer startIndex = Integer.parseInt(split[1]);
        currentStorage.setStartIndex(startIndex);

    }
    @RabbitListener(queues = {"5heartBeatQueue"})
    public synchronized void receiveStorage5(String message){
        long currentTimeMillis = System.currentTimeMillis();
        storage currentStorage = currentStorages[4];
        currentStorage.setLastTime(currentTimeMillis);
        String[] split = message.split(":");
        Integer startIndex = Integer.parseInt(split[1]);
        currentStorage.setStartIndex(startIndex);

    }
    private void detectHeartBeat(storage currentStorage) {
        long currentTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < currentStorages.length; i++) {
            storage storage = currentStorages[i];
            if(currentTimeMillis-storage.getLastTime()>20000){
                System.out.println(i+1+"号挂机");
                currentStorage.setOK(false);
            }
        }
    }

    public storage[] getAvailableStorages() {
        //新线程订阅心跳信息,心跳信息中尽量要包括每个storage的甚于容量，次方法返回的int[]按照容量由大到小
        storage[] availableStorage = currentStorages.clone();
        Arrays.sort(availableStorage);
        return availableStorage;

    }

}
