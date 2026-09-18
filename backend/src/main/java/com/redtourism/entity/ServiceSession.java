package com.redtourism.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 客服会话状态：每个用户一条，记录"提问→追问→答复→转人工"链路走到哪一步。
 */
@Data
@TableName("service_session")
public class ServiceSession implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** ASK 智能客服提问中 / CLARIFY 待用户选择候选 / ANSWER 机器人已答复 / WAITING_HUMAN 等待人工接入 / IN_HUMAN 人工服务中 */
    private String stage;
    /** CLARIFY 阶段待选 FAQ id 列表，逗号分隔 */
    private String pendingFaqIds;
    /** 当前进行中的处理经过追踪号 */
    private String activeTraceNo;
    private Date updateTime;
    private Date createTime;
}
