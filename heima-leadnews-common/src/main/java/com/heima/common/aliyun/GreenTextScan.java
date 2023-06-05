package com.heima.common.aliyun;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.imageaudit20191230.Client;
import com.aliyun.imageaudit20191230.models.ScanTextRequest;
import com.aliyun.imageaudit20191230.models.ScanTextResponse;
import com.aliyun.imageaudit20191230.models.ScanTextResponseBody;
import com.aliyun.tea.TeaModel;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.green.model.v20180509.TextScanRequest;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.heima.common.exception.CustomException;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.protocol.types.Field;
import org.checkerframework.checker.units.qual.C;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aliyun")
public class GreenTextScan {

    private String accessKeyId;
    private String secret;
    private String endpoint;

    public Map greeTextScan(String content) throws Exception {
        System.out.println(accessKeyId);
        IClientProfile profile = DefaultProfile
                .getProfile("cn-shanghai", accessKeyId, secret);
        DefaultProfile.addEndpoint("cn-shanghai", "cn-shanghai", "Green", "green.cn-shanghai.aliyuncs.com");

        IAcsClient client = new DefaultAcsClient(profile);
        TextScanRequest textScanRequest = new TextScanRequest();
        textScanRequest.setAcceptFormat(FormatType.JSON); // 指定api返回格式
        textScanRequest.setHttpContentType(FormatType.JSON);
        textScanRequest.setMethod(com.aliyuncs.http.MethodType.POST); // 指定请求方法
        textScanRequest.setEncoding("UTF-8");
        textScanRequest.setRegionId("cn-shanghai");
        List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
        Map<String, Object> task1 = new LinkedHashMap<String, Object>();
        task1.put("dataId", UUID.randomUUID().toString());
        /**
         * 待检测的文本，长度不超过10000个字符
         */
        task1.put("content", content);
        tasks.add(task1);
        JSONObject data = new JSONObject();

        /**
         * 检测场景，文本垃圾检测传递：antispam
         **/
        data.put("scenes", Arrays.asList("antispam"));
        data.put("tasks", tasks);
        System.out.println(JSON.toJSONString(data, true));
        textScanRequest.setHttpContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);
        // 请务必设置超时时间
        textScanRequest.setConnectTimeout(3000);
        textScanRequest.setReadTimeout(6000);

        Map<String, String> resultMap = new HashMap<>();
        try {
            HttpResponse httpResponse = client.doAction(textScanRequest);
            if (httpResponse.isSuccess()) {
                JSONObject scrResponse = JSON.parseObject(new String(httpResponse.getHttpContent(), "UTF-8"));
                System.out.println(JSON.toJSONString(scrResponse, true));
                if (200 == scrResponse.getInteger("code")) {
                    JSONArray taskResults = scrResponse.getJSONArray("data");
                    for (Object taskResult : taskResults) {
                        if (200 == ((JSONObject) taskResult).getInteger("code")) {
                            JSONArray sceneResults = ((JSONObject) taskResult).getJSONArray("results");
                            for (Object sceneResult : sceneResults) {
                                String scene = ((JSONObject) sceneResult).getString("scene");
                                String label = ((JSONObject) sceneResult).getString("label");
                                String suggestion = ((JSONObject) sceneResult).getString("suggestion");
                                System.out.println("suggestion = [" + label + "]");
                                if (!suggestion.equals("pass")) {
                                    resultMap.put("suggestion", suggestion);
                                    resultMap.put("label", label);
                                    return resultMap;
                                }

                            }
                        } else {
                            return null;
                        }
                    }
                    resultMap.put("suggestion", "pass");
                    return resultMap;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map checkMessage() throws Exception {
        //配置accesskey信息
        Config config = new Config();
        config.setAccessKeyId(accessKeyId);
        config.setAccessKeySecret(secret);
        config.setEndpoint(endpoint);
        Client client = new Client(config);
        ScanTextRequest.ScanTextRequestTasks tasks = new ScanTextRequest.ScanTextRequestTasks();
        tasks.setContent("rrggfyguguguguguyg");
        //ScanTextRequest.ScanTextRequestLabels labels =new ScanTextRequest.ScanTextRequestLabels().setLabel("ad");
        ScanTextRequest.ScanTextRequestLabels labels = new ScanTextRequest.ScanTextRequestLabels()
                .setLabel("ad");
        ScanTextRequest scanTextRequest = new ScanTextRequest();
        scanTextRequest.setLabels(Arrays.asList(labels));
        scanTextRequest.setTasks(Arrays.asList(tasks));
        RuntimeOptions runtimeOptions = new RuntimeOptions();
        ScanTextResponse scanTextResponse = client.scanTextWithOptions(scanTextRequest, runtimeOptions);
        List<ScanTextResponseBody.ScanTextResponseBodyDataElements> elements = scanTextResponse.getBody().getData().getElements();
       /* for (ScanTextResponseBody.ScanTextResponseBodyDataElements element : elements) {
            for (ScanTextResponseBody.ScanTextResponseBodyDataElementsResults result : element.getResults()) {
                result.
            }
        }*/

        //  System.out.println(scanTextResponse.getBody().);
        System.out.println(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(scanTextResponse)));
        return null;
    }

    /**
     * @param textList 需要检测的文本 可以是多个
     * @param adList   需要检测的文本的分类
     * @throws Exception
     */
    public List<Map<String, Object>> checkTest(List<String> textList, List<String> adList) throws Exception {
        //配置accesskey信息
        Config config = new Config();
        config.setAccessKeyId(accessKeyId);
        config.setAccessKeySecret(secret);
        config.setEndpoint(endpoint);
        Client client = new Client(config);
        List<ScanTextRequest.ScanTextRequestTasks> tasksList = new ArrayList<>();
        List<ScanTextRequest.ScanTextRequestLabels> labelsList = new ArrayList<>();
        ScanTextRequest scanTextRequest = new ScanTextRequest();
        for (String text : textList) {
            tasksList.add(new ScanTextRequest.ScanTextRequestTasks().setContent(text));
        }
        for (String label : adList) {
            labelsList.add(new ScanTextRequest.ScanTextRequestLabels().setLabel(label));
        }
        scanTextRequest.setTasks(tasksList).setLabels(labelsList);
        RuntimeOptions runtimeOptions = new RuntimeOptions();
        ScanTextResponse scanTextResponse = client.scanTextWithOptions(scanTextRequest, runtimeOptions);
        System.out.println(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(scanTextResponse)));
        if (ObjectUtils.isEmpty(scanTextResponse) || ObjectUtils.isEmpty(scanTextResponse.getBody()) || ObjectUtils.isEmpty(scanTextResponse.getBody().getData())) {
            throw new CustomException(AppHttpCodeEnum.ALIYUN_CHECKBACK_LISTNUll);
        }
        List<ScanTextResponseBody.ScanTextResponseBodyDataElements> elements = scanTextResponse.getBody().getData().getElements();
        if (CollectionUtils.isEmpty(elements)) {
            throw new CustomException(AppHttpCodeEnum.ALIYUN_CHECKBACK_LISTNUll);
        }
        List<List<ScanTextResponseBody.ScanTextResponseBodyDataElementsResults>> results = elements.stream().map(x -> x.getResults()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(results)) {
            throw new CustomException(AppHttpCodeEnum.ALIYUN_CHECKBACK_LISTNUll);
        }
        List<Map<String, Object>> backResult = new ArrayList<>();
        Map<String, Object> map;
        List<String> contextlist = null;
        for (List<ScanTextResponseBody.ScanTextResponseBodyDataElementsResults> result : results) {
            if (CollectionUtils.isEmpty(result)) {
                throw new CustomException(AppHttpCodeEnum.ALIYUN_CHECKBACK_LISTNUll);
            }
            for (ScanTextResponseBody.ScanTextResponseBodyDataElementsResults r : result) {
                if (ObjectUtils.isEmpty(r)) {
                    throw new CustomException(AppHttpCodeEnum.ALIYUN_CHECKBACK_LISTNUll);
                }
                map = new HashMap<>();
                map.put("Suggestion", r.getSuggestion());
                map.put("Label", r.getLabel());
                if (!CollectionUtils.isEmpty(r.getDetails())) {
                    for (ScanTextResponseBody.ScanTextResponseBodyDataElementsResultsDetails detail : r.getDetails()) {
                        map.put("detail_label", detail.label);
                        contextlist = new ArrayList<>();
                        for (ScanTextResponseBody.ScanTextResponseBodyDataElementsResultsDetailsContexts context : detail.getContexts()) {
                            contextlist.add(context.getContext());
                        }
                        map.put("detail_context", contextlist);

                    }

                }

                backResult.add(map);
            }
        }
        System.out.println(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(scanTextResponse)));
        return backResult;
    }

}