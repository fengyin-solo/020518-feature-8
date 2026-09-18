package com.redtourism;

import com.redtourism.common.dto.FaqMatch;
import com.redtourism.entity.Faq;
import com.redtourism.service.impl.FaqServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 智能客服匹配算法单元测试（不依赖 Spring 容器与数据库）。
 */
class FaqMatchTest {

    static class TestFaqService extends FaqServiceImpl {
        private final List<Faq> faqs = new ArrayList<>();

        TestFaqService() {
            // 与 data.sql 中 8 条 FAQ 一致
            add(1L, "景点门票");
            add(2L, "开放时间");
            add(3L, "交通");
            add(4L, "住宿");
            add(5L, "美食推荐");
            add(6L, "预约");
            add(7L, "天气");
            add(8L, "客服");
        }

        private void add(long id, String q) {
            Faq f = new Faq();
            f.setId(id);
            f.setQuestion(q);
            f.setAnswer("answer-" + id);
            f.setSortOrder((int) id);
            faqs.add(f);
        }

        @Override
        public List<Faq> listAll() {
            return faqs;
        }
    }

    private final TestFaqService service = new TestFaqService();

    @Test
    void exactContainSingleMatch() {
        // 整句包含（用户问的就是 FAQ 原文）→ 1.0 唯一命中
        List<FaqMatch> r = service.match("景点门票");
        assertEquals(1, r.size());
        assertEquals(1L, r.get(0).getId());
        assertEquals(1.0d, r.get(0).getScore(), 0.001);
    }

    @Test
    void singleBigramHitStillSingleCandidate() {
        // "门票多少钱" 与 "景点门票" 只共享"门票"一个二元组（覆盖率1/3）→ 唯一候选直接答复
        List<FaqMatch> r = service.match("门票多少钱");
        assertEquals(1, r.size());
        assertEquals(1L, r.get(0).getId());
        assertEquals(0.33d, r.get(0).getScore(), 0.01);
    }

    @Test
    void multiCandidates() {
        // "门票预约"："预约"整词命中(1.0)，"门票"二元组命中(0.33) → 两个候选，预约在前
        List<FaqMatch> r = service.match("门票预约");
        assertEquals(2, r.size());
        assertEquals(Arrays.asList(6L, 1L), Arrays.asList(r.get(0).getId(), r.get(1).getId()));
        assertTrue(r.get(0).getScore() >= MATCH_THRESHOLD());
    }

    @Test
    void bigramOverlap() {
        // "怎么去景点" 与 "交通" 无直接包含，靠"景点门票"类问题不匹配，验证二元组：
        // "开放时间是什么时候" 命中 "开放时间"
        List<FaqMatch> r = service.match("开放时间是什么时候");
        assertFalse(r.isEmpty());
        assertEquals(2L, r.get(0).getId());
    }

    @Test
    void noMatch() {
        List<FaqMatch> r = service.match("阿斯蒂芬规划局快乐");
        assertTrue(r.isEmpty());
    }

    @Test
    void emptyQuestion() {
        assertTrue(service.match("").isEmpty());
        assertTrue(service.match(null).isEmpty());
    }

    @Test
    void resultsSortedByScoreDescThenSortOrder() {
        // "住宿推荐美食"：住宿、美食推荐都包含；验证顺序稳定且为降序
        List<FaqMatch> r = service.match("住宿推荐美食");
        assertTrue(r.size() >= 2);
        for (int i = 1; i < r.size(); i++) {
            assertTrue(r.get(i - 1).getScore() >= r.get(i).getScore());
        }
    }

    @Test
    void autoReplyFallbackWhenNoMatch() {
        String reply = service.autoReply("毫无关联的内容xyz");
        assertTrue(reply.contains("人工客服"));
    }

    private static double MATCH_THRESHOLD() {
        return 0.25d;
    }
}
