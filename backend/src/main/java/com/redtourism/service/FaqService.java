package com.redtourism.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.redtourism.dto.FaqMatchResult;
import com.redtourism.entity.Faq;
import java.util.List;

public interface FaqService extends IService<Faq> {
    List<Faq> listAll();

    /**
     * 对用户提问进行 FAQ 匹配。
     * 唯一命中直接返回答复；命中多个相近问题返回候选列表（先追问再回答）；无命中返回 NONE。
     */
    FaqMatchResult match(String question);
}
