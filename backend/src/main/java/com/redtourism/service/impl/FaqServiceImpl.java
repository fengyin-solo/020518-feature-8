package com.redtourism.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.redtourism.common.dto.FaqMatch;
import com.redtourism.entity.Faq;
import com.redtourism.mapper.FaqMapper;
import com.redtourism.service.FaqService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class FaqServiceImpl extends ServiceImpl<FaqMapper, Faq> implements FaqService {

    @Override
    public List<Faq> listAll() {
        LambdaQueryWrapper<Faq> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Faq::getSortOrder);
        return list(wrapper);
    }

    @Override
    public String autoReply(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "请输入您的问题";
        }
        List<FaqMatch> matches = match(question);
        if (matches.isEmpty()) {
            return "抱歉，暂时无法回答您的问题。建议您联系人工客服获取帮助，或拨打服务热线：0851-12345。";
        }
        return matches.get(0).getAnswer();
    }

    @Override
    public List<FaqMatch> match(String question) {
        if (question == null || question.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String q = question.trim().toLowerCase();
        List<Faq> faqs = listAll();

        // 英文/空白分词关键词（中文无空格时不会切分）
        Set<String> queryTokens = new LinkedHashSet<>();
        for (String t : q.split("[\\s,，。？?！!、~～]+")) {
            if (t.length() >= 2) queryTokens.add(t);
        }
        // 中文二元组（如"门票怎么预约" -> 门票/票怎/怎么/么预/预约）
        Set<String> queryBigrams = chineseBigrams(q);

        List<FaqMatch> result = new ArrayList<>();
        for (Faq faq : faqs) {
            double score = similarity(q, faq.getQuestion() == null ? "" : faq.getQuestion().toLowerCase(),
                    queryTokens, queryBigrams);
            if (score >= MATCH_THRESHOLD) {
                result.add(FaqMatch.of(faq, round(score)));
            }
        }
        // 相近度降序，相同分值按 FAQ 排序号升序，保证候选顺序稳定
        result.sort(Comparator.comparingDouble(FaqMatch::getScore).reversed()
                .thenComparingInt(m -> m.getSortOrder() == null ? Integer.MAX_VALUE : m.getSortOrder())
                .thenComparing(FaqMatch::getId));
        return result;
    }

    /**
     * 计算用户问题与某条 FAQ 问题的相近度（0~1）：
     * 1. 整句互相包含（最短长度≥2）直接视为强命中；
     * 2. 中文二元组重合：命中的二元组数 / FAQ 端二元组数（覆盖率）；
     * 3. 英文/分词关键词重合：命中的关键词数 / FAQ 端关键词数。
     */
    private double similarity(String q, String faqQ, Set<String> queryTokens, Set<String> queryBigrams) {
        if (q.isEmpty() || faqQ.isEmpty()) return 0;

        // 1. 整句包含
        String shorter = q.length() <= faqQ.length() ? q : faqQ;
        String longer = q.length() <= faqQ.length() ? faqQ : q;
        if (shorter.length() >= 2 && longer.contains(shorter)) {
            return 1.0d;
        }

        double best = 0;

        // 2. 中文二元组覆盖率
        Set<String> faqBigrams = chineseBigrams(faqQ);
        if (!queryBigrams.isEmpty() && !faqBigrams.isEmpty()) {
            int hit = 0;
            for (String bg : queryBigrams) {
                if (faqBigrams.contains(bg)) hit++;
            }
            if (hit > 0) {
                best = Math.max(best, (double) hit / faqBigrams.size());
            }
        }

        // 3. 分词关键词覆盖率
        if (!queryTokens.isEmpty()) {
            Set<String> faqTokens = new LinkedHashSet<>();
            for (String t : faqQ.split("[\\s,，。？?！!、~～]+")) {
                if (t.length() >= 2) faqTokens.add(t);
            }
            int hit = 0;
            for (String tk : queryTokens) {
                if (faqTokens.contains(tk)) hit++;
            }
            if (hit > 0) {
                best = Math.max(best, (double) hit / Math.max(1, faqTokens.size()));
            }
        }
        return best;
    }

    /**
     * 提取字符串中连续 CJK 字符片段的二元组。
     */
    private Set<String> chineseBigrams(String text) {
        Set<String> set = new HashSet<>();
        if (text == null) return set;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i <= text.length(); i++) {
            char c = i < text.length() ? text.charAt(i) : 0;
            if (isChinese(c)) {
                run.append(c);
            } else if (run.length() > 0) {
                for (int j = 0; j + 1 < run.length(); j++) {
                    set.add(run.substring(j, j + 2));
                }
                run.setLength(0);
            }
        }
        return set;
    }

    private boolean isChinese(char c) {
        // CJK 扩展A区 U+3400~U+4DBF；CJK 统一表意文字 U+4E00~U+9FFF
        return (c >= '㐀' && c <= '䶿') || (c >= '一' && c <= '鿿');
    }

    private double round(double v) {
        return Math.round(v * 100) / 100.0d;
    }
}
