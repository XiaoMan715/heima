package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;


@Slf4j
@Service
@Transactional
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {
    @Autowired
    private WmMaterialMapper wmMaterialMapper;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private MinioClient minioClient;

    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {

        //1.检查参数
        if (multipartFile == null || multipartFile.getSize() == 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //从当前线程获取用户id
        WmUser user = WmThreadLocalUtil.getUser();
        if (ObjectUtils.isEmpty(user) || user.getId() == null) {
            log.info("sssssw ");
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "用户id不存在");
        }
        //2上传到minio
        //2.上传图片到minIO中
        String fileName = UUID.randomUUID().toString().replace("-", "");
        //aa.jpg
        String originalFilename = multipartFile.getOriginalFilename();
        String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileId = null;
        try {
            fileId = fileStorageService.uploadImgFile("", fileName + postfix, multipartFile.getInputStream());
            log.info("上传图片到MinIO中，fileId:{}", fileId);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("WmMaterialServiceImpl-上传文件失败");
        }
        String imgUrl = fileStorageService.getImgUrl(fileId);

        //3保存链接到数据库

        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(user.getId());
        wmMaterial.setUrl(imgUrl);
        wmMaterial.setCreatedTime(new Date());
        wmMaterial.setIsCollection((Integer) 0);
        wmMaterial.setType((Integer) 0);
        log.info("wmMaterial:{}", wmMaterial);
        boolean save = save(wmMaterial);
        log.info("上传是否成功：{}", save);
        //4返回结果

        return ResponseResult.okResult(wmMaterial);
    }

    /**
     * 查询素材列表
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmMaterialDto dto) {
        if (ObjectUtils.isEmpty(dto)||dto.getIsCollection()==null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //检查分页是否有值 没有给默认值
        dto.checkParam();
        //分页查询
        IPage page =new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmMaterial> lambdaQueryWrapper =new LambdaQueryWrapper<>();
        //是否是收藏列表
        if (dto.getIsCollection()==1){
            lambdaQueryWrapper.eq(WmMaterial::getIsCollection,dto.getIsCollection());
        }
        //获取当前用户的素材
         Integer userId = WmThreadLocalUtil.getUser().getId();
        if (userId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE,"缺少用户id");
        }
        lambdaQueryWrapper.eq(WmMaterial::getUserId,userId);
        //排序
        lambdaQueryWrapper.orderByDesc(WmMaterial::getCreatedTime);
          page =page(page,lambdaQueryWrapper);

        //3.结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }
}
