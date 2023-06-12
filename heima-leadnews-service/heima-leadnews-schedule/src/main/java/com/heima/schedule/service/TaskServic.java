package com.heima.schedule.service;

import com.heima.model.schedule.dto.Task;

public interface TaskServic {
    /**
     * 添加延时任务
     * @param task
     * @return
     */
    public long addTask(Task task);

    /**
     * 取消延时任务
     * @param taskId
     * @return
     */
    public boolean cancelTask(long taskId);

    /**
     * 按照任务类型和优先级拉去任务
     * @param type 类型
     * @param priority 优先级
     * @return
     */
    public Task poll(int type,int priority);
}
