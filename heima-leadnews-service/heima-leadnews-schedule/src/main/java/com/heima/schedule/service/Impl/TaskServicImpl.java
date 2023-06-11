package com.heima.schedule.service.Impl;

import com.alibaba.fastjson.JSON;
import com.heima.common.constants.ScheduleConstants;
import com.heima.common.exception.CustomException;
import com.heima.common.redis.CacheService;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.schedule.dto.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskServic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

@Slf4j
@Service
public class TaskServicImpl implements TaskServic {
    @Autowired
    private TaskinfoMapper taskinfoMapper;
    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;
    @Autowired
    private CacheService cacheService;


    @Transactional
    @Override
    public long addTask(Task task) {
        //1添加任务到数据库中 落库
        boolean success = addTaskDb(task);
        //2添加任务到redis中
        if (success){
            addTaskToCache(task);
        }
        //2.1如果执行任务的时间小于当前时间 说明要立即执行
        //2.2如果执行任务的时间大于当前时间，并且小于预设时间(
        // 预设时间用来防止同时有太多消息导致zset阻塞，所以我们设置预设时间来只有在预设时间这个区间的才写入zset)

        return task.getTaskId();
    }

    /**
     *把任务添加到redis中
     * @param task
     */
    private void addTaskToCache(Task task) {
        if (task == null) {
            throw  new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        String key =task.getTaskType()+"_"+task.getPriority();
        //获取五分钟之后的时间
        Calendar calendar =Calendar.getInstance();
        calendar.add(Calendar.MINUTE,5);
         long nextScheduleTime = calendar.getTimeInMillis();
        //如果任务时间小于当前时间 加入redis 的list中立即执行
        if (task.getExecuteTime()<=System.currentTimeMillis()){
           cacheService.lLeftPush(ScheduleConstants.TOPIC+key, JSON.toJSONString(task));
        } else if (task.getExecuteTime()<nextScheduleTime&&task.getExecuteTime()>System.currentTimeMillis()) {
         //如果任务时间大于当前时间 又小于预设时间 存入稍后执行的zset中
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), task.getExecuteTime());
        }

    }

    @Transactional
    public boolean addTaskDb(Task task) {
        if (task == null) {
            throw  new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        boolean flag = false;

        try {
            //保存任务表
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);
            //设置taskid
            task.setTaskId(taskinfo.getTaskId());

            //保存任务日志表
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            //设置乐观锁
            taskinfoLogs.setVersion(1);
            //设置执行状态 0=初始化状态 1=EXECUTED 2=CANCELLED
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            taskinfoLogsMapper.insert(taskinfoLogs);
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }
}
