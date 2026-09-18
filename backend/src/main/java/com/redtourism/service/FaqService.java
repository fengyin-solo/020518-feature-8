package com.redtourism.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.redtourism.common.dto.FaqMatch;
import com.redtourism.entity.Faq;
import java.util.List;

public interface FaqService extends IService<Faq> {
    List<Faq> listAll();

    /**
     * 旧版单句自动回复（保留兼容）。
     */
    String autoReply(String question);

    /**
     * 按与问题的相近度对 FAQ 打分并倒序排列。
     */
    List<FaqMatch> match(String question);

    /**
     * 命中度判定阈值：达到即视为"相近问题"。
     * 取 0.25：单个有意义的二元组命中（如"门票"在3字FAQ问题中占1/3）即可入围候选；
     * 唯一命中直接答复，多个命中列出候选让用户确认，避免误答。
     */
    double MATCH_THRESHOLD = 0.25d;
}
