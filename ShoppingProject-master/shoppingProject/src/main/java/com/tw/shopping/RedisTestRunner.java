package com.tw.shopping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisTestRunner implements CommandLineRunner {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // 1. 測試寫入資料
            System.out.println("----------------------------------------");
            System.out.println("正在測試 Redis 連線 (目標 IP: 43.203.134.113)...");
            
            redisTemplate.opsForValue().set("test:connection", "Hello from Spring Boot!");
            System.out.println("寫入測試: 成功 (Key: test:connection)");

            // 2. 測試讀取資料
            String value = redisTemplate.opsForValue().get("test:connection");
            System.out.println("讀取測試: 成功 (Value: " + value + ")");
            
            System.out.println("Redis 連線測試成功！");
            System.out.println("----------------------------------------");
        } catch (Exception e) {
            System.err.println("Redis 連線失敗！請檢查錯誤訊息：");
            e.printStackTrace();
        }
    }
}