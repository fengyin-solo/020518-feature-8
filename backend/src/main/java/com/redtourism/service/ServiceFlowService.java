package com.redtourism.service;

import com.redtourism.entity.ServiceChat;
import com.redtourism.entity.ServiceFlowLog;
import com.redtourism.entity.ServiceSession;

import java.util.List;

/**
 * 客服链路服务：维护"提问→追问→答复→转人工"的会话阶段，
 * 持久化全部客服消息（保证历史顺序），并记录可追溯的处理经过。
 */
public interface ServiceFlowService {

    /** 获取或创建用户的客服会话 */
    ServiceSession getOrCreateSession(Long userId);

    /** 获取会话（不存在返回 null） */
    ServiceSession getSession(Long userId);

    /** 更新会话阶段 */
    void updateStage(Long userId, String stage);

    /** 更新会话阶段，同时记录候选/命中的 FAQ */
    void updateSession(Long userId, String stage, String pendingFaqIds, Long resolvedFaqId);

    /** 记录一步处理经过 */
    void log(Long userId, String step, String detail);

    /** 查询用户的处理经过（按时间正序） */
    List<ServiceFlowLog> listLogs(Long userId);

    /** 持久化一条客服消息 */
    ServiceChat saveChat(Long userId, String sender, String msgType, String content);

    /** 查询用户全部客服消息（按 id 正序，保证原有顺序） */
    List<ServiceChat> listChats(Long userId);
}
