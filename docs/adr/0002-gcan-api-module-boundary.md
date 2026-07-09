# gcan-api 模块边界

Status: accepted

`gcan-api` 作为 `base-api/` 下与 `system-api` 平级的独立可启动 API 模块，负责 GCAN 盒子接入、CAN 帧解码、车辆 CAN 状态解析、当前状态查询和 CAN 历史数据入库。第一阶段由 `gcan-api` 自己拥有最小车辆档案和盒子绑定数据，避免依赖尚未成型的车辆服务；车辆档案需要提供最小 CRUD，盒子 ID 在车辆档案中保持唯一。GCAN 盒子 ID 的标准形态是两位大写 HEX，接口和前端字段使用 `boxIdHex` 与 `boxIdDec` 明确区分十六进制和十进制，前端需要同时展示二者以避免人工混淆。停用车辆不参与车辆 CAN 状态解析、实时快照刷新和 CAN 历史数据写入，但 TCP 接入层仍接收其 GCAN 盒子的原始 CAN 帧，以便在诊断接口中查看。车辆类型标准值先沿用 `REN_19`、`REN_19_B`、`LIAO_1_9T`、`LIAO_5T`，协议解析器只做迁移适配，不在第一阶段重构为规则配置。`system-api` 继续只负责认证签发和系统管理，`gcan-api` 可以依赖 `base-common`、共享 starter 和显式契约，但不得依赖 `system-api` 的实现代码。`gcan-api` 的 HTTP API 长期应接入 Sa-Token 登录态；第一阶段为测试和联调可临时放行 API 接口，TCP 接入本身不使用 Sa-Token 鉴权。
