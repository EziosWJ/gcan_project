-- 已初始化过数据库时执行本脚本；每条菜单按 permission_code 幂等写入。
INSERT INTO sys_menu
  (parent_id, menu_name, menu_type, path, component, icon, permission_code, sort_order, visible, status, is_builtin)
SELECT 0, 'GCAN 管理', 'DIR', '/gcan', NULL, 'Boxes', 'gcan', 5, 1, 1, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE permission_code = 'gcan' AND deleted = 0
);

INSERT INTO sys_menu
  (parent_id, menu_name, menu_type, path, component, icon, permission_code, sort_order, visible, status, is_builtin)
SELECT p.id, v.menu_name, 'MENU', v.path, v.component, v.icon, v.permission_code, v.sort_order, 1, 1, 1
FROM sys_menu p
JOIN (
  SELECT '车辆档案' AS menu_name, '/gcan/vehicles' AS path, 'gcan/vehicles/index' AS component, 'Package' AS icon, 'gcan:vehicle' AS permission_code, 1 AS sort_order
  UNION ALL SELECT '煤矿维护', '/gcan/mines', 'gcan/mines/index', 'Building2', 'gcan:mine', 2
  UNION ALL SELECT '车型管理', '/gcan/vehicle-types', 'gcan/vehicle-types/index', 'Package', 'gcan:vehicle-type', 3
  UNION ALL SELECT '故障码表维护', '/gcan/fault-profiles', 'gcan/fault-profiles/index', 'ListTree', 'gcan:fault-profile', 4
  UNION ALL SELECT '故障定义维护', '/gcan/fault-definitions', 'gcan/fault-definitions/index', 'ShieldHalf', 'gcan:fault-definition', 5
  UNION ALL SELECT '车辆状态', '/gcan/vehicle-can-state', 'gcan/vehicle-can-state/index', 'MonitorCog', 'gcan:vehicle-state', 6
  UNION ALL SELECT '十六进制转换', '/gcan/hex-dec', 'gcan/hex-dec/index', 'Database', 'gcan:hex-dec', 7
  UNION ALL SELECT '原始 CAN 数据', '/gcan/raw-frames', 'gcan/raw-frames/index', 'FileText', 'gcan:raw-frame', 8
) v ON p.permission_code = 'gcan' AND p.deleted = 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu existing WHERE existing.permission_code = v.permission_code AND existing.deleted = 0
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.permission_code IN (
  'gcan', 'gcan:vehicle', 'gcan:mine', 'gcan:vehicle-type', 'gcan:fault-profile',
  'gcan:fault-definition', 'gcan:vehicle-state', 'gcan:hex-dec', 'gcan:raw-frame'
)
AND m.deleted = 0
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id
);
