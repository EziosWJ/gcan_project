INSERT INTO sys_config (config_name, config_key, config_value, config_type, value_type, status, is_builtin, remark)
SELECT '外部车辆数据源开关', 'gcan.external.enabled', 'false', 'SYSTEM', 'BOOLEAN', 1, 0, '关闭时不请求外部煤矿车辆接口'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gcan.external.enabled' AND deleted = 0);

INSERT INTO sys_config (config_name, config_key, config_value, config_type, value_type, status, is_builtin, remark)
SELECT '外部车辆数据源地址', 'gcan.external.base-url', '', 'SYSTEM', 'TEXT', 1, 0, '外部煤矿车辆接口服务根地址'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gcan.external.base-url' AND deleted = 0);

INSERT INTO sys_config (config_name, config_key, config_value, config_type, value_type, status, is_builtin, remark)
SELECT '外部煤矿列表路径', 'gcan.external.mine-list-endpoint', '/api/v1/mine-config/list', 'SYSTEM', 'TEXT', 1, 0, '外部煤矿配置接口路径'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gcan.external.mine-list-endpoint' AND deleted = 0);

INSERT INTO sys_config (config_name, config_key, config_value, config_type, value_type, status, is_builtin, remark)
SELECT '外部车辆数据路径', 'gcan.external.vehicle-data-endpoint', '/api/v1/vehicle-data/{mineCode}', 'SYSTEM', 'TEXT', 1, 0, '外部车辆数据接口路径'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gcan.external.vehicle-data-endpoint' AND deleted = 0);

INSERT INTO sys_config (config_name, config_key, config_value, config_type, value_type, status, is_builtin, remark)
SELECT '外部车辆轮询间隔', 'gcan.external.poll-interval-ms', '300000', 'SYSTEM', 'NUMBER', 1, 0, '外部车辆数据轮询间隔，单位毫秒'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gcan.external.poll-interval-ms' AND deleted = 0);

INSERT INTO sys_config (config_name, config_key, config_value, config_type, value_type, status, is_builtin, remark)
SELECT '外部接口连接超时', 'gcan.external.connect-timeout-ms', '3000', 'SYSTEM', 'NUMBER', 1, 0, '外部接口连接超时，单位毫秒'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gcan.external.connect-timeout-ms' AND deleted = 0);

INSERT INTO sys_config (config_name, config_key, config_value, config_type, value_type, status, is_builtin, remark)
SELECT '外部接口读取超时', 'gcan.external.read-timeout-ms', '10000', 'SYSTEM', 'NUMBER', 1, 0, '外部接口读取超时，单位毫秒'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gcan.external.read-timeout-ms' AND deleted = 0);

INSERT INTO sys_config (config_name, config_key, config_value, config_type, value_type, status, is_builtin, remark)
SELECT '外部数据新鲜度倍数', 'gcan.external.freshness-multiplier', '2', 'SYSTEM', 'NUMBER', 1, 0, '外部数据允许的轮询间隔倍数'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gcan.external.freshness-multiplier' AND deleted = 0);

INSERT INTO sys_dict_data (dict_type_id, dict_label, dict_value, sort_order, remark)
SELECT id, '外部接口车辆', 'EXTERNAL', 5, '煤矿接口车辆默认类型，不参与 GCAN 协议解析'
FROM sys_dict_type
WHERE dict_code = 'gcan_vehicle_type'
  AND NOT EXISTS (
      SELECT 1 FROM sys_dict_data data
      WHERE data.dict_type_id = sys_dict_type.id AND data.dict_value = 'EXTERNAL' AND data.deleted = 0
  );
