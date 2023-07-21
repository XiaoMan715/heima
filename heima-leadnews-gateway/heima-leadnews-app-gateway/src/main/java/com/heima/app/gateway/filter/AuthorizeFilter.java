package com.heima.app.gateway.filter;

import com.heima.utils.common.AppJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关
 */
@Component
@Slf4j
public class AuthorizeFilter implements Ordered, GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取request和response
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //2.判断是否登录 如果当前的请求连接里面包含了Login 就是登录接口 直接放行去登录
        if (request.getURI().getPath().contains("/login")) {
            return chain.filter(exchange);
        }
        //3.获取token
        String token = request.getHeaders().getFirst("token");
        //4.判断token是否存在
        if (!StringUtils.hasText(token)) {
            //不存在 响应码设置为401 并且返回
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        try {
            //5.判断token是否有效
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            //获取当前用户id
            String userId = claimsBody.get("id").toString();

            //将数据存在header中
            ServerHttpRequest serverHttpRequest = request.mutate().headers(httpHeaders -> {
                httpHeaders.add("userId", userId);
            }).build();
            exchange.mutate().request(serverHttpRequest).build();
            log.info("解析token：{}",claimsBody);
            //判断token是否过期 过期时间是一天
            if (AppJwtUtil.verifyToken(claimsBody) > 0) {
                log.info("校验过期返回值：{}",AppJwtUtil.verifyToken(claimsBody));
                //返回值大于0就是过期了
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        //6.放行

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
