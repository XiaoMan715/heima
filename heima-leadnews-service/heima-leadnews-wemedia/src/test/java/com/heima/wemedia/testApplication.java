package com.heima.wemedia;


import com.fasterxml.jackson.core.type.TypeReference;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.constants.AliYunCheckTextConstants;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.exception.CustomException;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
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



    @Test
    public void testScanImage() throws Exception {
        //byte[] bytes = fileStorageService.downLoadFile("http://192.168.200.130:9000/leadnews/2023/06/03/1493198a9c4d4476b7873392d7a7ea36.jpg");
       // Map map = greenImageScan.imageScan(Arrays.asList(bytes));
      //  System.out.println(map);
        List<Map<String,String>> images =new ArrayList<>();
        Map<String,String> map=new HashMap<>();
        map.put("image","http://192.168.200.130:9000/leadnews/2023/06/03/1493198a9c4d4476b7873392d7a7ea36.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minio%2F20230603%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20230603T070737Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=af2bcbfac7989c7e69c0b530fdb20c09516d05d3f492b8cf4aa124e2c9a16f30");
        map.put("UUID",UUID.randomUUID().toString());
        images.add(map);
        List<String> sceneList = new ArrayList<>();
        sceneList.add("logo");
       //  sceneList.add("porn");
        System.out.println(greenImageScan.checkImage(images, sceneList));
    }

    @Test
    public void ss(){
        String content ="[{\"type\":\"image\",\"value\":\"http://192.168.200.130:9000/leadnews/2023/05/30/91fa77bac7194489a4e5792607e776c7.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minio%2F20230530%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20230530T135729Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=271f75b60fd4d517c28aad3d86949e81f4b77c63bfbd112c7b5afcf54223edfd\"},{\"type\":\"text\",\"value\":\"你iis是阿达\"},{\"type\":\"text\",\"value\":\"按市场第三方撒旦撒\"},{\"type\":\"image\",\"value\":\"http://192.168.200.130:9000/leadnews/2023/05/30/2697a71ec99d4fd8b6b6b9adf95e7aa3.jpg\"},{\"type\":\"text\",\"value\":\"请在这里输入正文\"}]";
      List<String > images;
        try {
          //  List<Map<String Object>> list = objectMapper.readValue(jsonString, new TypeReference<List<Map<String, Object>>>() {});

            List<Map> lists = objectMapper.readValue(content, new TypeReference<List<Map>>() {
            });
            images = lists.stream()
                    .filter(list -> list.get("type")
                            .equals("image")).
                    map(list -> (String)list.get("value"))
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
        List<String> texts=new ArrayList<>();
        List<String> labels=new ArrayList<>();
        texts.add("专业贷款安全快捷方便无抵押随机随贷当天放款上门服务联系微信123456 微信号：9727979   AV电影就来www.AV.com  胸大才是流量密码");
       texts.add("胸大才是流量密码");

        labels.add(AliYunCheckTextConstants.LABLE_TERRORISM);
        labels.add(AliYunCheckTextConstants.LABLE_AD);

       // labels.add("ad");

       try {
           greenTextScan.checkTest(texts,labels).stream().forEach(System.out::println);
       }catch (CustomException e){
           log.info("message:{}",e.getAppHttpCodeEnum());
       }
     //   greenTextScan.checkMessage();
    }

}
