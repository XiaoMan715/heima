package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;
import org.springframework.stereotype.Service;


public interface ApUserService extends IService<ApUser> {
    /**
     * 登录接口
     * @param loginDto 传入参数
     * @return
     */

    public ResponseResult Login(LoginDto loginDto);
}
