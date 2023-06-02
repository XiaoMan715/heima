package com.heima.wemedia.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private WmMaterialMapper wmMaterialMapper;
    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Override
    public ResponseResult findAll(WmNewsPageReqDto dto) {
        if (ObjectUtils.isEmpty(dto)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "前端未传对应参数");
        }
        //检查登录人信息
        WmUser user = WmThreadLocalUtil.getUser();
        if (ObjectUtils.isEmpty(user) || user.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "缺少用户信息");
        }
        //检查分页
        dto.checkParam();
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> lqw = new LambdaQueryWrapper();
        //查询当前用户的素材
        lqw.eq(WmNews::getUserId, user.getId());
        //查询草稿草稿
        lqw.eq(null != dto.getStatus(), WmNews::getStatus, dto.getStatus());
        //关键字
        lqw.like(null != dto.getKeyword(), WmNews::getTitle, dto.getKeyword());
        //频道
        lqw.eq(null != dto.getChannelId(), WmNews::getChannelId, dto.getChannelId());
        //发布日期
        lqw.between(null != dto.getBeginPubDate() && null != dto.getEndPubDate(), WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate());
        lqw.orderByDesc(WmNews::getPublishTime);
        page = page(page, lqw);
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        log.info("dto:{}", dto);
        if (ObjectUtils.isEmpty(dto) || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "为传入有效参数");
        }
        //获取当前用户id
        WmUser user = WmThreadLocalUtil.getUser();
        if (ObjectUtils.isEmpty(user) || user.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "当前用户不存在");
        }

        //新增
        //处理前端传来的数据
        WmNews wmNews = new WmNews();
        //设置当前用户id
        wmNews.setUserId(user.getId());
        BeanUtils.copyProperties(dto, wmNews);
        log.info("wmNews:{}", wmNews);
        //处理封面单图和多图
        if (dto.getImages() != null && dto.getImages().size() > 0) {
            //将list数组转成string 用逗号隔开
            String image = String.join(",", dto.getImages());
            log.info("image:{}", image);
            wmNews.setImages(image);
        }
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            wmNews.setType(null);

        }
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        log.info("type:{}",wmNews.getType());
        saveOrUpdateWmNews(wmNews);

        //判断是草稿还是保存 status提交为1  草稿为0 如果是草稿不需要保存素材关系直接先落库就行
        if (dto.getStatus().equals(WmNews.Status.NORMAL)) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        //如果不是草稿 就去保存
        //处理内容中的图片
        String content = dto.getContent();
        //将json数据转换成对应的List<Map>
        List<String> images = new ArrayList();
        try {
            List<Map> lists = objectMapper.readValue(content, new TypeReference<List<Map>>() {
            });
            images = lists.stream()
                    .filter(list -> list.get("type")
                            .equals("image")).
                    map(list -> (String) list.get("value"))
                    .collect(Collectors.toList());


        } catch (JsonProcessingException e) {
            throw new CustomException(AppHttpCodeEnum.PARAM_IMAGE_FORMAT_ERROR);
        }
        //处理内容里面的素材的关系
        saveContentImgs(images, wmNews.getId(), WemediaConstants.WM_CONTENT_REFERENCE);
        //不是草稿 处理封面的素材
        saveRelativeInfoForCover(dto, wmNews, images);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * * 第一个功能：如果当前封面类型为自动，则设置封面类型的数据
     * * 匹配规则：
     * * 1，如果内容图片大于等于1，小于3  单图  type 1
     * * 2，如果内容图片大于等于3  多图  type 3
     * * 3，如果内容没有图片，无图  type 0
     *
     * @param dto
     * @param wmNews
     * @param images 图片集合
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> images) {
        if (ObjectUtils.isEmpty(dto)) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //获取前端传来的images
        List<String> imageList = dto.getImages();
        //如果type=-1则为自动从内容中获取图片
        if (dto.getType() != null && dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            if (images.size() >= 3) {
                //三图
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                imageList = images.stream().limit(3).collect(Collectors.toList());
            } else if (images.size() >= 1 && images.size() < 3) {
                //单图
                wmNews.setType(WemediaConstants.WM_COVER_REFERENCE);
                imageList = images.stream().limit(1).collect(Collectors.toList());
            } else {
                //无图
                wmNews.setType(WemediaConstants.WM_CONTENT_REFERENCE);
            }
            if (!CollectionUtils.isEmpty(imageList)&&imageList.size()>0){
                 String join = String.join(",", imageList);
                 wmNews.setImages(join);

            }
            updateById(wmNews);
        }

        if (CollectionUtils.isEmpty(imageList)&&imageList.size()>0){
         saveContentImgs(imageList,dto.getId(),WemediaConstants.WM_COVER_REFERENCE);
        }


    }

    //处理内容里面的图片素材 并且保存
    private void saveContentImgs(List images, Integer newId, Integer type) {
        if (CollectionUtils.isEmpty(images)) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        LambdaQueryWrapper<WmMaterial> lwq = new LambdaQueryWrapper();
        lwq.in(WmMaterial::getUrl, images);
        List<WmMaterial> materials = wmMaterialMapper.selectList(lwq);
        if (CollectionUtils.isEmpty(materials) || materials.size() != images.size() || materials.size() == 0) {
            throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
        }
        //获取素材id集合
        List<Integer> ids = materials.stream().map(m -> m.getId()).collect(Collectors.toList());
        //素材id集合 和当前文章id 和 是否是内容引用的type 保存入库
        wmNewsMaterialMapper.saveRelations(ids, newId, type);

    }


    private void saveOrUpdateWmNews(WmNews wmNews) {
        if (wmNews.getId() == null) {
            //新增
            save(wmNews);
        } else {
          LambdaQueryWrapper<WmNewsMaterial> lwq= new LambdaQueryWrapper<>();
          lwq.eq(WmNewsMaterial::getNewsId,wmNews.getId());
          wmNewsMaterialMapper.delete(lwq);
          updateById(wmNews);
        }

    }

}
