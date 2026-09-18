package com.redtourism.controller;

import com.redtourism.common.Constants;
import com.redtourism.common.Result;
import com.redtourism.common.dto.ChatBotReply;
import com.redtourism.entity.Faq;
import com.redtourism.entity.User;
import com.redtourism.service.CustomerServiceService;
import com.redtourism.service.FaqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/faq")
public class FaqController {

    @Autowired
    private FaqService faqService;

    @Autowired
    private CustomerServiceService customerService;

    @GetMapping("/list")
    public Result<List<Faq>> list() {
        return Result.success(faqService.listAll());
    }

    /**
     * 旧版问答接口：只返回一句话，供无状态场景兼容使用。
     */
    @GetMapping("/ask")
    public Result<String> ask(@RequestParam String question) {
        return Result.success(faqService.autoReply(question));
    }

    /**
     * 智能客服提问（先追问再回答）：
     * 命中多个相近问题时返回候选项（CLARIFY），唯一命中给出完整答复（ANSWER），
     * 答不上来返回 FALLBACK 引导转人工。全过程落库，可追溯。
     */
    @GetMapping("/bot/ask")
    public Result<ChatBotReply> botAsk(@RequestParam String question, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(customerService.ask(user.getId(), question));
    }

    /**
     * 用户从候选追问中选定一项，返回该问题的完整答复。
     */
    @GetMapping("/bot/choose")
    public Result<ChatBotReply> botChoose(@RequestParam Long faqId, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(customerService.chooseFaq(user.getId(), faqId));
    }
}
