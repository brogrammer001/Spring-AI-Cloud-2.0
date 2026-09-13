-- ----------------------------
-- NL2SQL 语义层结构化表（建在业务库 ry-cloud，与 sys_user 等业务表同库）
-- 管理端维护，直查直用，不走向量。kbType=21 自由文本降级为长尾术语兜底。
-- 供 SemanticContextRenderer 加载：指标/维度/业务规则三层结构化渲染进 prompt
-- ----------------------------
SET NAMES utf8mb4;

-- 指标定义：统一口径，杜绝"复购率"每次被算出不同结果
DROP TABLE IF EXISTS `nl2sql_metric`;
CREATE TABLE `nl2sql_metric` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '指标ID',
  `metric_name` varchar(64) NOT NULL COMMENT '指标名，如复购率',
  `synonyms` varchar(255) DEFAULT NULL COMMENT '同义词，逗号分隔：回购率,重复购买率',
  `metric_expr` varchar(1024) NOT NULL COMMENT 'SQL表达式模板：COUNT(DISTINCT CASE WHEN 下单次数>=2 THEN user_id END) / COUNT(DISTINCT user_id)',
  `agg_default` varchar(32) DEFAULT 'SUM' COMMENT '默认聚合方式',
  `unit` varchar(16) DEFAULT NULL COMMENT '单位：% / 元 / 人',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用（1是 0否）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_metric_name` (`metric_name`),
  KEY `idx_metric_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='NL2SQL指标定义';

-- 维度定义：告诉 LLM 按什么字段分组、枚举值有哪些合法取值
DROP TABLE IF EXISTS `nl2sql_dimension`;
CREATE TABLE `nl2sql_dimension` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '维度ID',
  `dim_name` varchar(64) NOT NULL COMMENT '维度名，如订单状态',
  `table_name` varchar(64) NOT NULL COMMENT '所属表名',
  `column_name` varchar(64) NOT NULL COMMENT '所属列名',
  `synonyms` varchar(255) DEFAULT NULL COMMENT '同义词，逗号分隔',
  `dim_values` json DEFAULT NULL COMMENT '合法枚举值及含义：[{"value":"0","meaning":"待支付"},{"value":"1","meaning":"已支付"}]',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用（1是 0否）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dim_table_column` (`table_name`, `column_name`),
  KEY `idx_dim_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='NL2SQL维度定义';

-- 业务规则：时间口径、隐式过滤条件这类"每次都要遵守"的规则
DROP TABLE IF EXISTS `nl2sql_business_rule`;
CREATE TABLE `nl2sql_business_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_name` varchar(64) NOT NULL COMMENT '规则名',
  `rule_content` varchar(1024) NOT NULL COMMENT '"有效用户"= status=0 AND del_flag=0；"本月"= 本月1日至今',
  `applies_to` varchar(255) DEFAULT NULL COMMENT '适用关键词，逗号分隔，为空则全局生效',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用（1是 0否）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='NL2SQL业务规则';

-- ----------------------------
-- 初始种子数据（若依系统场景）
-- ----------------------------
-- 指标：用户状态相关
insert into nl2sql_metric(metric_name, synonyms, metric_expr, agg_default, unit, enabled, create_by) values
('用户总数', '用户数量,总用户数', 'COUNT(user_id)', 'COUNT', '人', '1', 'admin'),
('停用用户数', '禁用用户数,停用账号数', 'COUNT(CASE WHEN status = ''0'' THEN user_id END)', 'COUNT', '人', '1', 'admin'),
('部门人数', '部门员工数,部门人员数', 'COUNT(user_id)', 'COUNT', '人', '1', 'admin');

-- 维度：用户状态枚举（解决"查停用用户查空"类问题）
insert into nl2sql_dimension(dim_name, table_name, column_name, synonyms, dim_values, enabled, create_by) values
('用户状态', 'sys_user', 'status', '账号状态,用户状态', '[{"value":"0","meaning":"正常"},{"value":"1","meaning":"停用"}]', '1', 'admin'),
('性别', 'sys_user', 'sex', '用户性别', '[{"value":"0","meaning":"男"},{"value":"1","meaning":"女"},{"value":"2","meaning":"未知"}]', '1', 'admin'),
('删除标记', 'sys_user', 'del_flag', '删除状态', '[{"value":"0","meaning":"未删除"},{"value":"2","meaning":"已删除"}]', '1', 'admin');

-- 业务规则：全局生效的隐式过滤条件
insert into nl2sql_business_rule(rule_name, rule_content, applies_to, enabled, create_by) values
('有效用户过滤', '查询用户时默认必须加 del_flag = ''0'' 过滤已删除用户', NULL, '1', 'admin'),
('停用用户口径', '"停用用户"的SQL条件必须是 status = ''1''（1=停用，0=正常）', '停用,禁用', '1', 'admin');