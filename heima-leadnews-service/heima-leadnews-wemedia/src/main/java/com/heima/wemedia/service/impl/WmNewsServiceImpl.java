package com.heima.wemedia.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl  extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {


    @Override
    public ResponseResult findAll(WmNewsPageReqDto dto) {
        if (ObjectUtils.isEmpty(dto)){
            return  ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE,"前端未传对应参数");
        }
        //检查登录人信息
         WmUser user = WmThreadLocalUtil.getUser();
        if (ObjectUtils.isEmpty(user)||user.getId()==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE,"缺少用户信息");
        }
        //检查分页
        dto.checkParam();
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> lqw= new LambdaQueryWrapper();
        //查询当前用户的素材
        lqw.eq(WmNews::getUserId,user.getId());
        //查询草稿草稿
        lqw.eq(null!=dto.getStatus(),WmNews::getStatus,dto.getStatus());
        //关键字
        lqw.like(null!=dto.getKeyword(), WmNews::getTitle,dto.getKeyword());
        //频道
        lqw.eq(null!=dto.getChannelId(),WmNews::getChannelId,dto.getChannelId());
        //发布日期
        lqw.between(null!=dto.getBeginPubDate()&&null!=dto.getEndPubDate(), WmNews::getPublishTime,dto.getBeginPubDate(),dto.getEndPubDate());
        lqw.orderByDesc(WmNews::getPublishTime);
          page = page(page, lqw);
          ResponseResult responseResult =new PageResponseResult(dto.getPage(),dto.getSize(), (int) page.getTotal());
          responseResult.setData(page.getRecords());
        return responseResult;
    }
}
