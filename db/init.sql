SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO category (id, name, parent_id)
VALUES (1, '결혼식', NULL),
       (2, '스드메', NULL),
       (3, '예물 예단', NULL),
       (4, '신혼집', NULL),
       (5, '신혼여행', NULL);

-- '결혼식'의 소주제
INSERT INTO category (id, name, parent_id)
VALUES (101, '결혼식장', 1),
       (102, '상견례', 1),
       (103, '가방순이', 1),
       (104, '축가', 1),
       (105, '사회자', 1),
       (106, '주례', 1),
       (107, '축의대', 1),
       (108, '본식 촬영', 1),
       (109, '청첩장', 1),
       (110, '부케', 1),
       (111, '청첩장 모임', 1),
       (112, '결혼 답례품', 1),
       (113, '기타', 1);

-- '스드메'의 소주제
INSERT INTO category (id, name, parent_id)
VALUES (201, '신부 드레스', 2),
       (202, '메이크업 샵', 2),
       (203, '스튜디오', 2),
       (204, '스냅 사진', 2),
       (205, '신랑 예복', 2),
       (206, '기타', 2);

-- '예물 예단'의 소주제
INSERT INTO category (id, name, parent_id)
VALUES (301, '웨딩 링', 3),
       (302, '부모님 한복', 3),
       (303, '예물', 3),
       (304, '기타', 3);

-- '신혼집'의 소주제
INSERT INTO category (id, name, parent_id)
VALUES (401, '가전 기기', 4),
       (402, '가구', 4),
       (403, '기타', 4);

-- '신혼여행'의 소주제
INSERT INTO category (id, name, parent_id)
VALUES (501, '항공권', 5),
       (502, '숙소', 5),
       (503, '액티비티', 5),
       (504, '기타', 5);
