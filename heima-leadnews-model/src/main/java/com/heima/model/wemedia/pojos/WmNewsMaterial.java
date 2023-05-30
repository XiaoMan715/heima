package com.heima.model.wemedia.pojos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 自媒体图文引用素材信息表
 * @TableName wm_news_material
 */
@TableName(value ="wm_news_material")
@Data
public class WmNewsMaterial implements Serializable {
    /**
     * 主键
     */
    @TableId
    private Integer id;

    /**
     * 素材ID
     */
    private Integer materialId;

    /**
     * 图文ID
     */
    private Integer newsId;

    /**
     * 引用类型
            0 内容引用
            1 主图引用
     */
    private Integer type;

    /**
     * 引用排序
     */
    private Integer ord;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        WmNewsMaterial other = (WmNewsMaterial) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getMaterialId() == null ? other.getMaterialId() == null : this.getMaterialId().equals(other.getMaterialId()))
            && (this.getNewsId() == null ? other.getNewsId() == null : this.getNewsId().equals(other.getNewsId()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getOrd() == null ? other.getOrd() == null : this.getOrd().equals(other.getOrd()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getMaterialId() == null) ? 0 : getMaterialId().hashCode());
        result = prime * result + ((getNewsId() == null) ? 0 : getNewsId().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getOrd() == null) ? 0 : getOrd().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", materialId=").append(materialId);
        sb.append(", newsId=").append(newsId);
        sb.append(", type=").append(type);
        sb.append(", ord=").append(ord);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}