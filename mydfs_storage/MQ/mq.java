package com.example.mydfs_storage.MQ;

import com.example.mydfs_storage.utils.spaceUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class mq {

    @Autowired
    AmqpAdmin amqpAdmin;
    @Value("storageNum")
    Integer storageNum;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void autoHeartBeat(){
        //五个交换机，分别对应五个队列，每次心跳就是由交换机向对应队列发起信息
        DirectExchange directExchange = new DirectExchange(storageNum + ".heartBeat");
        amqpAdmin.declareExchange(directExchange);

        Queue queue = new Queue(storageNum+"heartBeatQueue");
        amqpAdmin.declareQueue(queue);

        Binding binding = new Binding(storageNum+"heartBeatQueue", Binding.DestinationType.QUEUE,storageNum+".heartBeat","heartBeat",null);
        amqpAdmin.declareBinding(binding);
    }

    @Scheduled(fixedRate = 5000)
    public void heartBeat(){
        double restSpace = spaceUtils.getRestSpace();
        String msg = storageNum + ":"+restSpace;
        rabbitTemplate.convertAndSend(storageNum+"heartBeat","heartBeatQueue",msg);
    }
}
