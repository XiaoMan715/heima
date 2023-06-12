package com.heima.schedule.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
        if (success) {
            addTaskToCache(task);
        }
        //2.1如果执行任务的时间小于当前时间 说明要立即执行
        //2.2如果执行任务的时间大于当前时间，并且小于预设时间(
        // 预设时间用来防止同时有太多消息导致zset阻塞，所以我们设置预设时间来只有在预设时间这个区间的才写入zset)

        return task.getTaskId();
    }

    @Transactional
    public boolean addTaskDb(Task task) {
        if (task == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
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


    /**
     * 把任务添加到redis中
     * 如果任务小于当前时间 说明要立即执行的任务 加入到list集合中去执行
     * 如果任务大于当前时间并且小于预设的时间 说明任务将要执行的的 加入zset中 按时间做分数去
     * 如果任务大于当前时间并且大于预设时间，说明任务不成执行的 就放在数据库中不动
     *
     * @param task
     */
    private void addTaskToCache(Task task) {
        if (task == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        String key = task.getTaskType() + "_" + task.getPriority();
        //获取五分钟之后的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        long nextScheduleTime = calendar.getTimeInMillis();
        //如果任务时间小于当前时间 加入redis 的list中立即执行
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));
        } else if (task.getExecuteTime() < nextScheduleTime && task.getExecuteTime() > System.currentTimeMillis()) {
            //如果任务时间大于当前时间 又小于预设时间 存入稍后执行的zset中
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), task.getExecuteTime());
        }

    }


    /**
     * 取消延时任务
     *
     * @param taskId
     * @return
     */
    @Override
    public boolean cancelTask(long taskId) {
        boolean flag = false;
        //删除任务 更新任务日志
        Task task = updataDb(taskId, ScheduleConstants.CANCELLED);
        //删除redis
        if (ObjectUtils.isEmpty(task)) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        removeTaskToCache(task);
        flag = true;
        return flag;
    }

    /**
     * 删除数据的里任务 并且修改日志表的状态
     *
     * @param taskId
     * @param status
     * @return
     */
    private Task updataDb(long taskId, int status) {
        Task task = null;
        try {
            //删除数据库任务
            taskinfoMapper.deleteById(taskId);
            TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
            taskinfoLogs.setStatus(status);
            //修改日志表里的状态
            taskinfoLogsMapper.updateById(taskinfoLogs);
            task = new Task();
            BeanUtils.copyProperties(taskinfoLogs, task);
            task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());
        } catch (Exception e) {
            log.error("task checal taskid:{}", taskId);
        }

        return task;
    }

    /**
     * 删除缓存中的任务
     *
     * @param task
     */
    private void removeTaskToCache(Task task) {

        if (ObjectUtils.isEmpty(task)) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        String key = task.getTaskType() + "_" + task.getPriority();
        //如果存在数据库的时间小于了当前时间，说明已经被插入的执行的list中了
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            //0删除所有的 小于0从尾部删除第一个 大于0从头部删除第一个
            cacheService.lRemove(ScheduleConstants.TOPIC + key, 0, JSON.toJSONString(task));
        } else {//从待执行的列表删除任务
            cacheService.zRemove(ScheduleConstants.FUTURE + key, JSON.toJSONString(task));
        }
    }


    /**
     * @param type     类型
     * @param priority 优先级
     * @return
     */
    @Override
    public Task poll(int type, int priority) {
        Task task = null;
        try {
            String key = type + "_" + priority;
            //从redis中拉去数据
            String task_json = cacheService.lRightPop(ScheduleConstants.TOPIC + key);
            if (StringUtils.hasText(task_json)) {
                task = JSON.parseObject(task_json, Task.class);
                //修改数据库信息 删除对应库并修改日志
                updataDb(task.getTaskId(), ScheduleConstants.EXECUTED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("task:{}", task);
        }
        return task;
    }

    /**
     * 未来数据zset列表定时刷新到执行列表中
     */
    @Scheduled(cron = "0 * * * * ?")
    public void refresh() {
        //加锁 防止多个线程调用出现并发
        String future_task_sync = cacheService.tryLock("FUTURE_TASK_SYNC", 1000 * 30);
        if (StringUtils.hasText(future_task_sync)) {

            log.info("定时刷新数据");
            //获取未来数据集合中所有的key
            Set<String> futuerkeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
            //根据key 和分值去查询符合条件的value
            for (String futuerkey : futuerkeys) {
                //获取当前数据的key
                String key = ScheduleConstants.TOPIC + futuerkey.split(ScheduleConstants.FUTURE)[1];
                //查询0到当前时间的所有value值
                Set<String> tasks = cacheService.zRangeByScore(futuerkey, 0, System.currentTimeMillis());
                //同步到立即执行的列表中
                if (!tasks.isEmpty()) {
                    cacheService.refreshWithPipeline(futuerkey, key, tasks);
                    log.info("刷新成功");
                }

            }
        }
    }

    /**
     * 定时刷新数据库的到reids中
     */
    @PostConstruct
    @Scheduled(cron = "0 */5 * * * ?")
    public void reloadDB() {
        //清理缓存中的数据 保证不重复添加
        clearCache();
        //查询符合条件的数据 小于当前时间五分钟的
        LambdaQueryWrapper<Taskinfo> lwq = new LambdaQueryWrapper();
        //获取五分钟之后的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        lwq.lt(Taskinfo::getExecuteTime, calendar.getTime());
        List<Taskinfo> taskinfos = taskinfoMapper.selectList(lwq);
        if (!CollectionUtils.isEmpty(taskinfos) && taskinfos.size() > 0) {
            //把任务添加到redis数据中
            for (Taskinfo taskinfo : taskinfos) {
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo, task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                addTaskToCache(task);
            }
        }

        log.info("数据库的任务同步到redis中");
    }

    /**
     * 清理所有的缓存
     */
    public void clearCache() {
        //先查询两个列表的所有的key
        Set<String> topicKeys = cacheService.scan(ScheduleConstants.TOPIC + "*");
        Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
        cacheService.delete(topicKeys);
        cacheService.delete(futureKeys);

    }

}
