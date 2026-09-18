package com.redtourism.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.redtourism.service.CustomerServiceService;
import com.redtourism.service.FaqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CustomerServiceServiceImpl implements CustomerServiceService {

    /** CLARIFY 候选追问最多展示条数 */
    private static final int MAX_CANDIDATES = 5;

    @Autowired private ServiceChatMapper chatMapper;
    @Autowired private ServiceSessionMapper sessionMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private FaqService faqService;

    @Override
    public ServiceSession getSession(Long userId) {
        ServiceSession session = findSession(userId);
        if (session != null) return session;
        session = new ServiceSession();
        session.setUserId(userId);
        session.setStage(Constants.STAGE_ASK);
        return session;
    }

    @Override
    public ChatBotReply ask(Long userId, String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入您的问题");
        }
        question = question.trim();

        // 人工服务阶段的消息走人工通道，不经过 FAQ 匹配
        ServiceSession session = getOrCreateSession(userId);
        if (Constants.STAGE_WAITING_HUMAN.equals(session.getStage())
                || Constants.STAGE_IN_HUMAN.equals(session.getStage())) {
            userSend(userId, question);
            ChatBotReply reply = new ChatBotReply();
            reply.setType("HUMAN");
            reply.setTraceNo(session.getActiveTraceNo());
            reply.setContent("消息已发送给人工客服");
            return reply;
        }

        String traceNo = newTraceNo();

        // 1) 用户提问落库（阶段：ASK）
        saveMsg(userId, Constants.CHAT_SENDER_USER, Constants.MSG_TYPE_CHAT, question,
                null, traceNo, Constants.STAGE_ASK);

        // 2) 匹配相近 FAQ
        List<FaqMatch> matches = faqService.match(question);

        ChatBotReply reply = new ChatBotReply();
        reply.setTraceNo(traceNo);

        if (matches.isEmpty()) {
            // 3a) 答不上来：引导转人工（仍处于提问阶段，可继续提问或一键转人工）
            String content = "抱歉，这个问题我暂时没能找到准确答案。\n"
                    + "您可以换个说法再试一次，或点击下方按钮转接人工客服，之前的对话内容会一并带给客服。";
            saveMsg(userId, Constants.CHAT_SENDER_BOT, Constants.MSG_TYPE_FALLBACK, content,
                    null, traceNo, Constants.STAGE_ASK);
            updateSession(session, Constants.STAGE_ASK, null, null);
            reply.setType("FALLBACK");
            reply.setContent(content);
            return reply;
        }

        if (matches.size() == 1) {
            // 3b) 唯一命中：直接给出完整答复
            FaqMatch m = matches.get(0);
            String content = buildAnswerContent(m);
            saveMsg(userId, Constants.CHAT_SENDER_BOT, Constants.MSG_TYPE_ANSWER, content,
                    String.valueOf(m.getId()), traceNo, Constants.STAGE_ANSWER);
            updateSession(session, Constants.STAGE_ANSWER, null, null);
            fillAnswer(reply, m, content);
            return reply;
        }

        // 3c) 命中多个相近问题：先追问，列出候选项让用户挑选
        List<FaqMatch> candidates = matches.size() > MAX_CANDIDATES
                ? new ArrayList<>(matches.subList(0, MAX_CANDIDATES)) : matches;
        String clarifyText = "您是不是想咨询以下问题？请点击最接近的一项：";
        String refIds = joinIds(candidates);
        saveMsg(userId, Constants.CHAT_SENDER_BOT, Constants.MSG_TYPE_CLARIFY, clarifyText,
                refIds, traceNo, Constants.STAGE_CLARIFY);
        updateSession(session, Constants.STAGE_CLARIFY, refIds, traceNo);
        reply.setType("CLARIFY");
        reply.setContent(clarifyText);
        reply.setCandidates(candidates);
        return reply;
    }

    @Override
    public ChatBotReply chooseFaq(Long userId, Long faqId) {
        if (faqId == null) throw new IllegalArgumentException("请选择一个问题");
        ServiceSession session = findSession(userId);
        if (session == null || !Constants.STAGE_CLARIFY.equals(session.getStage())) {
            throw new IllegalArgumentException("当前没有待选择的候选问题，请直接描述您的问题");
        }
        Set<Long> pending = parseIds(session.getPendingFaqIds());
        if (!pending.contains(faqId)) {
            throw new IllegalArgumentException("该问题不在候选列表中，请重新选择或重新提问");
        }
        Faq faq = faqService.getById(faqId);
        if (faq == null) throw new IllegalArgumentException("所选问题不存在或已被删除");

        String traceNo = session.getActiveTraceNo();

        // 用户选择落库（阶段仍为 CLARIFY，记录用户选了哪一问）
        saveMsg(userId, Constants.CHAT_SENDER_USER, Constants.MSG_TYPE_CHAT, faq.getQuestion(),
                String.valueOf(faqId), traceNo, Constants.STAGE_CLARIFY);

        // 完整答复落库（阶段：ANSWER）
        FaqMatch m = new FaqMatch();
        m.setId(faq.getId());
        m.setQuestion(faq.getQuestion());
        m.setAnswer(faq.getAnswer());
        m.setSortOrder(faq.getSortOrder());
        m.setScore(1.0d);
        String content = buildAnswerContent(m);
        saveMsg(userId, Constants.CHAT_SENDER_BOT, Constants.MSG_TYPE_ANSWER, content,
                String.valueOf(faqId), traceNo, Constants.STAGE_ANSWER);
        updateSession(session, Constants.STAGE_ANSWER, null, null);

        ChatBotReply reply = new ChatBotReply();
        reply.setTraceNo(traceNo);
        fillAnswer(reply, m, content);
        return reply;
    }

    @Override
    public ServiceChat handoff(Long userId) {
        ServiceSession session = getOrCreateSession(userId);
        List<ServiceChat> previous = history(userId);

        // 过滤掉此前的转人工通知，避免重复刷屏
        int carried = 0;
        for (ServiceChat c : previous) {
            if (!Constants.MSG_TYPE_HANDOFF.equals(c.getMsgType())) carried++;
        }

        String content = "【已转接人工客服】\n"
                + "您好，已为您接通人工客服。智能客服与您此前的 " + carried + " 条对话记录已一并转接，"
                + "客服可看到完整沟通过程，请稍候，客服马上为您服务。";
        ServiceChat handoffMsg = saveMsg(userId, Constants.CHAT_SENDER_SYSTEM,
                Constants.MSG_TYPE_HANDOFF, content, null,
                session.getActiveTraceNo() != null ? session.getActiveTraceNo() : newTraceNo(),
                Constants.STAGE_WAITING_HUMAN);

        updateSession(session, Constants.STAGE_WAITING_HUMAN, null, null);
        return handoffMsg;
    }

    @Override
    public ServiceChat userSend(Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        ServiceSession session = getOrCreateSession(userId);
        if (!Constants.STAGE_WAITING_HUMAN.equals(session.getStage())
                && !Constants.STAGE_IN_HUMAN.equals(session.getStage())) {
            throw new IllegalStateException("当前为智能客服会话，请先转接人工客服");
        }
        return saveMsg(userId, Constants.CHAT_SENDER_USER, Constants.MSG_TYPE_CHAT, content.trim(),
                null, null, session.getStage());
    }

    @Override
    public ServiceChat adminSend(Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("回复内容不能为空");
        }
        if (userMapper.selectById(userId) == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        ServiceSession session = getOrCreateSession(userId);
        ServiceChat msg = saveMsg(userId, Constants.CHAT_SENDER_ADMIN, Constants.MSG_TYPE_CHAT,
                content.trim(), null, null, Constants.STAGE_IN_HUMAN);
        // 客服首次回复即视为人工接入，链路进入"人工服务中"
        updateSession(session, Constants.STAGE_IN_HUMAN, null, null);
        return msg;
    }

    @Override
    public List<ServiceChat> history(Long userId) {
        return chatMapper.selectList(new LambdaQueryWrapper<ServiceChat>()
                .eq(ServiceChat::getUserId, userId)
                .orderByAsc(ServiceChat::getCreateTime)
                .orderByAsc(ServiceChat::getId));
    }

    @Override
    public List<Map<String, Object>> adminSessions() {
        // 取全部消息：id 升序便于按用户计算未读数，会话排序另用"最新消息时间"
        List<ServiceChat> asc = chatMapper.selectList(new LambdaQueryWrapper<ServiceChat>()
                .orderByAsc(ServiceChat::getCreateTime)
                .orderByAsc(ServiceChat::getId));

        Map<Long, ServiceChat> latestByUser = new HashMap<>();
        Map<Long, Integer> unreadByUser = new HashMap<>();
        Set<Long> handoffReached = new HashSet<>();
        Set<Long> humanChatLegacy = new HashSet<>(); // 兼容老数据：有 ADMIN 消息但无 HANDOFF 的用户

        for (ServiceChat c : asc) {
            Long uid = c.getUserId();
            latestByUser.put(uid, c);
            if (Constants.MSG_TYPE_HANDOFF.equals(c.getMsgType())) {
                handoffReached.add(uid);
            } else if (Constants.CHAT_SENDER_ADMIN.equals(c.getSender())) {
                humanChatLegacy.add(uid);
                unreadByUser.put(uid, 0); // 客服回复后未读清零
            } else if (Constants.CHAT_SENDER_USER.equals(c.getSender())
                    && (handoffReached.contains(uid) || humanChatLegacy.contains(uid))) {
                // 仅人工通道开启（转人工之后，或老的人工对话）中的用户消息才计客服未读，
                // 机器人追问阶段的提问不计入
                unreadByUser.merge(uid, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, ServiceChat> entry : latestByUser.entrySet()) {
            Long userId = entry.getKey();
            ServiceChat latest = entry.getValue();
            ServiceSession session = findSession(userId);

            Map<String, Object> m = new HashMap<>();
            m.put("userId", userId);
            User u = userMapper.selectById(userId);
            m.put("username", u != null
                    ? (u.getNickname() != null ? u.getNickname() : u.getUsername())
                    : "用户" + userId);
            m.put("lastMessage", latest.getContent());
            m.put("lastMsgType", latest.getMsgType());
            m.put("lastSender", latest.getSender());
            m.put("lastTime", latest.getCreateTime());
            m.put("stage", session != null ? session.getStage() : deriveStage(latest));
            m.put("unreadCount", unreadByUser.getOrDefault(userId, 0));
            result.add(m);
        }
        // 最新消息在前
        result.sort((a, b) -> {
            Date ta = (Date) a.get("lastTime");
            Date tb = (Date) b.get("lastTime");
            return tb == null ? 1 : (ta == null ? -1 : tb.compareTo(ta));
        });
        return result;
    }

    /* ==================== 内部方法 ==================== */

    private String buildAnswerContent(FaqMatch m) {
        return m.getQuestion() + "\n" + m.getAnswer();
    }

    private void fillAnswer(ChatBotReply reply, FaqMatch m, String content) {
        reply.setType("ANSWER");
        reply.setContent(content);
        reply.setFaqId(m.getId());
        reply.setQuestion(m.getQuestion());
        reply.setAnswer(m.getAnswer());
    }

    private ServiceChat saveMsg(Long userId, String sender, String msgType, String content,
                                String refIds, String traceNo, String stage) {
        ServiceChat c = new ServiceChat();
        c.setUserId(userId);
        c.setSender(sender);
        c.setMsgType(msgType);
        c.setContent(content);
        c.setRefIds(refIds);
        c.setTraceNo(traceNo);
        c.setStage(stage);
        c.setCreateTime(new Date());
        chatMapper.insert(c);
        return c;
    }

    private ServiceSession findSession(Long userId) {
        return sessionMapper.selectOne(new LambdaQueryWrapper<ServiceSession>()
                .eq(ServiceSession::getUserId, userId));
    }

    private ServiceSession getOrCreateSession(Long userId) {
        ServiceSession session = findSession(userId);
        if (session == null) {
            session = new ServiceSession();
            session.setUserId(userId);
            session.setStage(Constants.STAGE_ASK);
            session.setCreateTime(new Date());
            session.setUpdateTime(new Date());
            sessionMapper.insert(session);
        }
        return session;
    }

    private void updateSession(ServiceSession session, String stage, String pendingFaqIds,
                               String activeTraceNo) {
        session.setStage(stage);
        session.setPendingFaqIds(pendingFaqIds);
        session.setActiveTraceNo(activeTraceNo);
        session.setUpdateTime(new Date());
        sessionMapper.updateById(session);
    }

    /** 老数据（迁移前的 service_chat 行没有阶段）兜底推断 */
    private String deriveStage(ServiceChat latest) {
        if (latest.getStage() != null) return latest.getStage();
        return Constants.CHAT_SENDER_ADMIN.equals(latest.getSender())
                ? Constants.STAGE_IN_HUMAN : Constants.STAGE_WAITING_HUMAN;
    }

    private String joinIds(List<FaqMatch> candidates) {
        StringBuilder sb = new StringBuilder();
        for (FaqMatch c : candidates) {
            if (sb.length() > 0) sb.append(',');
            sb.append(c.getId());
        }
        return sb.toString();
    }

    private Set<Long> parseIds(String ids) {
        Set<Long> set = new HashSet<>();
        if (ids == null || ids.trim().isEmpty()) return set;
        for (String s : ids.split(",")) {
            try {
                set.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return set;
    }

    private String newTraceNo() {
        String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int rand = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "T" + ts + rand;
    }
}
