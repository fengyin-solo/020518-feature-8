package com.redtourism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redtourism.common.Constants;
import com.redtourism.common.Result;
import com.redtourism.dto.FaqMatchResult;
import com.redtourism.entity.Faq;
import com.redtourism.entity.User;
import com.redtourism.service.FaqService;
import com.redtourism.service.ServiceFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/faq")
public class FaqController {

    @Autowired
    private FaqService faqService;
    @Autowired
    private ServiceFlowService flowService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/list")
    public Result<List<Faq>> list() {
        return Result.success(faqService.listAll());
    }

    /**
     * 智能客服提问：唯一命中直接答复；命中多个相近问题先列出候选让用户挑选；
     * 未命中提示可转人工。登录用户的问答全程落库并记录处理经过。
     */
    @GetMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestParam String question, HttpSession session) {
        FaqMatchResult match = faqService.match(question);
        Map<String, Object> data = new HashMap<>();
        data.put("type", match.getType());

        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) {
            // 未登录：无状态问答，不写会话与日志
            fillMatchData(data, match);
            return Result.success(data);
        }

        Long userId = user.getId();
        flowService.saveChat(userId, Constants.CHAT_SENDER_USER, Constants.CHAT_MSG_TEXT, question);
        flowService.log(userId, Constants.FLOW_ASK, "用户提问：" + question);

        String stage;
        if (FaqMatchResult.TYPE_ANSWER.equals(match.getType())) {
            Faq faq = match.getFaq();
            flowService.saveChat(userId, Constants.CHAT_SENDER_BOT, Constants.CHAT_MSG_TEXT, faq.getAnswer());
            flowService.log(userId, Constants.FLOW_ANSWER,
                    "唯一命中 FAQ#" + faq.getId() + "「" + faq.getQuestion() + "」，已直接给出完整答复");
            stage = Constants.CHAT_STAGE_ANSWERED;
            flowService.updateSession(userId, stage, null, faq.getId());
        } else if (FaqMatchResult.TYPE_CANDIDATES.equals(match.getType())) {
            List<Faq> candidates = match.getCandidates();
            flowService.saveChat(userId, Constants.CHAT_SENDER_BOT, Constants.CHAT_MSG_CANDIDATES,
                    buildCandidatesJson(candidates));
            flowService.log(userId, Constants.FLOW_CLARIFY,
                    "命中 " + candidates.size() + " 个相近问题，已列出候选请用户确认：" +
                    candidates.stream().map(f -> "FAQ#" + f.getId() + "「" + f.getQuestion() + "」")
                            .collect(Collectors.joining("、")));
            stage = Constants.CHAT_STAGE_CLARIFYING;
            flowService.updateSession(userId, stage,
                    candidates.stream().map(f -> String.valueOf(f.getId())).collect(Collectors.joining(",")),
                    null);
        } else {
            String fallback = "抱歉，暂时无法回答您的问题。您可以换个方式提问，或点击「转人工客服」获取帮助，服务热线：0851-12345。";
            flowService.saveChat(userId, Constants.CHAT_SENDER_BOT, Constants.CHAT_MSG_NONE, fallback);
            flowService.log(userId, Constants.FLOW_NONE, "未命中任何 FAQ：" + question);
            stage = Constants.CHAT_STAGE_BOT;
            flowService.updateStage(userId, stage);
        }
        fillMatchData(data, match);
        data.put("stage", stage);
        return Result.success(data);
    }

    /**
     * 用户从候选列表中选定问题，返回完整答复（先追问再回答）。
     */
    @GetMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestParam Long faqId, HttpSession session) {
        Faq faq = faqService.getById(faqId);
        if (faq == null) return Result.error("该问题不存在或已被删除");

        Map<String, Object> data = new HashMap<>();
        data.put("type", FaqMatchResult.TYPE_ANSWER);
        data.put("faqId", faq.getId());
        data.put("question", faq.getQuestion());
        data.put("answer", faq.getAnswer());

        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user != null) {
            Long userId = user.getId();
            flowService.saveChat(userId, Constants.CHAT_SENDER_USER, Constants.CHAT_MSG_TEXT, faq.getQuestion());
            flowService.saveChat(userId, Constants.CHAT_SENDER_BOT, Constants.CHAT_MSG_TEXT, faq.getAnswer());
            flowService.log(userId, Constants.FLOW_SELECT,
                    "用户从候选中选定 FAQ#" + faq.getId() + "「" + faq.getQuestion() + "」");
            flowService.log(userId, Constants.FLOW_ANSWER,
                    "已给出完整答复（FAQ#" + faq.getId() + "）");
            flowService.updateSession(userId, Constants.CHAT_STAGE_ANSWERED, null, faq.getId());
            data.put("stage", Constants.CHAT_STAGE_ANSWERED);
        }
        return Result.success(data);
    }

    private void fillMatchData(Map<String, Object> data, FaqMatchResult match) {
        if (FaqMatchResult.TYPE_ANSWER.equals(match.getType())) {
            data.put("faqId", match.getFaq().getId());
            data.put("question", match.getFaq().getQuestion());
            data.put("answer", match.getAnswer());
        } else if (FaqMatchResult.TYPE_CANDIDATES.equals(match.getType())) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Faq f : match.getCandidates()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", f.getId());
                item.put("question", f.getQuestion());
                list.add(item);
            }
            data.put("candidates", list);
        } else {
            data.put("answer", "抱歉，暂时无法回答您的问题。您可以换个方式提问，或点击「转人工客服」获取帮助，服务热线：0851-12345。");
        }
    }

    /** 候选问题消息内容（JSON），落库后历史记录可原样恢复候选按钮 */
    private String buildCandidatesJson(List<Faq> candidates) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("text", "为您找到以下相近问题，请选择您想了解的：");
        ArrayNode arr = root.putArray("candidates");
        for (Faq f : candidates) {
            ObjectNode item = arr.addObject();
            item.put("id", f.getId());
            item.put("question", f.getQuestion());
        }
        return root.toString();
    }
}
