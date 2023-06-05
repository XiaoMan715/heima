package com.heima.common.aliyun;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.imageaudit20191230.Client;
import com.aliyun.imageaudit20191230.models.ScanImageAdvanceRequest;
import com.aliyun.imageaudit20191230.models.ScanImageRequest;
import com.aliyun.imageaudit20191230.models.ScanImageResponse;
import com.aliyun.imageaudit20191230.models.ScanImageResponseBody;
import com.aliyun.tea.TeaModel;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.green.model.v20180509.ImageSyncScanRequest;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.heima.common.aliyun.util.ClientUploader;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aliyun")
public class GreenImageScan {

    private String accessKeyId;
    private String secret;

    private String endpoint;

/*
    public Map imageScan(List<byte[]> imageList) throws Exception {
        IClientProfile profile = DefaultProfile
            .getProfile("cn-shanghai", accessKeyId, secret);
        DefaultProfile
            .addEndpoint("cn-shanghai", "cn-shanghai", "Green", "imageaudit.cn-shanghai.aliyuncs.com");
        IAcsClient client = new DefaultAcsClient(profile);
        ImageSyncScanRequest imageSyncScanRequest = new ImageSyncScanRequest();
        // 指定api返回格式
        imageSyncScanRequest.setAcceptFormat(FormatType.JSON);
        // 指定请求方法
        imageSyncScanRequest.setMethod(MethodType.POST);
        imageSyncScanRequest.setEncoding("utf-8");
        //支持http和https
        imageSyncScanRequest.setProtocol(ProtocolType.HTTP);
        JSONObject httpBody = new JSONObject();
        */
/**
 * 设置要检测的场景, 计费是按照该处传递的场景进行
 * 一次请求中可以同时检测多张图片，每张图片可以同时检测多个风险场景，计费按照场景计算
 * 例如：检测2张图片，场景传递porn、terrorism，计费会按照2张图片鉴黄，2张图片暴恐检测计算
 * porn: porn表示色情场景检测
 *//*


        httpBody.put("scenes", Arrays.asList(scenes.split(",")));

        */
/**
 * 如果您要检测的文件存于本地服务器上，可以通过下述代码片生成url
 * 再将返回的url作为图片地址传递到服务端进行检测
 *//*

     */
/**
 * 设置待检测图片， 一张图片一个task
 * 多张图片同时检测时，处理的时间由最后一个处理完的图片决定
 * 通常情况下批量检测的平均rt比单张检测的要长, 一次批量提交的图片数越多，rt被拉长的概率越高
 * 这里以单张图片检测作为示例, 如果是批量图片检测，请自行构建多个task
 *//*

        ClientUploader clientUploader = ClientUploader.getImageClientUploader(profile, false);
        String url = null;
        List<JSONObject> urlList = new ArrayList<JSONObject>();
        for (byte[] bytes : imageList) {
            url = clientUploader.uploadBytes(bytes);
            JSONObject task = new JSONObject();
            task.put("dataId", UUID.randomUUID().toString());
            //设置图片链接为上传后的url
            task.put("url", url);
            task.put("time", new Date());
            urlList.add(task);
        }
        httpBody.put("tasks", urlList);
        imageSyncScanRequest.setHttpContent(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(httpBody.toJSONString()),
            "UTF-8", FormatType.JSON);
        */
/**
 * 请设置超时时间, 服务端全链路处理超时时间为10秒，请做相应设置
 * 如果您设置的ReadTimeout小于服务端处理的时间，程序中会获得一个read timeout异常
 *//*

        imageSyncScanRequest.setConnectTimeout(3000);
        imageSyncScanRequest.setReadTimeout(10000);
        HttpResponse httpResponse = null;
        try {
            httpResponse = client.doAction(imageSyncScanRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, String> resultMap = new HashMap<>();

        //服务端接收到请求，并完成处理返回的结果
        if (httpResponse != null && httpResponse.isSuccess()) {
            JSONObject scrResponse = JSON.parseObject(org.apache.commons.codec.binary.StringUtils.newStringUtf8(httpResponse.getHttpContent()));
            System.out.println(JSON.toJSONString(scrResponse, true));
            int requestCode = scrResponse.getIntValue("code");
            //每一张图片的检测结果
            JSONArray taskResults = scrResponse.getJSONArray("data");
            if (200 == requestCode) {
                for (Object taskResult : taskResults) {
                    //单张图片的处理结果
                    int taskCode = ((JSONObject) taskResult).getIntValue("code");
                    //图片要检测的场景的处理结果, 如果是多个场景，则会有每个场景的结果
                    JSONArray sceneResults = ((JSONObject) taskResult).getJSONArray("results");
                    if (200 == taskCode) {
                        for (Object sceneResult : sceneResults) {
                            String scene = ((JSONObject) sceneResult).getString("scene");
                            String label = ((JSONObject) sceneResult).getString("label");
                            String suggestion = ((JSONObject) sceneResult).getString("suggestion");
                            //根据scene和suggetion做相关处理
                            //do something
                            System.out.println("scene = [" + scene + "]");
                            System.out.println("suggestion = [" + suggestion + "]");
                            System.out.println("suggestion = [" + label + "]");
                            if (!suggestion.equals("pass")) {
                                resultMap.put("suggestion", suggestion);
                                resultMap.put("label", label);
                                return resultMap;
                            }
                        }

                    } else {
                        //单张图片处理失败, 原因视具体的情况详细分析
                        System.out.println("task process fail. task response:" + JSON.toJSONString(taskResult));
                        return null;
                    }
                }
                resultMap.put("suggestion","pass");
                return resultMap;
            } else {
                */

    /**
     * 表明请求整体处理失败，原因视具体的情况详细分析
     *//*

                System.out.println("the whole image scan request failed. response:" + JSON.toJSONString(scrResponse));
                return null;
            }
        }
        return null;
    }
*/
    public List<Map<String,Object>> checkImage(List<Map<String, String>> images, List<String> sceneList) throws Exception {
        //配置accesskey信息
        Config config = new Config();
        config.setAccessKeyId(accessKeyId);
        config.setAccessKeySecret(secret);
        config.setEndpoint(endpoint);
        Client client = new Client(config);
        ScanImageAdvanceRequest.ScanImageAdvanceRequestTask task0 = new ScanImageAdvanceRequest.ScanImageAdvanceRequestTask();
        if (CollectionUtils.isEmpty(images) || images.size() == 0) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        List<ScanImageAdvanceRequest.ScanImageAdvanceRequestTask> taskList = new ArrayList<>();
        for (Map<String, String> image : images) {
            InputStream inputStream1 = new URL(image.get("image")).openConnection().getInputStream();
            task0.setImageURLObject(inputStream1);
            task0.setDataId(image.get("UUID"));
            task0.setImageTimeMillisecond(1L);
            task0.setInterval(1);
            task0.setMaxFrames(1);
            taskList.add(task0);
        }
        ScanImageAdvanceRequest scanImageAdvanceRequest = new ScanImageAdvanceRequest()
                .setTask(taskList)
                .setScene(sceneList);
        RuntimeOptions runtime = new RuntimeOptions();
        List<Map<String,Object>> checkBack=new ArrayList<>();
        Map<String,Object>map=null;
        try {
            ScanImageResponse sIRe = client.scanImageAdvance(scanImageAdvanceRequest, runtime);
            // 获取整体结果
            // System.out.println(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(scanImageResponse)));
            if (ObjectUtils.isEmpty(sIRe) || ObjectUtils.isEmpty(sIRe.getBody()) || ObjectUtils.isEmpty(sIRe.getBody())) {
                throw new CustomException(AppHttpCodeEnum.ALIYUN_CHECKBACK_OBJECTNUll);
            }
            if (CollectionUtils.isEmpty(sIRe.getBody().getData().getResults())||sIRe.getBody().getData().getResults().size()==0) {
                throw new CustomException(AppHttpCodeEnum.ALIYUN_CHECKBACK_LISTNUll);
            }
             List<ScanImageResponseBody.ScanImageResponseBodyDataResults> results = sIRe.getBody().getData().getResults();
             List<List<ScanImageResponseBody.ScanImageResponseBodyDataResultsSubResults>> collect = results.stream().map(x -> x.getSubResults()).collect(Collectors.toList());
            for (List<ScanImageResponseBody.ScanImageResponseBodyDataResultsSubResults> subResults : collect) {
                for (ScanImageResponseBody.ScanImageResponseBodyDataResultsSubResults result : subResults) {
                    map=new HashMap<>();
                    map.put("Suggestion",result.getSuggestion());
                    map.put("Label",result.getLabel());
                    map.put("Scene",result.getScene());
                    checkBack.add(map);

                }
            }
            return checkBack;
             // 获取单个字段
            // System.out.println(scanImageResponse.getBody().getData());
        } catch (com.aliyun.tea.TeaException teaException) {
            // 获取整体报错信息
            System.out.println(com.aliyun.teautil.Common.toJSONString(teaException));
            // 获取单个字段
            System.out.println(teaException.getCode());
        }
        return null;
    }


}