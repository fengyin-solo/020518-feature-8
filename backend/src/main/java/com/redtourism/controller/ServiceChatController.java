package com.redtourism.controller;

import com.redtourism.common.Constants;
import com.redtourism.common.Result;
import com.redtourism.entity.ServiceChat;
import com.redtourism.entity.ServiceSession;
import com.redtourism.entity.User;
import com.redtourism.service.CustomerServiceService;
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
    private CustomerServiceService customerService;

    /**
     * 发送消息：
     * 人工链路（WAITING_HUMAN/IN_HUMAN）阶段作为给客服的消息；
     * 智能客服阶段请改用 /api/faq/bot/ask。
     */
    @GetMapping("/send")
    public Result<ServiceChat> send(@RequestParam String content, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(customerService.userSend(user.getId(), content));
    }

    /**
     * 一键转接人工客服：此前与智能客服的全部对话记录一并带入人工会话。
     */
    @GetMapping("/handoff")
    public Result<ServiceChat> handoff(HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(customerService.handoff(user.getId()));
    }

    /**
     * 客服会话状态 + 完整消息流（按时间顺序，重新进入页面历史顺序保持不变）。
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> history(HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        ServiceSession sessionState = customerService.getSession(user.getId());
        List<ServiceChat> messages = customerService.history(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("stage", sessionState.getStage());
        data.put("pendingFaqIds", sessionState.getPendingFaqIds());
        data.put("activeTraceNo", sessionState.getActiveTraceNo());
        data.put("messages", messages);
        return Result.success(data);
    }
}
