package com.heima.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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



}
