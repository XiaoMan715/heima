package com.heima.schedule.service.Impl;

import com.heima.model.schedule.dto.Task;
import com.heima.schedule.ScheduleApplication;
import com.heima.schedule.service.TaskServic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;

import javax.xml.crypto.Data;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
@Slf4j
class TaskServicImplTest {
@Autowired
private TaskServic taskServic;
    @Test
    void addTask() {
        Task task =new Task();
        task.setTaskType(100);
        task.setPriority(50);
        task.setParameters("task test".getBytes());
        task.setExecuteTime(new Date().getTime()+5000000);
        final long l = taskServic.addTask(task);
        log.info("taskid:{}",l);
    }
}