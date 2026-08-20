# GCAN 对外 API 对接文档

> 面向监控大屏、第三方系统和智能体的只读接口契约。
>
> 当前版本：`v1`  
> API 前缀：`/api/open/gcan/v1`  
> 鉴权：匿名访问，无需登录、Token 或 API Key  
> 更新日期：2026-07-16

## 1. 接入说明

### 1.1 Base URL

将以下地址替换为实际部署域名：

```text
{BASE_URL}/api/open/gcan/v1
```

常见访问方式：

| 环境 | Base URL 示例 |
| --- | --- |
| 浏览器/Nginx 同源 | `https://example.com` |
| gcan-api 本地直连 | `http://localhost:8081` |
| 开发前端代理 | 使用前端当前域名下的 `/api` 路径 |

生产环境推荐通过 Nginx 代理，保留完整路径。例如请求 `/api/open/gcan/v1/monitor/overview` 时，Nginx 转发给 gcan-api 后路径不能被截断。

### 1.2 通用规则

- 所有接口均为 `GET`，当前不提供任何写入、删除或状态变更接口。
- 请求和响应使用 JSON；时间使用 ISO-8601 的本地日期时间，例如 `2026-07-16T14:30:00`，服务时区为 `Asia/Shanghai`。
- 监控总览和当前车辆状态只返回启用车辆。原始帧接口面向诊断，当前快照可能包含尚未关联车辆的帧；历史数据只保存接收时已关联启用车辆的帧。
- 对外接口允许跨域读取；匿名接口不要发送 Cookie，也不需要 `Authorization` 请求头。
- `v1` 遵循只增不删原则。已有字段的含义和类型不应被调用方重新推断。
- 数值字段保持 JSON number 类型；协议未提供的车辆状态字段返回 `null`。
- `boxIdHex` 为两位大写十六进制编码，例如 `01`；`boxIdDec` 为对应十进制值，例如 `1`。

## 2. 统一响应格式

成功响应统一包装为：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `number` | 业务状态码，成功为 `200` |
| `message` | `string` | 状态说明 |
| `data` | `object\|array\|null` | 业务数据 |

常见失败码：

| `code` | 含义 |
| ---: | --- |
| `400` | 参数错误 |
| `401` | 未登录或 Token 失效；对外匿名接口正常情况下不会使用 |
| `403` | 无权限 |
| `404` | 数据不存在 |
| `500` | 系统错误 |

调用方应同时检查 HTTP 状态和响应体中的 `code`。历史查询的时间范围、分页参数等应在客户端预先校验，不要依赖服务端错误信息完成业务判断。

## 3. 接口总览

| 用途 | 方法和路径 |
| --- | --- |
| 监控总览 | `GET /monitor/overview` |
| GCAN 字典 | `GET /dictionary/{dictCode}` |
| 当前车辆 CAN 状态 | `GET /vehicle-can-state/current` |
| 当前原始 CAN 帧 | `GET /raw-frame/current` |
| 历史原始 CAN 帧分页 | `GET /raw-frame/history/page` |

## 4. 监控总览

### `GET /monitor/overview`

用于监控大屏首屏和轮询刷新。服务端已经完成启用车辆补齐、实时状态关联、煤矿分组和统计计算。

推荐大屏每 3 秒请求一次本接口；当前原始帧和历史帧应按需请求，不要随总览轮询请求。

响应 `data`：

```json
{
  "generatedAt": "2026-07-16T14:30:00",
  "lastUpdateAt": "2026-07-16T14:29:59",
  "statistics": {
    "vehicleTotal": 2,
    "onlineCount": 1,
    "offlineCount": 1,
    "noDataCount": 0,
    "unsupportedCount": 0,
    "faultVehicleCount": 1,
    "latestDataAt": "2026-07-16T14:29:59"
  },
  "mines": [
    {
      "mineId": "mine-a",
      "mineName": "一号煤矿",
      "sortOrder": null,
      "statistics": {},
      "vehicles": []
    }
  ]
}
```

### 总览对象字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `generatedAt` | `string\|null` | 本次总览生成时间 |
| `lastUpdateAt` | `string\|null` | 所有车辆中最近一次状态更新时间 |
| `statistics` | `object` | 全局统计 |
| `mines` | `array` | 按煤矿分组的车辆；排序稳定，不因状态变化动态重排 |
| `mines[].mineId` | `string\|null` | 煤矿稳定编码 |
| `mines[].mineName` | `string\|null` | 煤矿展示名称 |
| `mines[].sortOrder` | `number\|null` | 煤矿排序值；当前实现可能为空，不应作为唯一排序依据 |
| `mines[].statistics` | `object` | 当前煤矿统计 |
| `mines[].vehicles` | `array` | 当前煤矿下的车辆状态 |

### 统计字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `vehicleTotal` | `number` | 车辆总数 |
| `onlineCount` | `number` | `connectionStatus=ONLINE` 的车辆数 |
| `offlineCount` | `number` | `connectionStatus=OFFLINE` 的车辆数 |
| `noDataCount` | `number` | `connectionStatus=NO_DATA` 的车辆数 |
| `unsupportedCount` | `number` | `parseStatus=UNSUPPORTED` 的车辆数 |
| `faultVehicleCount` | `number` | 当前故障车辆数，只统计在线、解析支持且故障码非 `0` 的车辆 |
| `latestDataAt` | `string\|null` | 本分组或全局最近一条数据的接收时间 |

## 5. 当前车辆 CAN 状态

### `GET /vehicle-can-state/current`

返回启用车辆的当前状态平铺列表。返回的车辆对象字段与 `overview.data.mines[].vehicles[]` 相同。

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | :---: | --- |
| `vehicleName` | `string` | 否 | 按车辆名称包含匹配 |
| `mineId` | `string` | 否 | 按煤矿编码精确匹配 |
| `vehicleType` | `string` | 否 | 按车型编码匹配，不区分大小写 |
| `boxIdHex` | `string` | 否 | 按 GCAN 盒子十六进制编码匹配，例如 `01` 或 `0x01` |

示例：

```bash
curl "${BASE_URL}/api/open/gcan/v1/vehicle-can-state/current?mineId=mine-a&vehicleType=REN_19"
```

## 6. 当前原始 CAN 帧

### `GET /raw-frame/current`

返回内存中的当前原始 CAN 帧快照，不分页。接口只返回当前仍保留在快照中的帧；如果帧无法关联到车辆档案，车辆相关字段会为 `null`。

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | :---: | --- |
| `vehicleName` | `string` | 否 | 按车辆名称包含匹配 |
| `mineId` | `string` | 否 | 按煤矿编码精确匹配 |
| `vehicleType` | `string` | 否 | 按车型编码匹配，不区分大小写 |
| `boxIdHex` | `string` | 否 | 盒子 HEX 编码，支持 `0x` 前缀 |
| `canId` | `string` | 否 | CAN ID，服务端按大写形式匹配 |
| `format` | `string` | 否 | `HEX`（默认）、`BIN`/`BINARY`、`DEC`/`DECIMAL` |

响应 `data` 是数组，每项结构如下：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `vehicleId` | `number\|null` | 车辆档案 ID |
| `vehicleName` | `string\|null` | 车辆名称 |
| `mineId` | `string\|null` | 煤矿编码 |
| `vehicleType` | `string\|null` | 车型编码 |
| `boxIdHex` | `string` | 盒子 HEX 编码 |
| `boxIdDec` | `number` | 盒子十进制编码 |
| `canId` | `string` | CAN ID |
| `data` | `string[8]` | 8 个数据字节，格式由 `format` 决定 |
| `receivedAt` | `string\|null` | 接收时间 |

`format` 示例：

| `format` | `data` 中单个字节示例 |
| --- | --- |
| `HEX` | `0x0A` |
| `BIN` | `00001010` |
| `DECIMAL` | `10` |

## 7. 历史原始 CAN 帧分页

### `GET /raw-frame/history/page`

查询已入库的 CAN 历史数据，按 `receivedAt` 倒序返回。

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | :---: | ---: | --- |
| `receivedStart` | `string` | 是 | - | 查询起始时间，ISO-8601 本地时间 |
| `receivedEnd` | `string` | 是 | - | 查询结束时间，不能早于 `receivedStart` |
| `page` | `number` | 否 | `1` | 从 1 开始 |
| `pageSize` | `number` | 否 | `50` | 最大 `200` |
| `vehicleName` | `string` | 否 | - | 车辆名称包含匹配 |
| `mineId` | `string` | 否 | - | 煤矿编码精确匹配 |
| `vehicleType` | `string` | 否 | - | 车型编码匹配，不区分大小写 |
| `boxIdHex` | `string` | 否 | - | 盒子 HEX 编码 |
| `canId` | `string` | 否 | - | CAN ID |
| `format` | `string` | 否 | `HEX` | `HEX`、`BIN`/`BINARY`、`DEC`/`DECIMAL` |

限制：

- `receivedStart` 和 `receivedEnd` 必须同时提供。
- 时间范围不得超过 24 小时；建议调用方自行按 24 小时切分更长时间范围。
- `receivedEnd` 是包含边界的查询条件。
- 分页响应使用统一 `PageResult`：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "pageSize": 50
}
```

示例：

```bash
curl "${BASE_URL}/api/open/gcan/v1/raw-frame/history/page?receivedStart=2026-07-16T00:00:00&receivedEnd=2026-07-16T01:00:00&page=1&pageSize=50&format=HEX"
```

`records[]` 的字段与当前原始 CAN 帧接口一致。

## 8. GCAN 字典

### `GET /dictionary/{dictCode}`

返回编码对应的展示名称。第三方系统应保存并使用 `code`，展示时使用 `name`；不要把中文名称当作业务主键。

支持的 `dictCode`：

| `dictCode` | 用途 | 已知编码 |
| --- | --- | --- |
| `gcan_mine` | 煤矿 | 由系统配置，可能动态变化 |
| `gcan_vehicle_type` | 车型 | `REN_19`、`REN_19_B`、`LIAO_1_9T`、`LIAO_5T` |
| `gcan_vehicle_connection_status` | 车辆连接状态 | `ONLINE`、`OFFLINE`、`NO_DATA` |
| `gcan_vehicle_parse_status` | 车辆解析状态 | `SUPPORTED`、`UNSUPPORTED` |

响应 `data` 示例：

```json
[
  { "code": "ONLINE", "name": "在线", "sortOrder": null },
  { "code": "OFFLINE", "name": "离线", "sortOrder": null },
  { "code": "NO_DATA", "name": "暂无数据", "sortOrder": null }
]
```

`sortOrder` 当前缓存转换可能为 `null`，调用方应按返回顺序或自身规则展示。

## 9. 车辆状态对象字段

以下字段同时出现在总览车辆项和当前车辆状态接口中。

### 9.1 识别、连接和解析状态

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `vehicleId` | `number\|null` | 车辆档案 ID |
| `vehicleName` | `string\|null` | 车辆名称 |
| `mineId` | `string\|null` | 煤矿稳定编码 |
| `mineName` | `string\|null` | 煤矿展示名称 |
| `vehicleType` | `string\|null` | 车型稳定编码 |
| `vehicleTypeLabel` | `string\|null` | 车型展示名称 |
| `boxIdHex` | `string\|null` | GCAN 盒子两位大写 HEX 编码 |
| `boxIdDec` | `number\|null` | GCAN 盒子十进制编码 |
| `online` | `boolean\|null` | 兼容字段；优先使用 `connectionStatus` |
| `parseSupported` | `boolean\|null` | 是否存在车型协议解析器 |
| `parseMessage` | `string\|null` | 无数据或未支持解析时的说明 |
| `connectionStatus` | `string` | `ONLINE`、`OFFLINE`、`NO_DATA` |
| `connectionStatusLabel` | `string\|null` | 连接状态展示名称 |
| `parseStatus` | `string` | `SUPPORTED` 或 `UNSUPPORTED` |
| `parseStatusLabel` | `string\|null` | 解析状态展示名称 |
| `lastReceivedAt` | `string\|null` | 最近接收 CAN 数据时间 |
| `updateTime` | `string\|null` | 当前状态更新时间 |
| `supportedUnits` | `string[]` | 可能使用的单位：`V`、`A`、`℃`、`%`、`km/h`、`rpm`、`km` |

状态判断：

- `ONLINE`：最近一次数据在 10 秒内更新。
- `OFFLINE`：曾经收到数据，但最近一次数据距当前超过 10 秒。
- `NO_DATA`：尚未收到该车辆数据。
- `UNSUPPORTED`：车辆有数据，但当前车型没有可用协议解析器。
- `faultVehicleCount` 不包含离线车辆，即使离线车辆保留了最后一次故障信息。

### 9.2 故障信息 `fault`

`fault` 在无数据、未支持解析或没有有效故障码时为 `null`。故障码 `"0"` 表示无故障。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `active` | `boolean` | 故障码是否非 `0` |
| `stale` | `boolean` | 是否为离线车辆保留的旧故障 |
| `configured` | `boolean` | 车辆是否关联故障码表 |
| `matched` | `boolean` | 故障码是否匹配到故障定义 |
| `code` | `string` | 故障码；按车辆关联故障码表解释 |
| `levelCode` / `levelName` | `string\|null` | 故障等级编码/名称 |
| `name` | `string\|null` | 故障名称 |
| `definition` / `description` | `string\|null` | 故障定义/描述 |
| `analysis` | `string\|null` | 原因分析 |
| `symptom` | `string\|null` | 故障现象 |
| `recovery` | `string\|null` | 恢复建议 |
| `clear` | `string\|null` | 清除条件或清除说明 |
| `handlingAdvice` / `suggestion` | `string\|null` | 处理建议；两字段当前内容一致 |

### 9.3 车辆 CAN 状态值

除特别说明外，表中数值字段为 JSON number；协议未上报对应 CAN 数据时为 `null`。单位由项目契约定义，不要根据字段名猜测单位。

| 字段 | 类型 | 单位 | 说明 |
| --- | --- | --- | --- |
| `highVoltage` | `number\|null` | `V` | 最高单体电压 |
| `lowVoltage` | `number\|null` | `V` | 最低单体电压 |
| `highTemperature` | `number\|null` | `℃` | 最高电池模块温度 |
| `lowTemperature` | `number\|null` | `℃` | 最低电池模块温度 |
| `motorControllerTemperature` | `number\|null` | `℃` | 电机控制器温度 |
| `motorTemperature` | `number\|null` | `℃` | 电机温度 |
| `insulationState` | `string\|null` | - | 绝缘状态，协议原始值通常为 `0`/`1` 字符串 |
| `startBatteryVoltage` | `number\|null` | `V` | 启动电池电压 |
| `rotarySpeed` | `number\|null` | `rpm` | 旋转速度 |
| `faultState` | `string\|null` | - | 故障码，`0` 表示无故障 |
| `throttleOpening` | `string\|null` | `%` | 油门开度；当前 Java 契约为字符串 |
| `batteryPercentage` | `number\|null` | `%` | 电池电量 |
| `handbrake` | `string\|null` | - | 手刹状态，协议原始值通常为 `0`/`1` 字符串 |
| `batteryVoltage` | `number\|null` | `V` | 电池电压 |
| `batteryElectric` | `number\|null` | `A` | 电池电流 |
| `speed` | `number\|null` | `km/h` | 车速 |
| `totalMileage` | `number\|null` | `km` | 总里程 |
| `runState` | `string\|null` | - | 运行状态原始编码 |
| `gear` | `string\|null` | - | 档位原始编码 |
| `lifecycle` | `string\|null` | - | 生命周期状态，协议原始值通常为 `0`/`1` 字符串 |
| `brakePedalOpening` | `number\|null` | `%` | 制动踏板开度 |
| `motorACCurrent` | `number\|null` | `A` | 电机交流电流 |
| `driveActiveStatus` | `string\|null` | - | 驱动激活状态 |
| `brakeActiveStatus` | `string\|null` | - | 制动激活状态 |
| `hillStartAssistStatus` | `string\|null` | - | 坡道起步辅助状态 |
| `creepModeStatus` | `string\|null` | - | 蠕行模式状态 |
| `prechargeContactorCmd` | `string\|null` | - | 预充接触器指令 |
| `mainContactorCmd` | `string\|null` | - | 主接触器指令 |
| `motorControllerDCVoltage` | `number\|null` | `V` | 电机控制器直流电压 |
| `accSignal` | `string\|null` | - | ACC 信号 |
| `onSignal` | `string\|null` | - | ON 信号 |
| `driveSignal` | `string\|null` | - | 前进信号 |
| `reverseSignal` | `string\|null` | - | 倒车信号 |
| `leftTurnLight` | `string\|null` | - | 左转向灯状态 |
| `rightTurnLight` | `string\|null` | - | 右转向灯状态 |
| `highBeam` | `string\|null` | - | 远光灯状态 |
| `lowBeam` | `string\|null` | - | 近光灯状态 |
| `smallLight` | `string\|null` | - | 小灯状态 |
| `door1Open` | `string\|null` | - | 车门 1 状态 |
| `door2Open` | `string\|null` | - | 车门 2 状态 |
| `door3Open` | `string\|null` | - | 车门 3 状态 |
| `mcuTemperature` | `number\|null` | `℃` | MCU 温度 |
| `methaneDetectionFailure` | `string\|null` | - | 甲烷检测故障状态 |
| `smokeDetectionFailure` | `string\|null` | - | 烟雾检测故障状态 |
| `readyState` | `string\|null` | - | Ready 状态 |

## 10. 智能体对接建议

推荐将以下规则写入智能体的工具或系统提示中：

1. 首次调用 `GET /monitor/overview`，使用 `mines[].vehicles[]` 构建监控上下文；煤矿、车型和状态同时保留编码与名称。
2. 轮询只调用总览接口，默认间隔 3 秒；不要为每辆车分别请求当前状态。
3. 判断在线、离线和暂无数据时使用 `connectionStatus`；判断是否可解释 CAN 状态时使用 `parseStatus`。
4. 需要原始报文时调用 `/raw-frame/current`，需要追溯时调用 `/raw-frame/history/page`；历史查询必须带时间范围并按页读取。
5. 解析故障时先检查 `fault == null`、`fault.active`、`fault.matched` 和 `fault.stale`，不要仅根据 `fault.code` 生成确定性结论。
6. 不要把 `null` 转换成 `0`、空字符串或“正常”；`null` 表示当前协议没有提供该字段值。
7. 不要把 `throttleOpening` 等字符串数值统一强制转换为 number，除非业务明确允许类型转换。
8. 所有用户输入的时间、页码和 `pageSize` 应在调用前校验：时间范围不超过 24 小时，`page >= 1`，`1 <= pageSize <= 200`。

## 11. 最小调用示例

```bash
# 1. 读取总览
curl "${BASE_URL}/api/open/gcan/v1/monitor/overview"

# 2. 查询某个盒子的当前原始帧
curl "${BASE_URL}/api/open/gcan/v1/raw-frame/current?boxIdHex=01&format=HEX"

# 3. 查询一小时历史数据
curl "${BASE_URL}/api/open/gcan/v1/raw-frame/history/page?receivedStart=2026-07-16T14:00:00&receivedEnd=2026-07-16T15:00:00&page=1&pageSize=50"

# 4. 查询车辆连接状态字典
curl "${BASE_URL}/api/open/gcan/v1/dictionary/gcan_vehicle_connection_status"
```

## 12. 安全和兼容性边界

- 当前接口匿名可读，任何能访问 API 地址的调用方都可以读取公开的车辆监控数据、盒子标识和原始 CAN 帧。
- 当前没有 API Key、访问限流和调用方级别的数据权限控制；部署时应通过网络边界、Nginx 和访问日志控制暴露范围。
- 后续如果增加客户端凭证、限流或字段脱敏，应在本文件中更新，并保持 `v1` 既有字段语义兼容；破坏性变更进入新版本前缀。
