package com.heima.wemedia.interceptor;

import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HeaderIterator;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Slf4j
@Configuration
public class WmTokenInterceptor implements HandlerInterceptor {
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
         String userID = request.getHeader("userID");
         if (StringUtils.hasText(userID)){
             WmUser wmUser = new WmUser();
             wmUser.setApUserId(Integer.valueOf(userID));
             //将用户存在线程中
             WmThreadLocalUtil.setUser(wmUser);
         }
         log.info("用户id:{}",userID);

        return true;
    }

    /**
     * 清理当前线程的数据
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.info("清理threadlocal...");
        WmThreadLocalUtil.clear();

    }


}
