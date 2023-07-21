package com.heima.search.Interceptor;

import com.heima.model.user.pojos.ApUser;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import com.heima.utils.thread.WmThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Slf4j
@Configuration
public class AppTokenInterceptor implements HandlerInterceptor {

    /**
     * 获取当前请求的用户信息 并存入当前线程中
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取用户信息
        String userID = request.getHeader("userId");
        if (StringUtils.hasText(userID)){
            ApUser apUser = new ApUser();
            apUser.setId(Integer.valueOf(userID));
            //将用户存在线程中
            AppThreadLocalUtil.setUser(apUser);
        }
        log.info("用户id:{}",userID);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.info("清理threadlocal...");
        AppThreadLocalUtil.clear();    }
}
