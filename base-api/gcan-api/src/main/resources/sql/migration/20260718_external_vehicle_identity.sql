ALTER TABLE gcan_vehicle
    ADD COLUMN access_mode VARCHAR(20) NOT NULL DEFAULT 'GCAN' COMMENT '主接入方式' AFTER mine_id,
    ADD COLUMN external_vehicle_code VARCHAR(100) NULL COMMENT '外部车辆编码' AFTER access_mode,
    MODIFY COLUMN box_id_hex VARCHAR(2) NULL COMMENT 'GCAN盒子ID HEX',
    MODIFY COLUMN box_id_dec INT NULL COMMENT 'GCAN盒子ID DEC',
    ADD UNIQUE KEY uk_gcan_vehicle_external (mine_id, external_vehicle_code, deleted),
    ADD KEY idx_gcan_vehicle_access_mode (access_mode);
