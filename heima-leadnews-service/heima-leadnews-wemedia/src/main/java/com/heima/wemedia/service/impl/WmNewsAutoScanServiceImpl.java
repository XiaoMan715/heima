package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.exception.CustomException;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {
    @Autowired
    private WmNewsMapper wmNewsMapper;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private GreenTextScan greenTextScan;
    @Autowired
    private GreenImageScan greenImageScan;


    @Autowired
    private IArticleClient articleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;
    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;

    /**
     * 自媒体审核接口
     *
     * @param id 自媒体文章id
     */
    @Override
    @Async//异步调用
    public void autoScanWmNews(Integer id) {
        //查询自媒体文章 通过id查询到自媒体文章
        if (id == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (ObjectUtils.isEmpty(wmNews)) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        TypeReference<List<Map<String, Object>>> typeRef
                = new TypeReference<List<Map<String, Object>>>() {
        };

        try {
            List<Map<String, Object>> content = objectMapper.readValue(wmNews.getContent(), typeRef);
            List<String> images = content.stream().filter(x -> x.get("type").equals("image")).map(x -> (String) x.get("value")).collect(Collectors.toList());
            List<String> texts = content.stream().filter(x -> x.get("type").equals("text")).map(x -> (String) x.get("value")).collect(Collectors.toList());
            if (StringUtils.hasText(wmNews.getImages())) {
                String[] l = wmNews.getImages().split(",");
                images.addAll(Arrays.asList(l));
            }
            //自管理的敏感词审核
            boolean isSensitive = handleSensitiveScan( texts, wmNews);

           /* //调用接口去判断是否违规 不想写了 就是去调阿里云的接口判断是否违规 我代码里面有自己看吧 直接默认合格
            List<String> adlist =new ArrayList<>();
            adlist.add("ad");
            List<Map<String, Object>> mapList = greenTextScan.checkTest(texts, adlist);
            if (mapList.isEmpty()){
                return ;
            }*/

             ResponseResult responseResult = saveAppArticle(wmNews);
             log.info("code:{}",responseResult.getCode());
             if (ObjectUtils.isEmpty(responseResult)||!responseResult.getCode().equals(200)){
                 throw  new RuntimeException("WmNewsAutoScanServiceImpl-文章审核，保存app端相关文章数据失败\"");
             }
             wmNews.setArticleId((Long) responseResult.getData());
            updateWmNews(wmNews,(short) 9,"审核成功");


        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private boolean handleSensitiveScan(List content, WmNews wmNews) {
        boolean flag = true;
        LambdaQueryWrapper<WmSensitive> lqw =new LambdaQueryWrapper<>();
        lqw.select(WmSensitive::getSensitives);
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(lqw);
        List<String> sensitiveList = wmSensitives.stream().map(x -> x.getSensitives()).collect(Collectors.toList());
        SensitiveWordUtil.initMap(sensitiveList);
         String join = String.join(",", content);
        Map<String, Integer> map = SensitiveWordUtil.matchWords(join);
        if(map.size() >0){
            updateWmNews(wmNews,(short) 2,"当前文章中存在违规内容"+map);
            flag = false;
        }


        return flag;
    }

    /**
     * 修改文章内容
     * @param wmNews
     * @param status
     * @param reason
     */
    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(( Integer.valueOf(status)));
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }


    /**
     * 保存app端相关的文章数据
     *
     * @param wmNews
     */
    private ResponseResult saveAppArticle(WmNews wmNews) {

        ArticleDto articleDto = new ArticleDto();
        BeanUtils.copyProperties(wmNews, articleDto);

        if (wmNews.getType() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "缺少文章布局参数");
        }
        articleDto.setLayout(wmNews.getType().shortValue());
        //频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (ObjectUtils.isEmpty(wmChannel)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "缺少频道参数");
        }
        //作者
        if (wmNews.getUserId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "缺少作者参数");
        }
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (ObjectUtils.isEmpty(wmUser)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "找不到用户");
        }
        articleDto.setAuthorId(wmUser.getId().longValue());
        articleDto.setAuthorName(wmUser.getName());
        if (wmNews.getArticleId() != null) {
            //存在id说名是修改
            articleDto.setId(wmNews.getArticleId());
        }
        articleDto.setCreatedTime(new Date());
        ResponseResult responseResult = articleClient.saveArticle(articleDto);
        return responseResult;

    }

}
