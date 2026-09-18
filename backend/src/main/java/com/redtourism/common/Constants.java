package com.redtourism.common;

public class Constants {
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STAFF = "STAFF";
    public static final String SESSION_USER = "currentUser";

    public static final String TARGET_SPOT = "SPOT";
    public static final String TARGET_ROUTE = "ROUTE";
    public static final String TARGET_CULTURE = "CULTURE";
    public static final String TARGET_HOTEL = "HOTEL";
    public static final String TARGET_FOOD = "FOOD";
    public static final String TARGET_COMMENT = "COMMENT";

    public static final String ORDER_HOTEL = "HOTEL";
    public static final String ORDER_FOOD = "FOOD";

    public static final String PAY_WECHAT = "WECHAT";
    public static final String PAY_BANK_ICBC = "BANK_ICBC";
    public static final String PAY_BANK_CCB = "BANK_CCB";
    public static final String PAY_BANK_ABC = "BANK_ABC";
    public static final String PAY_BANK_BOC = "BANK_BOC";
    public static final String PAY_BANK_BOCOM = "BANK_BOCOM";
    public static final String PAY_BANK_CMB = "BANK_CMB";
    public static final String PAY_BANK_PSBC = "BANK_PSBC";

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    /* ========== 客服链路阶段 ========== */
    public static final String CHAT_STAGE_BOT = "BOT";                  // 智能客服接待中
    public static final String CHAT_STAGE_CLARIFYING = "CLARIFYING";    // 已给出候选问题，待用户选择
    public static final String CHAT_STAGE_ANSWERED = "ANSWERED";        // 智能客服已给出完整答复
    public static final String CHAT_STAGE_TRANSFERRED = "TRANSFERRED";  // 已转人工，等待客服接入
    public static final String CHAT_STAGE_HUMAN = "HUMAN";              // 人工客服服务中

    /* ========== 客服消息发送方 ========== */
    public static final String CHAT_SENDER_USER = "USER";
    public static final String CHAT_SENDER_BOT = "BOT";
    public static final String CHAT_SENDER_ADMIN = "ADMIN";

    /* ========== 客服消息类型 ========== */
    public static final String CHAT_MSG_TEXT = "TEXT";
    public static final String CHAT_MSG_CANDIDATES = "CANDIDATES";  // 候选问题列表（content 为 JSON）
    public static final String CHAT_MSG_NONE = "NONE";              // 未命中提示（前端渲染时附带转人工入口）
    public static final String CHAT_MSG_TRANSFER = "TRANSFER";      // 转人工系统提示

    /* ========== 客服链路日志步骤 ========== */
    public static final String FLOW_ASK = "ASK";                // 用户提问
    public static final String FLOW_CLARIFY = "CLARIFY";        // 列出候选问题
    public static final String FLOW_SELECT = "SELECT";          // 用户选择候选
    public static final String FLOW_ANSWER = "ANSWER";          // 给出完整答复
    public static final String FLOW_NONE = "NONE";              // 未命中任何FAQ
    public static final String FLOW_TRANSFER = "TRANSFER";      // 转人工
    public static final String FLOW_ADMIN_REPLY = "ADMIN_REPLY";// 人工客服接入回复
}
