SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS red_tourism DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE red_tourism;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status INT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scenic_spot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(200),
    name_ja VARCHAR(200),
    description TEXT,
    description_en TEXT,
    description_ja TEXT,
    location VARCHAR(255),
    region VARCHAR(50),
    theme VARCHAR(50),
    open_time VARCHAR(100),
    ticket_price DECIMAL(10,2),
    traffic_info TEXT,
    history_background TEXT,
    revolution_event TEXT,
    person_story TEXT,
    cover_image VARCHAR(255),
    status INT DEFAULT 1,
    view_count BIGINT DEFAULT 0,
    favorite_count BIGINT DEFAULT 0,
    avg_rating DOUBLE DEFAULT 0,
    comment_count BIGINT DEFAULT 0,
    staff_id BIGINT COMMENT '所属工作人员ID（STAFF）',
    longitude DOUBLE,
    latitude DOUBLE,
    ticket_reservation VARCHAR(500),
    ticket_reservation_en VARCHAR(1000),
    ticket_reservation_ja VARCHAR(1000),
    suggested_duration VARCHAR(100),
    suggested_duration_en VARCHAR(200),
    suggested_duration_ja VARCHAR(200),
    items_to_bring TEXT,
    items_to_bring_en TEXT,
    items_to_bring_ja TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_staff_id (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scenic_spot_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spot_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    sort_order INT DEFAULT 0,
    INDEX idx_spot_id (spot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(200),
    name_ja VARCHAR(200),
    description TEXT,
    description_en TEXT,
    description_ja TEXT,
    days INT,
    theme VARCHAR(50),
    cover_image VARCHAR(255),
    traffic_suggestion TEXT,
    hotel_suggestion TEXT,
    budget DECIMAL(10,2),
    view_count BIGINT DEFAULT 0,
    favorite_count BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS route_spot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT NOT NULL,
    spot_id BIGINT NOT NULL,
    day_number INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    description VARCHAR(500),
    INDEX idx_route_id (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS culture_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS culture_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    title_en VARCHAR(300),
    title_ja VARCHAR(300),
    content LONGTEXT,
    content_en LONGTEXT,
    content_ja LONGTEXT,
    category_id BIGINT,
    cover_image VARCHAR(255),
    author VARCHAR(50),
    view_count BIGINT DEFAULT 0,
    favorite_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hotel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    cover_image VARCHAR(255),
    price DECIMAL(10,2),
    has_breakfast INT DEFAULT 0,
    has_room_service INT DEFAULT 0,
    phone VARCHAR(20),
    status INT DEFAULT 1,
    rating DOUBLE DEFAULT 0,
    longitude DOUBLE,
    latitude DOUBLE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS food (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    price DECIMAL(10,2),
    cover_image VARCHAR(255),
    store_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS food_store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    category VARCHAR(50),
    hygiene_level VARCHAR(20),
    phone VARCHAR(20),
    cover_image VARCHAR(255),
    longitude DOUBLE,
    latitude DOUBLE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    content TEXT,
    images VARCHAR(1000),
    rating INT DEFAULT 5,
    reply_content TEXT,
    reply_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS like_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'PENDING',
    pay_method VARCHAR(20),
    pay_time DATETIME,
    target_name VARCHAR(100),
    quantity INT DEFAULT 1,
    check_in_date DATE,
    check_out_date DATE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    content TEXT,
    is_read INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户自定义线路
CREATE TABLE IF NOT EXISTS user_custom_route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    days INT DEFAULT 1,
    spot_data TEXT COMMENT 'JSON格式景点列表',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT|SUBMITTED|APPROVED|REJECTED',
    reject_reason TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE COMMENT '角色代码 USER/ADMIN/STAFF',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色-菜单权限表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(30) NOT NULL,
    menu_key VARCHAR(80) NOT NULL COMMENT '菜单/按钮标识',
    menu_name VARCHAR(100) COMMENT '菜单名称',
    enabled INT DEFAULT 1 COMMENT '1=启用 0=禁用',
    UNIQUE KEY uk_role_menu (role_code, menu_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 景点更正建议
CREATE TABLE IF NOT EXISTS spot_suggestion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    spot_id BIGINT NOT NULL,
    spot_name VARCHAR(100),
    field_name VARCHAR(50) NOT NULL COMMENT '修改的字段',
    old_value TEXT,
    new_value TEXT NOT NULL,
    reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVED|REJECTED',
    reject_reason TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 客服对话（智能客服追问链路 + 人工客服统一消息流）
CREATE TABLE IF NOT EXISTS service_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sender VARCHAR(10) NOT NULL COMMENT 'USER用户 / ADMIN人工客服 / BOT智能客服 / SYSTEM系统',
    msg_type VARCHAR(16) NOT NULL DEFAULT 'CHAT' COMMENT 'CHAT普通消息/CLARIFY候选追问/ANSWER完整答复/FALLBACK未命中/HANDOFF转人工通知',
    content TEXT NOT NULL,
    ref_ids VARCHAR(255) COMMENT '候选FAQ id列表，逗号分隔（CLARIFY使用）',
    trace_no VARCHAR(32) COMMENT '处理经过追踪号：同一轮提问链路（提问→追问→答复/转人工）共用一个编号',
    stage VARCHAR(16) COMMENT '该消息产生时所处链路阶段：ASK/CLARIFY/ANSWER/WAITING_HUMAN/IN_HUMAN',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_trace (trace_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 客服会话状态（每个用户一条，记录链路当前走到哪一步）
CREATE TABLE IF NOT EXISTS service_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    stage VARCHAR(16) NOT NULL DEFAULT 'ASK' COMMENT 'ASK智能客服提问中/CLARIFY待用户选择候选/ANSWER机器人已答复/WAITING_HUMAN等待人工接入/IN_HUMAN人工服务中',
    pending_faq_ids VARCHAR(255) COMMENT 'CLARIFY阶段待选FAQ id列表，逗号分隔',
    active_trace_no VARCHAR(32) COMMENT '当前进行中的处理经过追踪号',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问题反馈
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    contact VARCHAR(100),
    category VARCHAR(30) DEFAULT 'GENERAL' COMMENT 'BUG|SUGGESTION|COMPLAINT|GENERAL',
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING|PROCESSING|RESOLVED',
    reply TEXT,
    reply_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

