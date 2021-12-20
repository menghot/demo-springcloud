package com.example.demospringcloud.controller;

import com.bmsoft.dc.client.BaseServiceFeignClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class SystemController {

    String streamKey = "sc-test";
    String consumerName = "sc-test-0";
    String consumerGroupName = "sc-test-group";

    @Autowired
    private BaseServiceFeignClient baseServiceFeignClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/info")
    public ResponseEntity<String> info() {
        System.out.println(baseServiceFeignClient.getDataModelById(1234L));
        System.out.println(baseServiceFeignClient);
        return ResponseEntity.ok("success call info");
    }

    @Bean
    public StreamMessageListenerContainer container(RedisConnectionFactory redisConnectionFactory) {
        StreamMessageListenerContainer container = StreamMessageListenerContainer.create(
                redisConnectionFactory);
        container.start();
        return container;
    }

    @Scheduled(fixedRate = 1000, initialDelay = 1000)
    public void triggerMessage() throws JsonProcessingException {

        Map<String, Object> data = new HashMap<>();
        data.putIfAbsent("name", "simon");
        data.putIfAbsent("age", 11);
        data.putIfAbsent("date", new Date().getTime());
        StreamRecords.mapBacked(data).withStreamKey(streamKey);

        RecordId recordId = this.stringRedisTemplate.opsForStream()
                .add(StreamRecords.mapBacked(data).withStreamKey(streamKey));

        //log.info(recordId.toString());
    }

    @Bean
    public Subscription subscription1(StreamMessageListenerContainer container) {

        try {
            stringRedisTemplate.opsForStream().groups(streamKey).stream().forEach(e -> log.info(e.groupName()));
        } catch (Exception e) {
            stringRedisTemplate.opsForStream().createGroup(streamKey, consumerGroupName);
        }

        Subscription subscription = container.receive(
                Consumer.from(consumerGroupName, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                (StreamListener<String, MapRecord<String, String, Object>>) message -> {
                    log.info("stream message。messageId={}, stream={}, body={}", message.getId(), message.getStream(), message.getValue());
                    //Map<String, Object> record = message.getValue();
                    //System.out.println(record);
                    //stringRedisTemplate.opsForStream().acknowledge(consumerGroupName, message);
                    throw new RuntimeException("test");
                });

        return subscription;
    }

//    private StreamMessageListenerContainerOptions<String, ObjectRecord<String, OrderDTO>> options() {
//        return StreamMessageListenerContainerOptions.builder()
//                .batchSize(400)
//                .executor(this.matchThreadPool)
//                .errorHandler(t -> log.error("redis msg listener error, e:{}", t.getMessage()))
//                .pollTimeout(Duration.ZERO)
//                .serializer(new StringRedisSerializer())
//                .targetType(OrderDTO.class)
//                .build();
//    }

}
