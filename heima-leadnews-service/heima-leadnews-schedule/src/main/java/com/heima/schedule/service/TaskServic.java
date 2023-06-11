package com.heima.schedule.service;

import com.heima.model.schedule.dto.Task;

public interface TaskServic {
    /**
     * 添加延时任务
     * @param task
     * @return
     */
    public long addTask(Task task);
}
