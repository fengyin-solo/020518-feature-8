package com.redtourism.controller;

import com.redtourism.common.Constants;
import com.redtourism.common.Result;
import com.redtourism.entity.ServiceChat;
import com.redtourism.entity.ServiceSession;
import com.redtourism.entity.User;
import com.redtourism.service.ServiceFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ServiceChatController {

    @Autowired
    private ServiceFlowService flowService;

    @GetMapping("/send")
    public Result<String> send(@RequestParam String content, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        flowService.saveChat(user.getId(), Constants.CHAT_SENDER_USER, Constants.CHAT_MSG_TEXT, content);
        return Result.success("已发送", null);
    }

    /**
     * 当前用户的客服会话：阶段 + 全部消息（按 id 正序，重新进入页面保持原有顺序）。
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> history(HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(buildSessionView(user.getId()));
    }

    /**
     * 转人工客服：更新链路阶段、写入系统提示消息，
     * 并把之前的智能客服对话摘要一并记录到处理经过，人工客服可完整看到前文。
     */
    @GetMapping("/transfer")
    public Result<Map<String, Object>> transfer(HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        Long userId = user.getId();

        ServiceSession s = flowService.getOrCreateSession(userId);
        if (!Constants.CHAT_STAGE_TRANSFERRED.equals(s.getStage())
                && !Constants.CHAT_STAGE_HUMAN.equals(s.getStage())) {
            List<ServiceChat> chats = flowService.listChats(userId);
            String detail = chats.isEmpty()
                    ? "用户请求转人工客服（暂无前文对话）"
                    : "用户请求转人工客服，已携带前文 " + chats.size() + " 条对话。最近对话：\n" + buildTranscript(chats);
            flowService.log(userId, Constants.FLOW_TRANSFER, detail);
            flowService.saveChat(userId, Constants.CHAT_SENDER_BOT, Constants.CHAT_MSG_TRANSFER,
                    "已为您转接人工客服，您之前的咨询记录已一并转交，客服会尽快回复，请稍候。");
            flowService.updateSession(userId, Constants.CHAT_STAGE_TRANSFERRED, null, s.getResolvedFaqId());
        }
        return Result.success(buildSessionView(userId));
    }

    private Map<String, Object> buildSessionView(Long userId) {
        ServiceSession s = flowService.getSession(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("stage", s != null ? s.getStage() : Constants.CHAT_STAGE_BOT);
        data.put("messages", flowService.listChats(userId));
        return data;
    }

    /** 最近若干条对话的紧凑摘要，随转人工一并留痕 */
    private String buildTranscript(List<ServiceChat> chats) {
        int from = Math.max(0, chats.size() - 10);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < chats.size(); i++) {
            ServiceChat c = chats.get(i);
            String who = Constants.CHAT_SENDER_USER.equals(c.getSender()) ? "用户"
                    : Constants.CHAT_SENDER_ADMIN.equals(c.getSender()) ? "客服" : "智能客服";
            String content = Constants.CHAT_MSG_CANDIDATES.equals(c.getMsgType()) ? "[候选问题列表]" : c.getContent();
            if (content != null && content.length() > 50) content = content.substring(0, 50) + "…";
            sb.append(who).append("：").append(content);
            if (i < chats.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
