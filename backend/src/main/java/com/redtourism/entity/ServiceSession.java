package com.redtourism.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("service_session")
public class ServiceSession implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String stage;
    private String pendingFaqIds;
    private Long resolvedFaqId;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String username;
}
