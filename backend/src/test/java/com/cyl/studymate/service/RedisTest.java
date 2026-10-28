package com.cyl.studymate.service;


import com.cyl.studymate.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
@Disabled("需要外部 Redis，禁止修改开发环境 Redis")
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    public void test() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("testUser", "girl");
        valueOperations.set("testUserInt", 1);
        valueOperations.set("testUserDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        valueOperations.set("testUser", user);

        // 查
        Object testUser = valueOperations.get("testUserString");
        Assertions.assertTrue("girl".equals((String) testUser));
        testUser = valueOperations.get("testUserInt");
        Assertions.assertTrue(1 == (Integer) testUser);
        testUser = valueOperations.get("testUserDouble");
        Assertions.assertTrue(2.0 == (Double) testUser);
        System.out.println(valueOperations.get("testUser"));
        valueOperations.set("testUserString", "girl");

        //删
//        redisTemplate.delete("testUserString");
    }

}
