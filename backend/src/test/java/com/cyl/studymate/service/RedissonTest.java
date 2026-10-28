package com.cyl.studymate.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


@SpringBootTest
@Disabled("需要外部 Redis，禁止修改开发环境 Redis")
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    void test() {
        // list，数据存在本地 JVM 内存中
        List<String> list = new ArrayList<>();
        list.add("testUser");
        System.out.println("list:" + list.get(0));

//        list.remove(0);

        // 数据存在 redis 的内存中
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("testUser");
        System.out.println("rlist:" + rList.get(0));
//        rList.remove(0);

        // map

        // set

        // stack


    }
}
