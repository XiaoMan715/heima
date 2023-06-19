package com.heima.article.listener;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.article.service.ApArticleConfigService;
import com.heima.common.constants.RabbitConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class RabbitDownListener {
    @Autowired
    private ApArticleConfigService apArticleConfigService;

    @RabbitListener(queues = RabbitConstants.QUEUE_NAME)
    public void onMessage(String message) {
        if (StringUtils.isNotBlank(message)) {
            Map map = JSON.parseObject(message, Map.class);
            apArticleConfigService.updateByMap(map);
            log.info("article端文章配置修改，articleId={}", map.get("articleId"));
        }
    }

   /* @RabbitListener(queues = RabbitConstants.QUEUE_NAME)
    public void onMessage(String message) {
        if (StringUtils.isNotBlank(message)) {
         *//*   Map map = JSON.parseObject(message, Map.class);
            apArticleConfigService.updateByMap(map);
            log.info("article端文章配置修改，articleId={}", map.get("articleId"));*//*
            log.info("我发送来的消息是是：{}",message);
        }
    }*/

}
