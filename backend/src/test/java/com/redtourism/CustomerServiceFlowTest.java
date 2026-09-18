package com.redtourism;

import com.redtourism.common.Constants;
import com.redtourism.common.dto.ChatBotReply;
import com.redtourism.common.dto.FaqMatch;
import com.redtourism.entity.Faq;
import com.redtourism.entity.ServiceChat;
import com.redtourism.entity.ServiceSession;
import com.redtourism.entity.User;
import com.redtourism.mapper.ServiceChatMapper;
import com.redtourism.mapper.ServiceSessionMapper;
import com.redtourism.mapper.UserMapper;
import com.redtourism.service.FaqService;
import com.redtourism.service.impl.CustomerServiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 客服链路"提问→候选追问→选择→完整答复→转人工→人工回复"单元测试。
 */
class CustomerServiceFlowTest {

    @Mock ServiceChatMapper chatMapper;
    @Mock ServiceSessionMapper sessionMapper;
    @Mock UserMapper userMapper;
    @Mock FaqService faqService;

    @InjectMocks CustomerServiceServiceImpl service;

    static final Long UID = 2L;

    private List<ServiceChat> stored;
    private ServiceSession session;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stored = new ArrayList<>();
        session = null;

        // 模拟 MyBatis insert：入库到内存 list 并回填自增 id
        when(chatMapper.insert(any(ServiceChat.class))).thenAnswer(inv -> {
            ServiceChat c = inv.getArgument(0);
            java.lang.reflect.Field id = ServiceChat.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(c, (long) (stored.size() + 1));
            stored.add(c);
            return 1;
        });
        when(chatMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(stored));
        when(sessionMapper.selectOne(any())).thenAnswer(inv -> session);
        when(sessionMapper.insert(any(ServiceSession.class))).thenAnswer(inv -> {
            session = inv.getArgument(0);
            return 1;
        });
        when(sessionMapper.updateById(any(ServiceSession.class))).thenAnswer(inv -> {
            session = inv.getArgument(0);
            return 1;
        });

        User u = new User();
        u.setId(UID);
        u.setUsername("zhangsan");
        u.setNickname("张三");
        when(userMapper.selectById(UID)).thenReturn(u);
    }

    private Faq faq(long id, String q) {
        Faq f = new Faq();
        f.setId(id);
        f.setQuestion(q);
        f.setAnswer("答案-" + q);
        f.setSortOrder((int) id);
        return f;
    }

    @Test
    void fullFlow_askClarifyChooseAnswerHandoffAdminReply() {
        Faq f1 = faq(1L, "景点门票");
        Faq f6 = faq(6L, "预约");
        when(faqService.match("门票怎么预约")).thenReturn(Arrays.asList(
                FaqMatch.of(f6, 1.0d), FaqMatch.of(f1, 0.33d)));
        when(faqService.getById(1L)).thenReturn(f1);

        // 1) 提问 → CLARIFY 候选追问
        ChatBotReply ask = service.ask(UID, "门票怎么预约");
        assertEquals("CLARIFY", ask.getType());
        assertEquals(2, ask.getCandidates().size());
        assertNotNull(ask.getTraceNo());
        String traceNo = ask.getTraceNo();
        assertEquals(Constants.STAGE_CLARIFY, session.getStage());
        assertEquals("6,1", session.getPendingFaqIds());
        assertEquals(traceNo, session.getActiveTraceNo());

        // 落库顺序：USER(提问) → BOT(CLARIFY)
        assertEquals(2, stored.size());
        assertEquals(Constants.CHAT_SENDER_USER, stored.get(0).getSender());
        assertEquals(Constants.MSG_TYPE_CHAT, stored.get(0).getMsgType());
        assertEquals(Constants.STAGE_ASK, stored.get(0).getStage());
        assertEquals(traceNo, stored.get(0).getTraceNo());
        assertEquals(Constants.MSG_TYPE_CLARIFY, stored.get(1).getMsgType());
        assertEquals("6,1", stored.get(1).getRefIds());

        // 2) 选择候选 1 → 完整答复，沿用同一追踪号
        ChatBotReply choose = service.chooseFaq(UID, 1L);
        assertEquals("ANSWER", choose.getType());
        assertEquals("景点门票", choose.getQuestion());
        assertTrue(choose.getAnswer().startsWith("答案"));
        assertEquals(traceNo, choose.getTraceNo());
        assertEquals(Constants.STAGE_ANSWER, session.getStage());
        assertNull(session.getPendingFaqIds());
        assertNull(session.getActiveTraceNo()); // 一轮链路结束
        // 又落库：USER(选择) → BOT(ANSWER)
        assertEquals(4, stored.size());
        assertEquals(Constants.MSG_TYPE_ANSWER, stored.get(3).getMsgType());

        // 3) 选择一个不在候选列表中的问题应被拒绝
        ServiceSession before = session;
        assertThrows(IllegalArgumentException.class, () -> service.chooseFaq(UID, 99L));
        assertEquals(before, session);

        // 4) 转人工：阶段变为等待人工，转交通知落库并携带此前消息数
        ServiceChat handoff = service.handoff(UID);
        assertEquals(Constants.MSG_TYPE_HANDOFF, handoff.getMsgType());
        assertEquals(Constants.CHAT_SENDER_SYSTEM, handoff.getSender());
        assertEquals(Constants.STAGE_WAITING_HUMAN, session.getStage());
        assertTrue(handoff.getContent().contains("4 条对话记录"), "应携带此前4条对话，实际：" + handoff.getContent());

        // 5) 等待人工阶段用户消息
        ServiceChat userMsg = service.userSend(UID, "在吗？");
        assertEquals(Constants.STAGE_WAITING_HUMAN, userMsg.getStage());
        assertNull(userMsg.getTraceNo(), "人工消息不属于追问链路，无追踪号");

        // 6) 客服回复 → 人工服务中
        ServiceChat adminMsg = service.adminSend(UID, "您好，请问有什么可以帮您？");
        assertEquals(Constants.CHAT_SENDER_ADMIN, adminMsg.getSender());
        assertEquals(Constants.STAGE_IN_HUMAN, session.getStage());

        // 7) 历史严格按 id（时间）升序，顺序与落库一致
        List<ServiceChat> history = service.history(UID);
        assertEquals(7, history.size());
        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i).getId() > history.get(i - 1).getId());
        }

        // 8) 智能问答接口在人工阶段直接当人工消息发送
        ChatBotReply askInHuman = service.ask(UID, "再问一个");
        assertEquals("HUMAN", askInHuman.getType());
        assertEquals(8, stored.size());

        // 9) 非人工阶段不能直接发人工消息
        ServiceSession freshSession = new ServiceSession();
        freshSession.setUserId(99L);
        freshSession.setStage(Constants.STAGE_ASK);
        assertThrows(IllegalStateException.class, () -> {
            ServiceSession old = session;
            session = freshSession;
            try { service.userSend(99L, "hi"); } finally { session = old; }
        });
    }

    @Test
    void singleMatchGivesDirectAnswer() {
        when(faqService.match("天气")).thenReturn(Collections.singletonList(FaqMatch.of(faq(7L, "天气"), 1.0d)));
        ChatBotReply reply = service.ask(UID, "天气");
        assertEquals("ANSWER", reply.getType());
        assertEquals(7L, reply.getFaqId());
        assertEquals(Constants.STAGE_ANSWER, session.getStage());
    }

    @Test
    void noMatchGivesFallback() {
        when(faqService.match("xyz")).thenReturn(Collections.emptyList());
        ChatBotReply reply = service.ask(UID, "xyz");
        assertEquals("FALLBACK", reply.getType());
        // 仍停留在提问阶段，可继续提问或转人工
        assertEquals(Constants.STAGE_ASK, session.getStage());
        ServiceChat botMsg = stored.get(stored.size() - 1);
        assertEquals(Constants.MSG_TYPE_FALLBACK, botMsg.getMsgType());
        assertEquals(Constants.STAGE_ASK, botMsg.getStage());
    }

    @Test
    void chooseWithoutClarifyRejected() {
        // 全新会话没有 CLARIFY 阶段
        assertThrows(IllegalArgumentException.class, () -> service.chooseFaq(UID, 1L));
    }

    @Test
    void adminSessionsAggregateStageAndUnread() {
        when(faqService.match("天气")).thenReturn(Collections.singletonList(FaqMatch.of(faq(7L, "天气"), 1.0d)));
        service.ask(UID, "天气");           // USER + BOT ANSWER
        service.handoff(UID);              // SYSTEM HANDOFF
        service.userSend(UID, "有人吗");    // 未读 USER

        List<Map<String, Object>> sessions = service.adminSessions();
        assertEquals(1, sessions.size());
        Map<String, Object> s = sessions.get(0);
        assertEquals(UID, s.get("userId"));
        assertEquals("张三", s.get("username"));
        assertEquals(Constants.STAGE_WAITING_HUMAN, s.get("stage"));
        // 机器人与系统消息不算客服未读；只有真实 USER 人工消息计 1 条
        assertEquals(1, s.get("unreadCount"));
        assertNotNull(s.get("lastTime"));
    }

    @Test
    void adminReplyMarksInHumanAndClearsUnread() {
        service.handoff(UID);
        service.userSend(UID, "有人吗");
        service.adminSend(UID, "在的");

        Map<String, Object> s = service.adminSessions().get(0);
        assertEquals(Constants.STAGE_IN_HUMAN, s.get("stage"));
        assertEquals(0, s.get("unreadCount"));
    }
}
