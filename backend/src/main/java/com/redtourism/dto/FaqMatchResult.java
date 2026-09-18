package com.redtourism.dto;

import com.redtourism.entity.Faq;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 智能客服匹配结果：
 * ANSWER     —— 唯一命中，直接给出完整答复
 * CANDIDATES —— 命中多个相近问题，先追问让用户挑选
 * NONE       —— 未命中，建议转人工
 */
@Data
public class FaqMatchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_ANSWER = "ANSWER";
    public static final String TYPE_CANDIDATES = "CANDIDATES";
    public static final String TYPE_NONE = "NONE";

    private String type;
    /** type=ANSWER 时的完整答复 */
    private String answer;
    /** type=ANSWER 时命中的 FAQ */
    private Faq faq;
    /** type=CANDIDATES 时的候选问题列表 */
    private List<Faq> candidates;
}
