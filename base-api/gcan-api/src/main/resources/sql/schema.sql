CREATE TABLE IF NOT EXISTS gcan_vehicle (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    vehicle_name VARCHAR(100) NOT NULL COMMENT '车辆名称',
    vehicle_type VARCHAR(50) NOT NULL COMMENT '车辆类型',
    box_id_hex VARCHAR(2) NOT NULL COMMENT 'GCAN盒子ID HEX',
    box_id_dec INT NOT NULL COMMENT 'GCAN盒子ID DEC',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark VARCHAR(500) NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人',
    update_by BIGINT NULL COMMENT '更新人',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_gcan_vehicle_box_deleted (box_id_hex, deleted),
    KEY idx_gcan_vehicle_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GCAN车辆档案';

CREATE TABLE IF NOT EXISTS gcan_can_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    vehicle_id BIGINT NULL COMMENT '车辆ID',
    box_id_hex VARCHAR(2) NOT NULL COMMENT 'GCAN盒子ID HEX',
    box_id_dec INT NOT NULL COMMENT 'GCAN盒子ID DEC',
    can_id VARCHAR(8) NOT NULL COMMENT 'CAN ID',
    value0 INT NOT NULL COMMENT '数据0',
    value1 INT NOT NULL COMMENT '数据1',
    value2 INT NOT NULL COMMENT '数据2',
    value3 INT NOT NULL COMMENT '数据3',
    value4 INT NOT NULL COMMENT '数据4',
    value5 INT NOT NULL COMMENT '数据5',
    value6 INT NOT NULL COMMENT '数据6',
    value7 INT NOT NULL COMMENT '数据7',
    received_at DATETIME(3) NOT NULL COMMENT '接收时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_gcan_can_history_box_can_time (box_id_hex, can_id, received_at),
    KEY idx_gcan_can_history_vehicle_time (vehicle_id, received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GCAN CAN历史数据';
