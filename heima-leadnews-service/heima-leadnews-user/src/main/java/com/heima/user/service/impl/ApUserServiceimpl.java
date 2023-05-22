package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserService;
import com.heima.utils.common.AppJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.sql.Wrapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class ApUserServiceimpl extends ServiceImpl<ApUserMapper, ApUser> implements ApUserService {
    /**
     * 登录接口实现方法
     * 两种登录模式 用户和游客
     * 当有phone和密码为用户登录 直接登录并返回基于用户id生成jwt
     * 当没有为游客登录 用0去生成jwt返回
     * @param loginDto 传入参数
     * @return
     */

    @Override
    public ResponseResult Login(LoginDto loginDto) {
        log.info("登录信息:{}",loginDto);
        Map map;
        //正常登录 需要用户名和密码
        if (StringUtils.hasText(loginDto.getPhone())&&StringUtils.hasText(loginDto.getPassword())){
            //查询数据库是否有对应用户
             ApUser dbuser = getOne( Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone,loginDto.getPhone()));
             if (ObjectUtils.isEmpty(dbuser)){
                 return  ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"用户信息不存在");
             }
             //比对密码
             String password = loginDto.getPassword();
             String salt = dbuser.getSalt();
             //加盐后得到密码
             String saltPassword = DigestUtils.md5DigestAsHex((password + salt).getBytes());

             if (!StringUtils.hasText(dbuser.getPassword()) && !dbuser.getPassword().equals(saltPassword)){
                 log.info("数据库密码：{}， 用户输入密码：{}",dbuser.getPassword(),saltPassword);
                 return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR,"用户密码错误");
             }
             //如果密码正确返回用户id jwt
             String token = AppJwtUtil.getToken(dbuser.getId().longValue());
             map =new HashMap();
             map.put("token",token);
             dbuser.setSalt("");
             dbuser.setPassword("");
             map.put("user",dbuser);
             return ResponseResult.okResult(map);

        }else {
            //游客登录
             map =new HashMap();
             map.put("token",AppJwtUtil.getToken(0L));
            return ResponseResult.okResult(map);
        }


    }
}
