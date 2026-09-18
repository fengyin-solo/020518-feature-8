package com.redtourism.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("service_chat")
public class ServiceChat implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** USER 用户 / ADMIN 人工客服 / BOT 智能客服 / SYSTEM 系统 */
    private String sender;
    /** CHAT 普通消息 / CLARIFY 候选追问 / ANSWER 完整答复 / FALLBACK 未命中 / HANDOFF 转人工通知 */
    private String msgType;
    private String content;
    /** CLARIFY 消息的候选 FAQ id 列表，逗号分隔 */
    private String refIds;
    /** 处理经过追踪号：同一轮提问链路共用一个编号 */
    private String traceNo;
    /** 消息产生时链路所处阶段：ASK/CLARIFY/ANSWER/WAITING_HUMAN/IN_HUMAN */
    private String stage;
    private Date createTime;

    @TableField(exist = false)
    private String username;
}
