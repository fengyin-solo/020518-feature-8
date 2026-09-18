package com.redtourism.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.redtourism.dto.FaqMatchResult;
import com.redtourism.entity.Faq;
import com.redtourism.mapper.FaqMapper;
import com.redtourism.service.FaqService;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FaqServiceImpl extends ServiceImpl<FaqMapper, Faq> implements FaqService {

    /** 候选问题最多返回条数 */
    private static final int MAX_CANDIDATES = 5;
    /** 模糊相似度阈值（字符二元组 Dice 系数） */
    private static final double SIMILARITY_THRESHOLD = 0.25;

    @Override
    public List<Faq> listAll() {
        LambdaQueryWrapper<Faq> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Faq::getSortOrder);
        return list(wrapper);
    }

    @Override
    public FaqMatchResult match(String question) {
        FaqMatchResult result = new FaqMatchResult();
        if (question == null || question.trim().isEmpty()) {
            result.setType(FaqMatchResult.TYPE_NONE);
            return result;
        }
        String q = question.trim().toLowerCase();
        List<Faq> faqs = listAll();

        // 1. 完全相等 —— 直接答复
        for (Faq faq : faqs) {
            if (q.equals(faq.getQuestion().toLowerCase())) {
                return answer(result, faq);
            }
        }

        // 2. 强匹配（互相包含）：唯一命中直接答复，多个命中进入候选追问
        List<Faq> strong = new ArrayList<>();
        for (Faq faq : faqs) {
            String fq = faq.getQuestion().toLowerCase();
            if (q.contains(fq) || fq.contains(q)) {
                strong.add(faq);
            }
        }
        if (strong.size() == 1) {
            return answer(result, strong.get(0));
        }
        if (strong.size() > 1) {
            return candidates(result, strong);
        }

        // 3. 模糊匹配：按字符二元组相似度找出相近问题
        List<Map.Entry<Faq, Double>> scored = new ArrayList<>();
        for (Faq faq : faqs) {
            double sim = bigramSimilarity(q, faq.getQuestion().toLowerCase());
            if (sim >= SIMILARITY_THRESHOLD) {
                scored.add(new AbstractMap.SimpleEntry<>(faq, sim));
            }
        }
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        if (scored.isEmpty()) {
            result.setType(FaqMatchResult.TYPE_NONE);
            return result;
        }
        if (scored.size() == 1) {
            return answer(result, scored.get(0).getKey());
        }
        List<Faq> similar = new ArrayList<>();
        for (Map.Entry<Faq, Double> e : scored) {
            similar.add(e.getKey());
        }
        return candidates(result, similar);
    }

    private FaqMatchResult answer(FaqMatchResult result, Faq faq) {
        result.setType(FaqMatchResult.TYPE_ANSWER);
        result.setFaq(faq);
        result.setAnswer(faq.getAnswer());
        return result;
    }

    private FaqMatchResult candidates(FaqMatchResult result, List<Faq> faqs) {
        result.setType(FaqMatchResult.TYPE_CANDIDATES);
        result.setCandidates(faqs.size() > MAX_CANDIDATES ? faqs.subList(0, MAX_CANDIDATES) : faqs);
        return result;
    }

    /** 字符二元组 Dice 相似度，适用于中文短文本 */
    private double bigramSimilarity(String a, String b) {
        Set<String> sa = bigrams(a);
        Set<String> sb = bigrams(b);
        if (sa.isEmpty() || sb.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(sa);
        intersection.retainAll(sb);
        return 2.0 * intersection.size() / (sa.size() + sb.size());
    }

    private Set<String> bigrams(String s) {
        Set<String> set = new HashSet<>();
        if (s.length() == 1) {
            set.add(s);
            return set;
        }
        for (int i = 0; i < s.length() - 1; i++) {
            set.add(s.substring(i, i + 2));
        }
        return set;
    }
}
