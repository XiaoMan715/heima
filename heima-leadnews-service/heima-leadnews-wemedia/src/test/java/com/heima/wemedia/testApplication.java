package com.heima.wemedia;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.constants.AliYunCheckTextConstants;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.exception.CustomException;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
@Slf4j
public class testApplication {
    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private GreenImageScan greenImageScan;
    @Autowired
    private GreenTextScan greenTextScan;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;
    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;


    @Test
    public void testScanImage() throws Exception {
        //byte[] bytes = fileStorageService.downLoadFile("http://192.168.200.130:9000/leadnews/2023/06/03/1493198a9c4d4476b7873392d7a7ea36.jpg");
        // Map map = greenImageScan.imageScan(Arrays.asList(bytes));
        //  System.out.println(map);
        List<Map<String, String>> images = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("image", "http://192.168.200.130:9000/leadnews/2023/06/03/1493198a9c4d4476b7873392d7a7ea36.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minio%2F20230603%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20230603T070737Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=af2bcbfac7989c7e69c0b530fdb20c09516d05d3f492b8cf4aa124e2c9a16f30");
        map.put("UUID", UUID.randomUUID().toString());
        images.add(map);
        List<String> sceneList = new ArrayList<>();
        sceneList.add("logo");
        //  sceneList.add("porn");
        System.out.println(greenImageScan.checkImage(images, sceneList));
    }

    @Test
    public void ss() {
        String content = "[{\"type\":\"image\",\"value\":\"http://192.168.200.130:9000/leadnews/2023/05/30/91fa77bac7194489a4e5792607e776c7.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minio%2F20230530%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20230530T135729Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=271f75b60fd4d517c28aad3d86949e81f4b77c63bfbd112c7b5afcf54223edfd\"},{\"type\":\"text\",\"value\":\"你iis是阿达\"},{\"type\":\"text\",\"value\":\"按市场第三方撒旦撒\"},{\"type\":\"image\",\"value\":\"http://192.168.200.130:9000/leadnews/2023/05/30/2697a71ec99d4fd8b6b6b9adf95e7aa3.jpg\"},{\"type\":\"text\",\"value\":\"请在这里输入正文\"}]";
        List<String> images;
        try {
            //  List<Map<String Object>> list = objectMapper.readValue(jsonString, new TypeReference<List<Map<String, Object>>>() {});

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

        List<String> collect = images.stream().limit(3).collect(Collectors.toList());
        System.out.println(collect);
    }

    @Test
    public void sda() throws Exception {
        /* Map map = greenTextScan.greeTextScan("我是一个好人");
        System.out.println(map);*/
        List<String> texts = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        texts.add("专业贷款安全快捷方便无抵押随机随贷当天放款上门服务联系微信123456 微信号：9727979   AV电影就来www.AV.com  胸大才是流量密码");
        texts.add("胸大才是流量密码");

        labels.add(AliYunCheckTextConstants.LABLE_TERRORISM);
        labels.add(AliYunCheckTextConstants.LABLE_AD);

        // labels.add("ad");

        try {
            greenTextScan.checkTest(texts, labels).stream().forEach(System.out::println);
        } catch (CustomException e) {
            log.info("message:{}", e.getAppHttpCodeEnum());
        }
        //   greenTextScan.checkMessage();
    }
    @Autowired
    WmNewsMapper wmNewsMapper;



    @Test
    public void s() throws Exception {
        WmNews wmNews = wmNewsMapper.selectById(6248);
        TypeReference<List<Map<String, Object>>> typeRef
                = new TypeReference<List<Map<String, Object>>>() {
        };
       // log.info("ss:{}",wmNews);
        List<Map<String, Object>> content = objectMapper.readValue(wmNews.getContent(), typeRef);

        List<String> images = content.stream().filter(x -> x.get("type").equals("image")).map(x ->(String) x.get("value")).collect(Collectors.toList());
        List<String> texts = content.stream().filter(x -> x.get("type").equals("text")).map(x ->(String) x.get("value")).collect(Collectors.toList());
        if (StringUtils.hasText(wmNews.getImages())) {
            String[] l = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(l));
        }
        images=images.stream().distinct().collect(Collectors.toList());


        List<String> sceneList = new ArrayList<>();
        sceneList.add(AliYunCheckTextConstants.LABLE_AD);
        List<String> adlist = new ArrayList<>();
        adlist.add("pron");
      // greenImageScan.checkImage(images,adlist);
         List<Map<String, Object>> mapList = greenTextScan.checkTest(texts, sceneList);
         mapList.stream().forEach(System.out::println);


    }
    @Autowired
    private IArticleClient client;

    @Test
    public void  sdasdasd(){
        wmNewsAutoScanService.autoScanWmNews(6248);
     /*   ArticleDto articleDto =new ArticleDto();
        articleDto.setId(1666047846295670786L);*/
        //client.saveArticle(articleDto);


    }

    @Test
    public void sdea(){
       /* LambdaQueryWrapper<WmSensitive> lqw =new LambdaQueryWrapper<>();
        lqw.select(WmSensitive::getSensitives);
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(lqw);
        List<String> sensitiveList = wmSensitives.stream().map(x -> x.getSensitives()).collect(Collectors.toList());
        SensitiveWordUtil.initMap(sensitiveList);
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);*/
        WmNews wmNews = wmNewsMapper.selectById(6259);
        log.info("wmNews:{}",wmNews);
    }

}
