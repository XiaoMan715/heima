package com.heima.search.service.impl;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dto.HistorySearchDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.pojos.ApUserSearch;
import com.heima.search.service.ApUserSearchService;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ApUserSearchServiceImpl implements ApUserSearchService {

    private static final String MONGODB_LISTNAME = "ap_user_search";
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    @Async
    public void insert(String keyword, Integer userId) {
        if (StringUtils.hasText(keyword) || userId != null) {
            //查询用户当前搜索关键词
            Query query = Query.query(Criteria.where("keyword").is(keyword).and("userId").is(userId));
            ApUserSearch apUserSearch = mongoTemplate.findOne(query, ApUserSearch.class);
            if (ObjectUtils.isEmpty(apUserSearch)) {
                //不存在就替换时间最久的一条
                //先要判断是否超过十条 如果超过十条就更新时间最久那条 没超过就添加
                long count = mongoTemplate.count(Query.query(Criteria.where("userId").is(userId)), MONGODB_LISTNAME);
                log.info("我看看有多少条数据:{}", count);
                apUserSearch = new ApUserSearch();
                apUserSearch.setUserId(userId);
                apUserSearch.setKeyword(keyword);
                apUserSearch.setCreatedTime(new Date());
                if (count > 0 && count < 10) {
                    //小于十条我们直接插入
                    mongoTemplate.save(apUserSearch);
                } else {
                    //大于十条我们更新最久的那条数据
                    Query query1 = Query.query(Criteria.where("userId").is(userId))
                            .with(Sort.by(Sort.Direction.ASC, "createdTime"));
                    ApUserSearch LastSearch = mongoTemplate.findOne(query1, ApUserSearch.class);
                    LastSearch.setKeyword(keyword);
                    LastSearch.setCreatedTime(new Date());
                    mongoTemplate.save(LastSearch);
                }
                return;
            }
            //存在就更新时间 使他排名提前
            apUserSearch.setCreatedTime(new Date());
            mongoTemplate.save(apUserSearch);

        }


    }

    //查询当前用户的搜索记录倒叙排列
    @Override
    public ResponseResult findUserSearch() {
        //获取当前用户id
        ApUser user = AppThreadLocalUtil.getUser();
        if (ObjectUtils.isEmpty(user) || user.getId() == null) {
            ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        Query query = Query.query(Criteria.where("userId").is(user.getId()))
                .with(Sort.by(Sort.Direction.DESC, "createdTime"));
        List<ApUserSearch> apUserSearches = mongoTemplate.find(query, ApUserSearch.class);
        return ResponseResult.okResult(apUserSearches);
    }

    @Override
    public ResponseResult delUserSearch(HistorySearchDto historySearchDto) {
        ApUser user = AppThreadLocalUtil.getUser();
        if (ObjectUtils.isEmpty(user) || user.getId() == null || ObjectUtils.isEmpty(historySearchDto)) {
            ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        Query query = Query.query(Criteria.where("id").is(historySearchDto.getId()).and("userID").is(user.getId()));
        long deletedCount = mongoTemplate.remove(query, ApUserSearch.class).getDeletedCount();
        if (deletedCount > 0) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
    }
}
