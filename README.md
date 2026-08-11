# Mekanism AE Bridge

一个面向 Minecraft NeoForge 1.21.1 的开发中模组，目标是让 Mekanism 加工机器直接作为 AE2 处理样板提供器工作，并逐步实现类似 GTNH 样板总成的任务缓存与调度体验。

> 当前仍处于早期开发阶段，存档格式、GUI 和加工调度可能继续调整，请勿直接用于重要存档。

## 当前实现

- ME 富集仓、ME 粉碎机与 ME 冶金灌注机，可被 AE2 网络识别为处理样板提供器。
- 顶部 9 个编码处理样板槽。
- NeoForge FE 与 Mekanism Strict Energy 双接口供能。
- ME 加速卡、并行卡和能量卡，每种最多安装 8 张。
- 当前配方持续加工，等待任务按编码样板身份隔离。
- GTNH 风格的大容量预投递缓存；并行卡仅控制加工倍率，不限制 AE2 发配深度。
- 当前输入、预计产物、网络状态、队列、进度和能量 GUI。
- 暂停新任务以及将内部缓存资源返还至 ME 网络的按钮。
- 方块实体状态、能量、升级、样板和等待任务的 NBT 持久化。
- 冶金灌注任务把物品与 Applied Mekanistics 化学品存入同一样板专属账本，不同灌注类型不会串料。
- Jade 悬浮信息显示在线状态、缓冲操作、当前任务、化学品、实际速度/并行倍率和精确能量。
- 三台 ME 机器注册为 Mekanism 现有 JEI 配方分类的加工设备；可直接查看各自能处理的配方。
- 开发环境提供 JEI 与 AE2 JEI Integration，可从 JEI 的 `+` 按钮直接填充 AE2 处理样板。
- 双层平衡配置：整合包作者可设置全局默认值，每个存档也可选择启用自己的覆盖值。

## 计划方向

- 在通用机器基础上增加更多 Mekanism 物品加工机，以及带气体、颜料、浆液等化学品输入的 ME 机器。
- 继续完善资源返还、方块拆除保护和异常恢复。

更完整的开发记录见 [PLAN.zh-CN.md](PLAN.zh-CN.md)。

## 开发环境

| 组件 | 版本 |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.248` |
| Java | `21` |
| Mekanism | `10.7.19.85` |
| Applied Energistics 2 | `19.2.17` |
| Applied Mekanistics | `1.6.3` |
| GuideME | `21.1.17` |
| Just Enough Items | `19.27.0.335` |
| AE2 JEI Integration | `1.2.1` |
| Jade | `15.10.5` |

第三方模组 JAR 不提交到仓库。请按 [兼容性清单](docs/compatibility.md) 下载对应版本，并放入本地 `libs/` 目录。

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

离线构建可使用：

```powershell
.\gradlew.bat build --offline
```

## 配置

在模组列表中选择 Mekanism AE Bridge 并点击“配置”，或直接编辑以下文件：

- 全局默认：`config/mekanismae-server.toml`。
- 每存档覆盖：把全局文件复制到 `saves/<存档>/serverconfig/mekanismae-server.toml`，再修改该副本。NeoForge 只在同名副本存在时启用该存档的覆盖值。

可调整是否消耗 AE2 频道、待机 AE 功耗、红石暂停、能量容量与输入速率、每次加工耗能、基础加工时间、最大预投递缓存，以及 0～8 张加速卡/并行卡的完整倍率曲线。三台机器默认继承统一的 `machines.defaults`；把对应机器的 `use_defaults` 改为 `false` 后，可以单独设置其能量、耗能、加工时间和缓存上限。

这些参数都在世界重新载入时应用。没有存档副本时，所有存档直接沿用全局文件；这也是默认状态。降低缓存上限不会删除已接收任务，但机器在低于新上限前不会再接收新任务；降低能量容量时，超出新容量的已存能量会被截断。

## 参与开发

欢迎通过 Issue 讨论设计，或从新分支提交 Pull Request。当前尚未选择开源许可证；在许可证确定前，请先将代码使用范围限定为本仓库的协作开发。
