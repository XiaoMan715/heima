package com.heima.schedule;

import com.heima.common.redis.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
@Slf4j
public class redisTest {

    @Autowired
    CacheService cacheService;
    @Test
    public void listTest(){
        cacheService.lLeftPush("list_oo1","hello_redis");
         String list_oo1 = cacheService.lRightPop("list_oo1");
         log.info("list_oo1:{}",list_oo1 );
    }
}
