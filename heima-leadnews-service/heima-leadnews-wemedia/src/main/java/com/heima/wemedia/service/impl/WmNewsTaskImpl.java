package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.schedule.IScheduleClient;
import com.heima.common.constants.ScheduleConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.TaskTypeEnum;
import com.heima.model.schedule.dto.Task;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.utils.common.ProtostuffUtil;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Slf4j
public class WmNewsTaskImpl implements WmNewsTaskService {
    @Autowired
    private IScheduleClient scheduleClient;
    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Override
    @Async
    public void addNewsToTask(Integer id, Date publishTime) {
        log.info("添加任务到延迟服务中begin————————————————————");
        Task task =new Task();
        task.setExecuteTime(publishTime.getTime());
        task.setTaskType(TaskTypeEnum.NEWS_SCAN_TIME.getTaskType());
        task.setPriority(TaskTypeEnum.NEWS_SCAN_TIME.getPriority());
        WmNews wmNews =new WmNews();
        wmNews.setId(id);
        task.setParameters(ProtostuffUtil.serialize(wmNews));
         ResponseResult responseResult = scheduleClient.addTask(task);
          log.info("responseResult:{}",responseResult);
        log.info("添加任务到延迟服务中ending————————————————————");
    }

    @Override
    @Scheduled(fixedRate = 1000)
    public void scanNewsByTask() {
        log.info("拉取任务去执行---------begin");
         ResponseResult responseResult = scheduleClient.poll(TaskTypeEnum.NEWS_SCAN_TIME.getTaskType(), TaskTypeEnum.NEWS_SCAN_TIME.getPriority());
        if(responseResult.getCode().equals(200) && responseResult.getData() != null){
            String json_str = JSON.toJSONString(responseResult.getData());
            Task task = JSON.parseObject(json_str, Task.class);
            byte[] parameters = task.getParameters();
            WmNews wmNews = ProtostuffUtil.deserialize(parameters, WmNews.class);
            System.out.println(wmNews.getId()+"-----------");
            wmNewsAutoScanService.autoScanWmNews(wmNews.getId());
        }
        log.info("文章审核---消费任务执行---end---");

    }
}
