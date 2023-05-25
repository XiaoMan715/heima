package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import org.apache.commons.net.nntp.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApArticleMapper extends BaseMapper<ApArticle> {
    /**
     *
     * @param dto 前端传来参数
     * @param type 1 加载更多接口 2加载最新接口
     * @return
     */
    public List<ApArticle> loadArticleList(@Param("dto")ArticleHomeDto dto ,@Param("type") Short type);

}
