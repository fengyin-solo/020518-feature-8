package com.redtourism.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 智能客服"先追问再回答"链路的一次回复结果。
 * type：
 *   ANSWER   唯一命中，直接给出完整答复
 *   CLARIFY  命中多个相近问题，给出候选项让用户挑选
 *   FALLBACK 答不上来，引导转人工
 */
@Data
public class ChatBotReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    /** 处理经过追踪号（同一轮提问→追问→答复/转人工共用） */
    private String traceNo;
    /** 已落库的机器人消息内容 */
    private String content;

    // ANSWER
    private Long faqId;
    private String question;
    private String answer;

    // CLARIFY
    private List<FaqMatch> candidates;
}
