package com.redtourism.common.dto;

import com.redtourism.entity.Faq;
import lombok.Data;

import java.io.Serializable;

/**
 * 智能客服单条 FAQ 的匹配结果（带相似度分值）。
 */
@Data
public class FaqMatch implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String question;
    private String answer;
    private Integer sortOrder;
    /** 0~1 的匹配度，分值越高越相近 */
    private double score;

    public static FaqMatch of(Faq faq, double score) {
        FaqMatch m = new FaqMatch();
        m.setId(faq.getId());
        m.setQuestion(faq.getQuestion());
        m.setAnswer(faq.getAnswer());
        m.setSortOrder(faq.getSortOrder());
        m.setScore(score);
        return m;
    }
}
