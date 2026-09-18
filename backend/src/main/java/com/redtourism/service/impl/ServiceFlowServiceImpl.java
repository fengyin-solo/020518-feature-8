package com.redtourism.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.redtourism.common.Constants;
import com.redtourism.entity.ServiceChat;
import com.redtourism.entity.ServiceFlowLog;
import com.redtourism.entity.ServiceSession;
import com.redtourism.mapper.ServiceChatMapper;
import com.redtourism.mapper.ServiceFlowLogMapper;
import com.redtourism.mapper.ServiceSessionMapper;
import com.redtourism.service.ServiceFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ServiceFlowServiceImpl implements ServiceFlowService {

    @Autowired
    private ServiceSessionMapper sessionMapper;
    @Autowired
    private ServiceFlowLogMapper flowLogMapper;
    @Autowired
    private ServiceChatMapper chatMapper;

    @Override
    public ServiceSession getOrCreateSession(Long userId) {
        ServiceSession session = getSession(userId);
        if (session != null) return session;
        session = new ServiceSession();
        session.setUserId(userId);
        session.setStage(Constants.CHAT_STAGE_BOT);
        session.setCreateTime(new Date());
        session.setUpdateTime(new Date());
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public ServiceSession getSession(Long userId) {
        return sessionMapper.selectOne(
                new LambdaQueryWrapper<ServiceSession>().eq(ServiceSession::getUserId, userId));
    }

    @Override
    public void updateStage(Long userId, String stage) {
        updateSession(userId, stage, null, null);
    }

    @Override
    public void updateSession(Long userId, String stage, String pendingFaqIds, Long resolvedFaqId) {
        ServiceSession session = getOrCreateSession(userId);
        session.setStage(stage);
        session.setPendingFaqIds(pendingFaqIds);
        session.setResolvedFaqId(resolvedFaqId);
        session.setUpdateTime(new Date());
        sessionMapper.updateById(session);
    }

    @Override
    public void log(Long userId, String step, String detail) {
        ServiceFlowLog log = new ServiceFlowLog();
        log.setUserId(userId);
        log.setStep(step);
        log.setDetail(detail);
        log.setCreateTime(new Date());
        flowLogMapper.insert(log);
    }

    @Override
    public List<ServiceFlowLog> listLogs(Long userId) {
        return flowLogMapper.selectList(
                new LambdaQueryWrapper<ServiceFlowLog>()
                        .eq(ServiceFlowLog::getUserId, userId)
                        .orderByAsc(ServiceFlowLog::getId));
    }

    @Override
    public ServiceChat saveChat(Long userId, String sender, String msgType, String content) {
        ServiceChat chat = new ServiceChat();
        chat.setUserId(userId);
        chat.setSender(sender);
        chat.setMsgType(msgType);
        chat.setContent(content);
        chat.setCreateTime(new Date());
        chatMapper.insert(chat);
        return chat;
    }

    @Override
    public List<ServiceChat> listChats(Long userId) {
        // 按自增 id 正序返回，保证重新进入页面时历史消息保持原有顺序
        return chatMapper.selectList(
                new LambdaQueryWrapper<ServiceChat>()
                        .eq(ServiceChat::getUserId, userId)
                        .orderByAsc(ServiceChat::getCreateTime)
                        .orderByAsc(ServiceChat::getId));
    }
}
