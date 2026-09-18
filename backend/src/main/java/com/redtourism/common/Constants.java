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

    /* ==================== 客服链路 ==================== */
    /** 消息发送方 */
    public static final String CHAT_SENDER_USER = "USER";
    public static final String CHAT_SENDER_ADMIN = "ADMIN";
    public static final String CHAT_SENDER_BOT = "BOT";
    public static final String CHAT_SENDER_SYSTEM = "SYSTEM";

    /** 消息类型 */
    public static final String MSG_TYPE_CHAT = "CHAT";
    public static final String MSG_TYPE_CLARIFY = "CLARIFY";
    public static final String MSG_TYPE_ANSWER = "ANSWER";
    public static final String MSG_TYPE_FALLBACK = "FALLBACK";
    public static final String MSG_TYPE_HANDOFF = "HANDOFF";

    /** 链路阶段：ASK提问中 / CLARIFY待选候选 / ANSWER机器人已答复 / WAITING_HUMAN等待人工 / IN_HUMAN人工服务中 */
    public static final String STAGE_ASK = "ASK";
    public static final String STAGE_CLARIFY = "CLARIFY";
    public static final String STAGE_ANSWER = "ANSWER";
    public static final String STAGE_WAITING_HUMAN = "WAITING_HUMAN";
    public static final String STAGE_IN_HUMAN = "IN_HUMAN";
}
