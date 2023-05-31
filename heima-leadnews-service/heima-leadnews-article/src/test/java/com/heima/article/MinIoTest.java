package com.heima.article;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.file.service.FileStorageService;
import com.heima.file.service.impl.MinIOFileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.wemedia.pojos.WmMaterial;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Wrapper;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = ArticleApplication.class)
@RunWith(SpringRunner.class)
@Slf4j
public class MinIoTest {
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;
    @Autowired
    private Configuration configuration;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ApArticleMapper apArticleMapper;
    @Autowired
    private MinioClient minioClient;
    @Autowired


    @Test
    public void test() throws IOException, ServerException, InvalidBucketNameException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        FileInputStream fileInputStream = new FileInputStream("D:\\黑马头条\\模板文件\\plugins\\js\\axios.min.js");
        MinioClient client = MinioClient.builder().credentials("minio", "minio123")
                .endpoint("http://192.168.200.130:9000").build();

        PutObjectArgs putObjectArgs = PutObjectArgs.builder().
                object("plugins/js/axios.min.js").contentType("text/js").bucket("leadnews")
                .stream(fileInputStream, fileInputStream.available(), -1).build();
        client.putObject(putObjectArgs);

    }

    @Test
    public void test2() throws IOException, TemplateException {
        //查询出文章对应的内容详情
        LambdaQueryWrapper<ApArticleContent> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ApArticleContent::getArticleId, 1302862387124125698L);
        ApArticleContent apArticleContent = apArticleContentMapper.selectOne(lambdaQueryWrapper);
        if (ObjectUtils.isNotEmpty(apArticleContent) && StringUtils.hasText(apArticleContent.getContent())) {
            //如果有文章生成模板hyml
            StringWriter out = new StringWriter();
            Template template = configuration.getTemplate("article.ftl");
            Map<String, Object> params = new HashMap<>();
            params.put("content", JSONArray.parseArray(apArticleContent.getContent()));

            template.process(params, out);
            InputStream is = new ByteArrayInputStream(out.toString().getBytes());

            //3.把html文件上传到minio中
            String path = fileStorageService.uploadHtmlFile("", apArticleContent.getArticleId() + ".html", is);
            ApArticle article = new ApArticle();
            article.setId(apArticleContent.getArticleId());
            article.setStaticUrl(path);
            apArticleMapper.updateById(article);
        }

    }

    @Test
    public void  dd() throws ServerException, InvalidBucketNameException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, InvalidExpiresRangeException {
       // GetPresignedObjectUrlArgs args =new GetPresignedObjectUrlArgs();
     //   final String leadnews = minioClient.getObjectUrl("leadnews", "cf488e3199ab4cce91823e7856f128a8.jpg");
       /*  String leadnews = minioClient.getObjectUrl("leadnews", "2023/05/30/49df2619f9a1468a8c1c4f707da9fdf8.jpg");
        System.out.println(leadnews);*/
        // String presignedObjectUrl = minioClient.getPresignedObjectUrl();
        //minioClient.getObjectUrl("leadnews","cf488e3199ab4cce91823e7856f128a8.jpg");
        // String leadnews = minioClient.presignedGetObject("leadnews", "2023/05/30/49df2619f9a1468a8c1c4f707da9fdf8.jpg");
/*String url ="http://192.168.200.130:9000/leadnews/2023/05/30/1a8b60d4918948289e6f104ed356a853.jpg";
        System.out.println(url.indexOf("leadnews"));
         String substring = url.substring(28 + "leadnews".length() + 1);
        System.out.println(minioClient.presignedGetObject("leadnews", substring));*/
        // System.out.println(leadnews);
//

//         String leadnews = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket("leadnews").object("2023/05/30/727c7aef4ffa439ca2de5ce17188d70a.jpg").method(Method.GET).build());
//
//        System.out.println(leadnews);


    }

}
