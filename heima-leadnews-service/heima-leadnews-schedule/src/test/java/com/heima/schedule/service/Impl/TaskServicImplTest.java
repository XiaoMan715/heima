package com.heima.schedule.service.Impl;

import com.heima.model.schedule.dto.Task;
import com.heima.schedule.ScheduleApplication;
import com.heima.schedule.service.TaskServic;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForOffsetDateTime;
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

        for (int i = 0; i < 5; i++) {
            Task task =new Task();
            task.setTaskType(100+i);
            task.setPriority(50);
            task.setParameters("task test".getBytes());
            task.setExecuteTime(new Date().getTime()+500*i);
            final long l = taskServic.addTask(task);

        }

    }

    @Test
    public void cacheTesk(){
        final boolean b = taskServic.cancelTask(1667724670767759361L);
        System.out.println(b);
    }
    @Test
    public void taskPull(){
        System.out.println(taskServic.poll(100, 50));
    }

}