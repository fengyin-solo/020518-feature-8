package com.redtourism.service;

import com.redtourism.common.dto.ChatBotReply;
import com.redtourism.entity.ServiceChat;
import com.redtourism.entity.ServiceSession;

import java.util.List;
import java.util.Map;

/**
 * 客服链路服务：统一承载"提问 → 候选追问 → 完整答复 → 转人工 → 人工会话"全过程，
 * 每一步都落库到 service_chat（可追溯），当前所处阶段记录在 service_session。
 */
public interface CustomerServiceService {

    /** 获取用户会话状态（没有时返回一个 stage=ASK 的默认对象，不写库） */
    ServiceSession getSession(Long userId);

    /** 用户提问：先匹配 FAQ，命中唯一问题直接答复，命中多个相近问题返回候选项，答不上来引导转人工 */
    ChatBotReply ask(Long userId, String question);

    /** 用户从候选追问中选定一个问题，给出完整答复 */
    ChatBotReply chooseFaq(Long userId, Long faqId);

    /** 转接人工客服：携带此前全部对话记录，链路进入等待人工接入阶段 */
    ServiceChat handoff(Long userId);

    /** 人工模式下用户发送消息 */
    ServiceChat userSend(Long userId, String content);

    /** 人工客服回复，链路进入人工服务中 */
    ServiceChat adminSend(Long userId, String content);

    /** 按时间顺序、id 顺序返回完整消息流（保证重进页面历史顺序不变） */
    List<ServiceChat> history(Long userId);

    /** 管理端会话列表：每条会话带当前链路阶段、最新消息预览、未读数等 */
    List<Map<String, Object>> adminSessions();
}
