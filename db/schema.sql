CREATE TABLE budget
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    created_at   datetime              NULL,
    updated_at   datetime              NULL,
    wedding_id   BIGINT                NOT NULL,
    total_budget BIGINT                NOT NULL,
    used_budget  BIGINT                NOT NULL,
    CONSTRAINT pk_budget PRIMARY KEY (id)
);

CREATE TABLE category
(
    id        BIGINT       NOT NULL,
    name      VARCHAR(255) NOT NULL,
    parent_id BIGINT       NULL,
    CONSTRAINT pk_category PRIMARY KEY (id),
    CONSTRAINT uq_category_id UNIQUE (id)
);

CREATE TABLE category_expense
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    created_at  datetime              NULL,
    updated_at  datetime              NULL,
    wedding_id  BIGINT                NOT NULL,
    category_id BIGINT                NOT NULL,
    expense     INT                   NOT NULL,
    CONSTRAINT pk_category_expense PRIMARY KEY (id)
);

CREATE TABLE chat_message
(
    message_id        BIGINT       NOT NULL,
    created_at        datetime     NULL,
    updated_at        datetime     NULL,
    room_id           BIGINT       NOT NULL,
    text              VARCHAR(255) NOT NULL,
    parent_message_id BIGINT       NULL,
    sender_id         BIGINT       NOT NULL,
    sent_at           datetime     NOT NULL,
    CONSTRAINT pk_chat_message PRIMARY KEY (message_id, room_id)
);

CREATE TABLE chat_message_content
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    created_at   datetime              NULL,
    updated_at   datetime              NULL,
    content_type VARCHAR(200)          NOT NULL,
    url          VARCHAR(255)          NOT NULL,
    message_id   BIGINT                NULL,
    room_id      BIGINT                NULL,
    CONSTRAINT pk_chat_message_content PRIMARY KEY (id)
);

CREATE TABLE chat_room
(
    id                    BIGINT AUTO_INCREMENT NOT NULL,
    created_at            datetime              NULL,
    updated_at            datetime              NULL,
    title                 VARCHAR(255)          NOT NULL,
    reserved_member_count INT                   NOT NULL,
    used_member_count     INT                   NOT NULL,
    image_url             VARCHAR(255)          NULL,
    user_id               BIGINT                NOT NULL,
    CONSTRAINT pk_chat_room PRIMARY KEY (id)
);

CREATE TABLE chat_room_message_counter
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    created_at      datetime              NULL,
    updated_at      datetime              NULL,
    room_id         BIGINT                NOT NULL,
    last_message_id BIGINT                NOT NULL,
    CONSTRAINT pk_chat_room_message_counter PRIMARY KEY (id)
);

CREATE TABLE chat_room_user
(
    id                   BIGINT AUTO_INCREMENT NOT NULL,
    created_at           datetime              NULL,
    updated_at           datetime              NULL,
    user_id              BIGINT                NOT NULL,
    chat_room_id         BIGINT                NOT NULL,
    last_read_message_id BIGINT                NOT NULL,
    CONSTRAINT pk_chat_room_user PRIMARY KEY (id)
);

CREATE TABLE email_auth
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    email_address VARCHAR(255)          NOT NULL,
    auth_code     VARCHAR(255)          NOT NULL,
    expired_at    datetime              NOT NULL,
    auth_status   BIT(1)                NOT NULL,
    CONSTRAINT pk_email_auth PRIMARY KEY (id)
);

CREATE TABLE event
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NULL,
    updated_at       datetime              NULL,
    wedding_id       BIGINT                NOT NULL,
    title            VARCHAR(255)          NOT NULL,
    date             datetime              NOT NULL,
    location         VARCHAR(255)          NULL,
    main_category_id BIGINT                NULL,
    sub_category_id  BIGINT                NULL,
    price            INT                   NULL,
    completed        BIT(1)                NOT NULL,
    CONSTRAINT pk_event PRIMARY KEY (id)
);

CREATE TABLE task
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at datetime              NULL,
    updated_at datetime              NULL,
    event_id   BIGINT                NOT NULL,
    name       VARCHAR(255)          NOT NULL,
    completed  BIT(1)                NOT NULL,
    CONSTRAINT pk_task PRIMARY KEY (id)
);

CREATE TABLE users
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    created_at    datetime              NULL,
    updated_at    datetime              NULL,
    email_address VARCHAR(255)          NOT NULL,
    password      VARCHAR(255)          NULL,
    name          VARCHAR(255)          NULL,
    nickname      VARCHAR(255)          NULL,
    avatar_url    VARCHAR(255)          NULL,
    my_code       VARCHAR(255)          NULL,
    wedding_id    BIGINT                NULL,
    wedding_role  VARCHAR(255)          NULL,
    provider      VARCHAR(255)          NULL,
    provider_id   VARCHAR(255)          NULL,
    `role`        VARCHAR(255)          NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE wedding
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    created_at   datetime              NULL,
    updated_at   datetime              NULL,
    bride_id     BIGINT                NULL,
    groom_id     BIGINT                NULL,
    wedding_day  datetime              NOT NULL,
    is_onboarded BIT(1)                NOT NULL,
    CONSTRAINT pk_wedding PRIMARY KEY (id)
);

CREATE TABLE wedding_category
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    created_at  datetime              NULL,
    updated_at  datetime              NULL,
    wedding_id  BIGINT                NOT NULL,
    category_id BIGINT                NOT NULL,
    CONSTRAINT pk_wedding_category PRIMARY KEY (id)
);

CREATE TABLE wish_item
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NULL,
    updated_at       datetime              NULL,
    wishlist_id      BIGINT                NOT NULL,
    naver_product_id BIGINT                NOT NULL,
    title            VARCHAR(255)          NOT NULL,
    image_url        VARCHAR(255)          NULL,
    price            INT                   NOT NULL,
    link_url         VARCHAR(255)          NOT NULL,
    mall_name        VARCHAR(255)          NULL,
    CONSTRAINT pk_wish_item PRIMARY KEY (id)
);

CREATE TABLE wishlist
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at datetime              NULL,
    updated_at datetime              NULL,
    wedding_id BIGINT                NOT NULL,
    name       VARCHAR(255)          NULL,
    CONSTRAINT pk_wishlist PRIMARY KEY (id)
);

ALTER TABLE budget
    ADD CONSTRAINT uc_budget_wedding UNIQUE (wedding_id);

ALTER TABLE wishlist
    ADD CONSTRAINT uc_wishlist_wedding UNIQUE (wedding_id);

CREATE UNIQUE INDEX idx_room_id ON chat_room_message_counter (room_id);

CREATE UNIQUE INDEX idx_user_room ON chat_room_user (user_id, chat_room_id);

CREATE INDEX idx_wedding_category ON category_expense (wedding_id, category_id);
