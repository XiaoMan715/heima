package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
@Transactional
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper,ApArticle> implements ApArticleService {
    // 单页最大加载的数字
    private final static short MAX_PAGE_SIZE = 50;
    //单页加载默认数字
    private final static Integer  DEFAULT_PAGE_SIZE=10;

    @Autowired
    private ApArticleMapper apArticleMapper;
    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    /**
     * 加载文章
     *
     * @param dto
     * @param loadtype 1为加载更多  2为加载最新
     * @return
     */
    @Override
    public ResponseResult load(Short loadtype,ArticleHomeDto dto) {
         //校验分页数目
        Integer size = dto.getSize();
        //如果没有就设置为默认数字
        if (size == null || size == 0) {
            size = DEFAULT_PAGE_SIZE;
        }
        //如果有 要比较不能超过最大
          size = Math.min(size, MAX_PAGE_SIZE);
        //校验loadtype类型参数
        if (!loadtype.equals(ArticleConstants.LOADTYPE_LOAD_MORE) &&loadtype.equals(ArticleConstants.LOADTYPE_LOAD_MORE)){
           //默认设置为加载更多
            loadtype=ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //校验频道
        if (!StringUtils.hasText(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        //校验时间
        if (dto.getMaxBehotTime()==null){
            dto.setMaxBehotTime(new Date());
        }
        if (dto.getMinBehotTime()==null){
            dto.setMinBehotTime(new Date());
        }

        List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, loadtype);
        return ResponseResult.okResult(apArticles);
    }

    @Override
    public ResponseResult saveArticle(ArticleDto dto) {

      /*  try {
            System.out.println("2222222222222222222");
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/
        System.out.println("aparewds dsaveArticle");
        //1检查参数 如果id为空为新增 id有值为修改
        if (ObjectUtils.isEmpty(dto)){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE,"缺少文章参数");
        }
        //拷贝传过来的参数
        ApArticle article=new ApArticle();
        BeanUtils.copyProperties(dto,article);
        log.info("article1:{}",article);

        if (dto.getId()==null){
            //新增
            //保存文章
            save(article);
            //将文章保存在配置表里面
            ApArticleConfig articleConfig =new ApArticleConfig(article.getId());
            apArticleConfigMapper.insert(articleConfig);

            //将文章内容保存在内容表里面
            ApArticleContent articleContent =new ApArticleContent();
            articleContent.setArticleId(article.getId());
            articleContent.setContent(dto.getContent());
            apArticleContentMapper.insert(articleContent);



        }else {
            //2.2 存在id   修改  文章  文章内容

            //修改  文章
            updateById(article);
            log.info("article:{}",article);
            //修改文章内容
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, dto.getId()));
          if (ObjectUtils.isEmpty(apArticleContent)){
              return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"未查到对应的文章数据d w ");
          }
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);

        }

        return ResponseResult.okResult(article.getId());
    }


}
