# Mekanism AE Bridge 开发计划

## 1. 项目定位

这是一个 Mekanism、Applied Energistics 2 和 Applied Mekanistics 之间的附属模组。

项目目录：`E:\mods\ae2fushu`

暂定项目名称：`Mekanism AE Bridge`

暂定 Mod ID：`mekanismae`

目标是把 Mekanism 中需要主动处理配方的机器，包装成可以直接连接 AE2 线缆、参与 ME 自动合成的单方块机器。

### 当前进度

- [x] 锁定 Minecraft 1.21.1 和依赖版本。
- [x] 建立 NeoForge 最小工程并通过完整构建。
- [x] 注册第一个 ME 富集仓方块和物品原型。
- [x] 为原型接入 AE2 单频道网络节点。
- [x] 修复 AE2 节点主机 Capability 注册，并移除递归邻居通知补丁。
- [x] 通过 `compileJava` 和完整 `build`，生成可用 jar。
- [x] 在实际存档中完成线缆连接、重进存档和网络识别验收。
- [x] 接入处理样板和 Mekanism 富集配方。
- [x] 接入 Jade 状态显示和 JEI 到 AE2 处理样板的传输兼容。

## 2. 设计目标

- 每个 MEK 单方块机器直接连接 AE2 网络。
- 每个机器网络节点占用 1 个 AE2 频道。
- 机器只接受能源和红石信号。
- 默认不开放物品、流体、气体或化学品的外部输入输出接口。
- 机器内部使用容量很大的持久化资源缓存。
- 玩家不能直接从内部缓存取出物品，只能通过 GUI 将资源返还到 AE 网络。
- 保留 Mekanism 原有的红石控制习惯。
- 复用 Mekanism 原生配方和处理规则，不复制一套平行配方系统。

## 3. 依赖和版本策略

### 当前锁定版本

- Minecraft：`1.21.1`
- NeoForge：`21.1.248`
- Java：`21`
- Mekanism：`10.7.19.85`
- Applied Energistics 2：`19.2.17`
- Applied Mekanistics：`1.6.3`
- GuideME：`21.1.17`（AE2 的必需依赖）

正式开发前先锁定以下依赖的共同支持版本：

- Minecraft
- NeoForge
- Mekanism
- Applied Energistics 2
- Applied Mekanistics

当前 `copper-furnace` 使用的 Minecraft 26.1.1 / NeoForge 26.1.1.15-beta 只能作为工程模板参考，不能直接假定 AE2、Mekanism 和 Applied Mekanistics 都支持该版本。

版本兼容性验证需要确认：

1. 三个依赖的准确 Mod ID、版本范围和加载侧。
2. AE2 的网络节点、处理样板和合成机器 API。
3. Applied Mekanistics 的流体、气体和化学品桥接 API。
4. Mekanism 配方类型、机器处理器和工厂等级接口。

如果没有共同支持的 26.1.1 版本，应切换到三个依赖共同支持的版本，并在项目文档中固定版本矩阵。

## 4. 第一阶段支持范围

第一阶段先完成一个完整的物品配方闭环：

1. 富集仓 / 富集工厂。
2. 粉碎机 / 粉碎工厂。

建议先用富集仓完成垂直切片，再抽象成通用机器控制器。

暂时排除：

- 中子活化室等偏被动或特殊机制机器。
- 发电机和能源设备。
- 多方块结构。
- 依赖世界环境交互的机器。

压射仓以及其他需要流体、气体或化学品的机器放到第二阶段，等 Applied Mekanistics 的资源接口验证完成后再接入。

## 5. 总体架构

### 5.1 通用机器控制器

所有 MEK 单方块机器共用一套控制器逻辑，每种机器只注册一个机器配置：

- 机器类型和注册名。
- 对应的 Mekanism 配方类型。
- 输入、输出资源类型。
- 基础处理时间。
- 基础能耗。
- 工厂等级和并行数量。
- GUI 显示名称和图标。

外观和方块注册名可以按机器类型区分，内部逻辑使用同一个通用控制器，避免为每个机器复制处理代码。

### 5.2 AE2 网络节点

机器放置后创建一个受管理的 AE2 网络节点：

- 直接连接 AE2 线缆。
- 一个机器只创建一个网络节点，因此默认只占用一个频道。
- 不额外放置独立的 Pattern Provider 方块。
- 连接、断开、区块卸载和重新加载时正确注册和注销节点。
- GUI 显示网络是否连接以及频道是否可用。

具体 API 名称以锁定版本的 AE2 API 为准，优先使用 AE2 / Applied Mekanistics 原生的 processing-pattern 和 crafting-machine 接口。

### 5.3 能源和红石

- 暴露 Mekanism 或 NeoForge 兼容的能源能力。
- 默认不暴露物品、流体、气体和化学品能力。
- 读取附近红石信号。
- 保留忽略红石、有信号运行、无信号运行、脉冲等控制模式。

## 6. AE 合成流程

```mermaid
flowchart LR
    A[AE2 合成请求] --> B[读取机器中的处理样板]
    B --> C[验证样板和机器类型]
    C --> D[从 ME 网络事务性提取资源]
    D --> E[写入内部资源缓存]
    E --> F[调用 Mekanism 原生配方处理]
    F --> G[产物进入输出缓存]
    G --> H[提交到 AE2 网络或完成合成任务]
```

具体行为：

1. GUI 中放入一个有效的 AE2 处理样板。
2. 机器验证样板是否属于当前机器类型。
3. AE2 合成服务发现该样板后，将机器作为可调度的处理器。
4. 下单时，从 ME 网络提取输入资源并写入内部缓存。
5. 机器按 Mekanism 原生配方推进处理进度。
6. 产物通过 AE2 事务接口提交给网络和对应的合成任务。
7. 网络断开或存储空间不足时暂停，不丢失资源。

输入资源需要使用任务账本进行预留，避免多个合成任务互相抢夺同一批资源。

## 7. 内部缓存和任务规则

- 缓存按物品、流体、气体和化学品资源 ID 分组。
- 数量使用足够大的整数类型，但缓存仍然有可配置上限，避免无限增长和 NBT 膨胀。
- 任务队列容量可配置。
- 当前正在处理的配方不能被新的样板覆盖。
- 有活动任务时不允许直接取出样板，或要求先停止并完成排空。
- 取消任务时返还尚未消耗的资源。
- 已经完成处理的输入不能回滚，相关产物仍按正常流程返回网络。

## 8. GUI 设计

GUI 不显示传统机器的物品槽、流体槽或气体槽，只显示状态信息：

- 一个处理样板槽。
- 当前机器名称和运行状态。
- 当前配方的输入、输出图标和数量摘要。
- 处理进度条。
- 能量存量和最大能量。
- 红石控制模式和当前红石状态。
- AE2 网络连接和频道状态。
- 内部缓存总量和任务队列数量。
- “将内部资源全部返回 ME 网络”按钮。

返还按钮进入排空模式：

1. 停止接受新的合成输入。
2. 让当前处理周期完成。
3. 将产物和未使用资源返回 ME 网络。
4. 网络暂时不可用时保留排空状态并持续重试。

玩家只能交互样板和控制按钮，不能通过 GUI 直接拿出内部材料。

## 9. 外部输入输出配置

默认配置为 ME 网络专用：

```text
external_io = false
```

后续可以增加服务器配置或机器级别配置：

- 是否允许物品外部输入。
- 是否允许流体外部输入。
- 是否允许气体或化学品外部输入。
- 是否允许外部输出。

即使开启外部接口，也必须把外部资源写入同一个内部任务账本，不能绕过 AE2 合成任务直接修改缓存。

## 10. 断线、破坏和持久化规则

- AE2 网络断开时，机器暂停新任务并保留现有缓存。
- AE2 网络重新连接后继续处理和提交产物。
- 区块卸载、服务器重启后恢复样板、缓存、能量、进度、红石设置和任务账本。
- 网络存储满时，产物停留在输出缓存中，不强行丢弃。
- 默认情况下，缓存非空或有活动任务时禁止破坏机器。
- 有网络时可以先执行排空；没有网络时显示原因并保留机器。
- 数据结构带版本号，方便以后迁移缓存格式。

## 11. 工厂等级

不为每个工厂等级复制一套机器代码。建议使用统一的并行参数：

- 基础：1 路处理。
- 高级：3 路处理。
- 精英：5 路处理。
- 终极：7 路处理。
- 如果 Mekanism 当前版本使用不同数值，以实际机器行为为准。

第一版先完成基础等级，确认合成、缓存和输出流程稳定后，再加入工厂等级或升级物品。

## 12. 开发阶段

### 阶段一：版本和 API 验证

- 锁定共同支持版本。
- 验证 AE2 频道、处理样板和合成机器接口。
- 验证 Mekanism 原生配方调用方式。
- 验证 Applied Mekanistics 的化学品桥接方式。

### 阶段二：工程骨架

- 创建 NeoForge 工程。
- 配置三个必需依赖。
- 建立方块、方块实体、菜单和资源注册结构。
- 建立中英文语言文件和基础测试环境。

### 阶段三：富集仓垂直切片

- 实现单频道 AE2 节点。
- 实现处理样板验证。
- 实现物品输入缓存和任务预留。
- 调用富集仓原生配方。
- 将产物返回 AE2 网络。
- 完成能源、红石和 GUI。

### 阶段四：通用化和粉碎机

- 抽象机器配置和配方适配器。
- 接入粉碎机和粉碎工厂。
- 增加工厂并行参数。
- 增加断线恢复、排空和服务器重启测试。

### 阶段五：流体和化学品

- 接入 Applied Mekanistics 的化学品资源。
- 接入流体和气体配方。
- 实现压射仓。
- 补充多资源输入、多资源输出和事务失败重试。

### 阶段六：扩展和发布

- 增加更多主动处理机器。
- 增加配置文件和权限控制。
- 增加 JEI/REI 显示支持。
- 完成多人服务器、性能和数据迁移测试。

## 13. 测试清单

- 单方块连接 AE2 后占用一个频道。
- 无频道、网络断开时显示正确状态。
- 有效样板可以参与 AE2 合成。
- 无效样板不能被提交。
- 资源能正确进入内部缓存。
- 配方能按 Mekanism 原生速度和能耗推进。
- 产物能返回 AE2 网络并完成合成任务。
- 红石信号能暂停和恢复机器。
- 玩家、漏斗和普通管道不能直接取出资源。
- 排空操作不会丢失或复制资源。
- 网络断开、区块卸载、服务器重启后数据完整。
- 网络存储满时不会丢失产物。
- 多人同时打开 GUI 时服务端状态保持一致。
- 取消合成任务时未消耗资源能正确返还。

## 14. 第一版默认决策

- 单机器一个样板槽。
- 默认只支持物品配方。
- 默认只允许 ME 网络输入输出。
- 默认基础处理速度和基础并行数。
- 默认不支持中子活化室、发电机和多方块机器。
- 压射仓等流体/化学品机器放在第一版闭环完成后开发。

## 15. AI 交接记录（2026-08-05）

本节记录本轮实际排查、修改和验证结果。切换 AI 后应先阅读本节，再继续开发，避免重复从 NBT 或节点创建顺序开始猜测。

### 15.1 当前项目状态

- 项目目录：`E:\mods\ae2fushu`
- 目标版本：Minecraft `1.21.1`、NeoForge `21.1.248`、Mekanism `10.7.19.85`、AE2 `19.2.17`、Applied Mekanistics `1.6.3`、GuideME `21.1.17`。
- 当前核心原型：`MeEnrichmentChamberBlock` / `MeEnrichmentChamberBlockEntity`。
- 项目不是 Git 仓库；本轮没有使用回滚或覆盖用户原有修改。

### 15.2 本轮已完成

1. 检查了本地方块、方块实体、Capability 注册、AE2 节点生命周期和运行日志。
2. 对照了 AE2 `neoforge/v19.2.17` 的 API 手册和源码。
3. 修复了 AE2 节点无法在反向扫描中发现的问题。
4. 删除了会触发邻居更新递归的临时补丁。
5. 完成 Java 编译和完整 Gradle 构建。

### 15.3 已确认的根因

原代码在 `MekanismAeMod.java` 中只注册了 NeoForge 能源和 Mekanism 能源 Capability，没有注册 AE2 的 `AECapabilities.IN_WORLD_GRID_NODE_HOST`。

AE2 1.21.1 的 `GridHelper.getNodeHost(Level, BlockPos)` 通过这个 Capability 查找相邻的 `IInWorldGridNodeHost`。仅仅继承 `AENetworkedBlockEntity`、实现接口还不够；第三方方块实体必须用 `RegisterCapabilitiesEvent.registerBlockEntity` 注册自己的 BlockEntityType。

这解释了全部原始现象：

- 先放线缆再放机器：机器创建节点时可以主动找到已有线缆，因此看起来正常。
- 先放机器再放线缆：线缆反向查询机器时查不到 Capability，因此无法连接。
- 重进存档：机器和线缆的就绪顺序使第一次扫描失败；之后另一端仍查不到机器，导致稳定断开。

持久化不是主因。`MeEnrichmentChamberBlockEntity.saveAdditional` 和 `loadTag` 都调用了父类；AE2 的 `AENetworkedBlockEntity` 已经负责 managed node 的 NBT、首次就绪创建、区块卸载销毁和移除销毁。

### 15.4 已完成的代码修改

#### `MekanismAeMod.java`

已增加：

```java
import appeng.api.AECapabilities;

event.registerBlockEntity(
        AECapabilities.IN_WORLD_GRID_NODE_HOST,
        ModBlockEntities.ME_ENRICHMENT_CHAMBER.get(),
        (blockEntity, ignored) -> blockEntity);
```

#### `MeEnrichmentChamberBlockEntity.java`

已删除：

- 自定义 `onReady()`。
- `notifyGridNeighbors()`。
- `level.updateNeighborsAt(...)` 以及六方向循环。

现在由 AE2 父类和节点自身负责创建、连接、加载和销毁。

#### `MeEnrichmentChamberBlock.java`

已删除调用 `notifyGridNeighbors()` 的 `neighborChanged()` 覆盖方法。

### 15.5 遇到的问题、原因和解决方式

- 之前反复尝试 NBT、`onReady()` 和手动邻居刷新，但没有解决“放置顺序敏感”。原因是遗漏了 NeoForge Capability 注册这一层；放置顺序症状实际上是典型的单向节点发现失败。
- 手动邻居刷新不是正确修复，反而造成 `neighborChanged -> notifyGridNeighbors -> updateNeighborsAt` 递归。旧日志 `run/logs/latest.log` 中出现了大量 `Too many chained neighbor updates`。相关代码已删除。
- 第一次 Gradle 命令的外层等待超时，但后台编译随后完成；随后使用 `--offline --console=plain` 重新执行，得到明确成功结果。
- 旧 `latest.log` 是修复前日志，不能拿它判断修复后的运行结果。需要用新启动产生的新日志验证。

### 15.6 构建和静态验证结果

- `./gradlew.bat compileJava --no-daemon --offline --console=plain`：`BUILD SUCCESSFUL`。
- `./gradlew.bat build --no-daemon --offline --console=plain`：`BUILD SUCCESSFUL`。
- 构建产物：`E:\mods\ae2fushu\build\libs\mekanismae-0.1.0.jar`。
- `javap` 已确认生成的字节码包含 `AECapabilities.IN_WORLD_GRID_NODE_HOST` 的注册调用。
- 源码搜索确认不再存在 `notifyGridNeighbors`、`updateNeighborsAt` 或该 `neighborChanged` 补丁。
- 当前未完成真实游戏存档中的自动化验收，因此不能把静态构建等同于最终运行验收。

### 15.7 下一步必须做的验收

使用新构建的 jar 启动实际实例，至少验证以下四组场景：

1. 先放线缆，再放机器；保存、退出、重新进入。
2. 先放机器，再放线缆；保存、退出、重新进入。
3. 跨区块加载/卸载后重新观察连接状态。
4. 新日志中不再出现 `Too many chained neighbor updates`。

如果仍然无法连接，优先检查服务端首 tick 后：

- `level.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, worldPosition, null)` 是否返回当前方块实体。
- `getMainNode().isReady()` 是否为 `true`。
- `getMainNode().getNode()` 是否为空。
- 机器朝向对应的 exposed sides 是否正确。

不要首先重新添加手工 `neighborChanged`、`updateNeighborsAt`、`onReady/create/destroy` 或重复的 NBT 代码。

### 15.8 官方参考

- [AE2 19.2.17 API 手册：Managed Grid Nodes](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/neoforge/v19.2.17/API.md#L100)
- [`IInWorldGridNodeHost`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/neoforge/v19.2.17/src/main/java/appeng/api/networking/IInWorldGridNodeHost.java#L34)
- [`GridHelper.getNodeHost`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/neoforge/v19.2.17/src/main/java/appeng/api/networking/GridHelper.java#L132)
- [AE2 Capability 注册示例](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/neoforge/v19.2.17/src/main/java/appeng/init/InitCapabilityProviders.java#L85)
- [AE2 `AENetworkedBlockEntity` 生命周期](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/neoforge/v19.2.17/src/main/java/appeng/blockentity/grid/AENetworkedBlockEntity.java#L39)

### 15.9 GUI 与状态同步重构（2026-08-05）

实际存档已确认机器能够接入 AE2 网络、被网络工具识别，并在退出后重新进入存档时恢复连接。

本轮定位并修复了 GUI 始终显示“离线”和 `0 FE` 的同步问题：服务端菜单使用方块实体提供的 `ContainerData`，但客户端菜单也创建了同一个包装对象；网络数据包在客户端调用 `ContainerData.set(...)` 时，该实现为空操作，导致服务端同步来的能量、最大能量、网络状态、队列和进度全部被丢弃。客户端现在改用 `SimpleContainerData` 接收同步值，服务端仍读取真实方块实体状态。

GUI 已按 Mekanism 机器风格重构：

- 左侧增加 ME 任务接入开关；关闭后不再公开样板或接受新任务，但不会销毁 AE2 节点和已有任务。
- 左侧增加“返还至 ME”按钮和带精确数值提示的 FE 能量条。
- 右侧增加 6 格升级抽屉；支持 ME 加速卡、ME 并行卡和 ME 能量卡，每种最多 8 张。
- 升级面板悬停提示每类卡的当前数量和上限。
- 主面板显示处理样板、网络状态、队列、能量和进度；移除了本机不需要的分面输入输出配置。
- 外部能量插入会将方块实体标记为已更改，确保能量持久化；处理循环改用内部专用耗能方法，外部接口仍保持只允许输入，避免出现“达到能量门槛但加工不扣能量”的问题。

注意：AE2 线缆只为 AE2 网络节点提供 AE 能量，不会自动填充机器的内部 FE 缓存。处理能量仍需通过 NeoForge FE 或 Mekanism 能量线缆从外部输入。如果修复后 GUI 在线但 FE 仍为 0，应先连接外部能源，再根据能量条的精确提示判断 Capability 是否收到了能量。

静态验证结果：

- `compileJava --offline --no-daemon`：`BUILD SUCCESSFUL`。
- `build --offline --no-daemon`：`BUILD SUCCESSFUL`。
- 新客户端已启动并完成 Mekanism、AE2、Applied Mekanistics 和 `mekanismae` 资源加载。

仍需实机验证：

1. GUI 在线状态与 AE2 网络工具一致。
2. Mekanism 通用线缆或其他 FE 源能让内部能量条和精确数值实时增长。
3. 三种升级卡均可放入任意升级槽，每种总数不能超过 8。
4. ME 任务接入开关会停止/恢复公开处理样板。
5. “返还至 ME”在网络在线、离线和存储已满时都不会丢失资源。

### 15.10 通用能量兼容与多样板 GUI（2026-08-05）

实机测试确认 Mekanism 通用线缆连接机器任意面后，内部 FE 仍保持为 0。对照 NeoForge 1.21.1 官方 Capability 文档和本地 Mekanism 10.7.19.85 实现后，确认：

- 跨模组通用能量接口是 NeoForge `IEnergyStorage` / `Capabilities.EnergyStorage.BLOCK`，其设计源自 TeamCoFH RedstoneFlux API。
- 本项目已经正确注册该 Capability，并对所有 `Direction` 返回可接收能量的存储对象。
- Mekanism 通用线缆会优先查询 `IStrictEnergyHandler`，再兼容包装 NeoForge FE 等接口。
- `IStrictEnergyHandler.insertEnergy(...)` 返回的是“未插入的剩余量”，而不是“成功插入量”。旧实现直接返回 `EnergyStorage.receiveEnergy(...)` 的成功插入量，导致通用线缆将完全成功的模拟误判为完全失败。

修复方式：Strict Energy 插入现在返回 `amount - accepted`；非法容器则返回全部 `amount`。NeoForge FE Capability 继续保留，因此 Mekanism 通用线缆和其他 FE/RF 线缆可同时兼容。

本轮同时完成 GUI 和样板存储扩展：

- 处理样板从 1 槽扩展为顶部一排 9 槽，AE2 crafting provider 会公开全部有效富集配方样板。
- 旧存档中的单个 `Pattern` NBT 会自动迁移到新版 `Patterns` 列表的第 0 槽。
- 中间增加只读的“当前输入”和“预计/待返回产物”显示槽，由菜单同步真实任务状态，玩家不能放入或取出物品。
- 主界面宽度增加，状态框扩展；能量概要改为紧凑格式，完整精确 FE 数值仍在悬停提示中显示。
- 左下能量侧栏改为接近方形的 Mekanism 风格控件。

构建验证：

- `compileJava --offline --no-daemon`：`BUILD SUCCESSFUL`。
- `build --offline --no-daemon`：`BUILD SUCCESSFUL`。

实机重点验收：

1. 使用现有 Mekanism 通用线缆重新加载存档后，内部 FE 应立即增长。
2. 从上下左右任一面连接 NeoForge FE 或 Mekanism 能量源均可输入，外部不能抽取机器能量。
3. 9 个样板槽可分别放入有效处理样板，旧存档原样板仍保留。
4. 合成任务执行时，中间输入和产物显示与真实任务一致，且无法手动交互。

### 15.11 并行调度、样板任务隔离与 GUI 二次调整（2026-08-05）

实机已确认通用线缆供能、基础加工、加速卡和能量卡均正常。并行卡没有明显效果的原因不是升级数量未读取，而是 AE2 经常按单次配方逐份调用 `pushPattern(...)`；旧实现一接到首份任务就令机器全局忙碌，后续份数无法进入内部缓存，因此 `1 + 并行卡数` 的批量循环永远只有一份可处理。红石和钻石任务轮流加工也是同一单任务模型造成的：每完成一份机器就重新交给 AE2 选择下一配方。

加工调度已改为按编码样板身份隔离的任务队列：

- 每份任务保存自己的编码样板、输入物品、单次消耗量和剩余操作数；不同样板的资源不会合并。
- 与当前活动样板相同的新任务会继续追加到活动任务，同一配方只要内部仍有材料就不会切换。
- 其他样板进入隔离等待队列；活动任务完全耗尽并返还产物后，才按队列顺序切换下一配方。
- 并行卡使机器在内部累计达到 `1 + 并行卡数` 份操作前继续允许 AE2 投递，随后在同一个加工周期内最多完成对应份数。这样即使 AE2 每次只投递一份，并行卡也能实际提升吞吐。
- 活动任务身份和等待队列均写入方块实体 NBT；重新进入存档后不会丢任务，也不会把不同样板的输入串在一起。
- “返还至 ME”现在会依次返还活动任务和每个隔离等待任务的输入。

该任务对象目前承载富集仓的物品输入；以后扩展冶金灌注机时，可在同一个任务对象内增加该样板专属的化学品/灌注类型缓存，不需要共享机器级化学品槽，因此相同物品使用红石和钻石等不同灌注配方时不会串料。

能量参数同步提高：

- 基础缓存：`1,000,000 FE`；每张能量卡增加 `500,000 FE`，8 张时为 `5,000,000 FE`。
- 基础最大输入：`200,000 FE/t`；每张能量卡增加 `200,000 FE/t`，8 张时为 `1,800,000 FE/t`。
- 左下能量控件悬停提示增加精确的最大输入速率。
- 修复加载顺序：先恢复升级卡并重算容量，再载入已存能量，避免高于基础容量的存量在读档时被提前截断。

GUI 二次调整：

- 主面板和数字状态框再次加宽；能量概要优先使用无多余小数的 `k/M` 格式，并按实际字体宽度切换到无空格紧凑格式。
- 当前输入与产物槽重新拉开；进度箭头被严格限制在两个槽之间的间隙内，不再压入产物槽或越过处理框。
- 升级抽屉随主面板右移，槽位和背景坐标保持一致。

验证结果：

- 中英文语言 JSON 解析通过。
- `compileJava --offline --no-daemon`：`BUILD SUCCESSFUL`。
- `build --offline --no-daemon`：`BUILD SUCCESSFUL`。
- 更新后的客户端已启动并完成 AE2、Mekanism、Applied Mekanistics 和本模组资源加载。

实机重点验收：

1. 安装并行卡后同时下发多份同一富集配方，单个周期的产物数量应高于 1。
2. 同时下发红石和钻石任务；当前配方仍有内部材料时不应切换，耗尽后才处理另一配方。
3. 退出并重新进入存档，活动任务及等待任务应继续保持原样板归属。
4. 能量状态文字和箭头不得越过各自边框；能量悬停提示应显示新的容量和最大输入速率。

### 15.12 GTNH 式预投递缓存与倍率并行（2026-08-06）

本轮把富集仓从“并行数决定 AE2 最多下发多少份”的按需模型，改为类似 GTNH 样板总成的预投递模型：

- `isBusy()` 不再以并行批量为门槛；任务接入开启且机器没有安全故障时，会持续接受 AE2 投递，直到内部累计达到 `1,048,576` 次操作的防御性上限。
- AE2 CPU 因此会在正常任务规模下尽快把可发配的材料全部交给机器；并行卡只决定每个加工周期能执行多少次，不再限制投递深度。
- 内部输入与待返还产物改用 `AEItemKey + long` 计数，不再把大量资源塞进 `ItemStack.count`，NBT 仍按聚合任务保存，不会为每次操作写一条记录。
- 活动任务和等待任务继续携带各自编码样板身份；相同样板会聚合并保持当前配方亲和，不同样板不会共享输入账本，为后续化学品/灌注类型隔离保留扩展位置。
- 并行卡倍率固定为：0~8 张分别 `x1/x2/x3/x4/x6/x8/x10/x12/x16`。
- 升级抽屉由 6 个物理槽扩成 8 个，按 2 列 4 行排列；每种升级卡仍最多安装 8 张。
- GUI 数字框显示紧凑缓存用量和能量；并行卡悬停提示显示当前倍率及完整倍率曲线。

安全与迁移：

- 新任务账本使用 `TaskDataVersion = 2`。旧版 `PendingInput`、`PendingOutput`、`ActivePattern`、`PendingOperations`、`InputPerOperation` 和 `ProcessingQueue` 会迁移到长计数格式。
- 读到配方失效、数量不整除或损坏任务时，不再删除或无限重新排队；机器进入“需要返还物品”状态，拒绝新任务并保留资源，玩家可用左侧返还按钮排空。
- 返还输入前先模拟 AE2 可接收数量，并只按完整配方单位返还，避免存储接近满载时把一个操作拆成无法继续处理的残数。
- 玩家不能在机器仍有缓存、样板或升级卡时直接拆除方块；需要先返还缓存并取出槽内物品，避免普通拆机造成账本丢失。

实现过程：

- 使用本机 OpenCode 中已配置的 `deepseek/deepseek-v4-pro` 完成第一版核心改造；该会话在编译阶段卡住并超时。
- 随后人工审查并修复了错误的 Mekanism 配方接口、失效任务无限重排队、超大显示栈、旧 NBT 异常记录丢失、活动配方被插队和状态框文字越界等问题。
- `compileJava --offline --console=plain --no-daemon` 和完整 `build --offline --console=plain --no-daemon` 均已通过，中英文语言 JSON 解析正常；仍需在实际存档中验收 AE2 CPU 一次性下发、返还按钮、重进存档和 0~8 张并行卡的吞吐。

实机验收结果（用户于 2026-08-06 确认）：

- 大批量任务能够由 AE2 CPU 预投递到机器内部，不再停在并行批量数量等待。
- 红石与钻石等不同样板同时下发时，当前配方会持续加工至内部材料耗尽后再切换。
- 8 张并行卡能够按 `x16` 批量工作。
- “返还至 ME”能够正确返还缓存资源。
- 退出并重新进入存档后，缓存数量和任务归属保持正常。
- 8 格升级抽屉、缓存/能量文字及整体 GUI 显示正常。

上述 GTNH 式预投递缓存、样板隔离、倍率并行、返还和持久化功能已通过本轮实机验收。

### 15.13 Jade 与 JEI 测试兼容（2026-08-06）

本轮增加测试配方和内部缓冲所需的两项工具：

- Jade 采用官方 15.10.5 API 实现自有方块信息提供器。服务端发送权威状态，客户端只排版，不读取可能滞后的客户端方块实体字段。
- 看向 ME 富集仓时显示：ME 在线/离线/任务接入关闭/需要返还、缓冲操作数与上限、当前输入或待返产物、当前并行倍率，以及精确 FE/容量。
- Jade 的数据写入独立的 `mekanismae:me_enrichment_chamber` 子标签，避免与其他插件共用根 NBT 时键名冲突；新增的方块实体 getter 全部只读，不会推进队列或修改状态。
- Mekanism 10.7.19.85 本身已提供富集等 JEI 配方分类；没有在本项目中复制配方展示逻辑。
- AE2 19.2.17 已内置 EMI/REI 等集成，但没有 JEI 集成。本地加入开源 `AE2 JEI Integration 1.2.1`，使用它针对 AE2 样板编码终端注册的传输处理器：JEI 的合成配方编码为合成样板，其他配方（包括 Mekanism 富集配方）编码为处理样板。

实现依据均来自对应开源项目，而不是猜测接口：

- [Jade 官方源码](https://github.com/Snownee/Jade)
- [JEI 官方源码](https://github.com/mezz/JustEnoughItems)
- [AE2 JEI Integration 源码](https://github.com/Tamaized/AE2-JEI-Integration)

本轮原计划将边界清晰的 Jade provider 交给本机 OpenCode 的 `deepseek/deepseek-v4-flash`。模型列表查询成功，但两次实际调用均在开始前被 `unknown certificate verification error` 阻断，未产生任何代码修改；随后人工对照 Jade 15.10.5 官方源码完成实现。首次客户端验收发现 Jade 开发环境会断言检查 provider 的配置项翻译，补齐 `config.jade.plugin_mekanismae.me_enrichment_chamber_status` 后重新启动通过。

验证结果：

- `compileJava --offline --console=plain --no-daemon`：`BUILD SUCCESSFUL`。
- `build --offline --console=plain --no-daemon`：`BUILD SUCCESSFUL`。
- 新客户端日志确认加载 `Jade 15.10.5`、`JEI 19.27.0.335`、`AE2 JEI Integration 1.2.1` 和本模组。
- Jade 成功发现并加载 `MeEnrichmentChamberJadePlugin`，未再出现配置翻译断言。
- JEI 成功注册 Mekanism 配方和 AE2-JEI 的配方传输处理器，运行时构建完成且无相关错误。

实机验收结果（用户于 2026-08-10 确认）：

1. 看向有大批量内部缓存的 ME 富集仓时，Jade 各行数值、状态和排版正常。
2. 在 AE2 样板编码终端中打开 Mekanism 富集配方并点击 JEI 的 `+`，输入和产物能够直接填入处理样板。

Jade 状态显示与 JEI 到 AE2 处理样板的传输兼容已通过实机验收。

### 15.14 ME 冶金灌注机双输入账本（2026-08-10）

本轮新增 ME 冶金灌注机，并把 GTNH 式预投递模型扩展为真正的物品＋化学品双输入任务：

- 处理样板必须包含一个 `AEItemKey` 物品输入、一个 Applied Mekanistics `MekanismKey` 化学品输入和一个物品产物。
- 配方识别使用 Mekanism 10.7.19.85 的 `TYPE_METALLURGIC_INFUSING` 与 `SingleItemChemicalRecipeInput`；单次物品用量和化学品用量均由配方 ingredient 计算。
- 对 `perTickUsage` 配方，化学品总量按冶金灌注机基础 200 tick 换算，与 Mekanism JEI 分类写入处理样板的数量保持一致。
- 每个活动/等待任务独立保存编码样板身份、物品键与 long 数量、化学品键与 long 数量、两类单次用量和剩余操作数。只有这些字段全部一致时任务才允许聚合。
- 当前配方仍有完整材料时持续加工，耗尽并清空待输出后才切换下一任务；共享物品输入但使用红石、钻石等不同化学品的样板不会串料。
- 0~8 张并行卡继续使用 `x1/x2/x3/x4/x6/x8/x10/x12/x16` 曲线；AE2 预投递深度与并行倍率相互独立。
- 返还按钮先模拟物品与化学品两边可接收的完整操作数，再共同返还；异常残数进入故障恢复路径，可分别送回网络，避免资源永久卡死。
- 新机器使用 `TaskDataVersion = 3` 保存双输入活动任务、等待队列、待输出、能量、样板和升级卡。

GUI 与兼容：

- 复用已验收的 Mekanism 风格布局：9 个顶部样板槽、8 个升级槽、ME 接入与返还按钮、内部能量状态。
- 数字状态框新增当前化学品名称与缓存量；Jade provider 从服务端同步化学品注册名和 long 数量，客户端不读取滞后的方块实体字段。
- JEI 到 AE2 处理样板仍由 AE2 JEI Integration 负责；Applied Mekanistics 的 JEI 转换器会把 Mekanism `ChemicalStack` 编码成 `MekanismKey`，本机直接消费该官方键类型。

实现分工与依据：

- OpenCode `deepseek/deepseek-v4-flash` 负责新方块注册/资源/基础 GUI 脚手架、后端基线机械复制，以及 Jade/语言重复适配；主代理审查了全部改动范围。
- 双输入解析、配方核算、隔离账本、加工调度、返还安全和 NBT 持久化由主代理实现。
- 接口依据来自锁定版本的 [Mekanism 官方源码](https://github.com/mekanism/Mekanism) 与 [Applied Mekanistics 官方源码](https://github.com/AppliedEnergistics/Applied-Mekanistics)，没有自行假设化学品 AEKey 或配方格式。

自动验证：

- 中英文语言 JSON 以 UTF-8 解析通过，`git diff --check` 无空白错误。
- `compileJava --offline --no-daemon --rerun-tasks --console=plain`：`BUILD SUCCESSFUL`。
- 客户端完成 Mekanism、AE2、Applied Mekanistics、JEI、Jade 和本模组资源加载；Jade 成功加载包含新冶金灌注机 provider 的插件，无注册崩溃或配置翻译断言。

实机验收结果（用户于 2026-08-10 确认）：

1. AE2 能读取机器中的冶金灌注处理样板，正常倍率样板可以成功发配并加工。
2. 大倍率样板同样能够参与调度；测试中出现的阻塞最终确认是样板把红石化学品写多了 10 倍：`1000` 次铜锭灌注应填写 `10 B`，而不是 `100 B`，并非机器容量或投递深度限制。
3. 物品与化学品的双输入识别、样板专属任务账本和 GTNH 式预投递流程整体测试无问题。
4. 最终测试日志没有出现冶金灌注样板拒绝、任务故障或本模组异常；客户端退出时所有维度均正常保存。

在正式发布前仍应把双配方串料、返回至 ME、任务中途重进存档和 0/1/8 张并行卡列入完整回归测试，避免后续通用化改造引入倒退。

### 15.15 通用加工基础与 ME 粉碎机（2026-08-11）

本轮先完成机器通用化，再以 ME 粉碎机验证扩展新物品加工机不需要复制整套富集仓代码：

- 新增 `AbstractMeProcessingBlockEntity`，统一管理 AE2 托管节点、9 个处理样板槽、8 个升级槽、FE/Mekanism Strict Energy 双接口、网络接入开关、并行倍率曲线和通用 NBT。
- 新增 `AbstractItemToItemMeMachineBlockEntity`，承载物品到物品机器共用的 GTNH 式大容量预投递、按编码样板身份隔离的任务账本、当前配方亲和、产物返还、故障恢复和 `TaskDataVersion = 2` 迁移逻辑。
- ME 富集仓迁移到物品加工基类，ME 冶金灌注机迁移到通用节点/能量/样板/升级基类；旧存档使用的 `Patterns`、`Pattern`、`Energy`、`Upgrades`、`NetworkEnabled`、任务队列及升级数量键保持不变。
- GUI、菜单、方块交互与 Jade 的物品加工部分也抽成共用实现；新机器只需要声明自己的配方类型、注册项、显示名称和资源模型。
- 对照 Jade 15.10.5 的 `HierarchyLookup` 后，将共用方块交互放入中性基类，使富集仓与粉碎机成为并列子类；避免粉碎机因继承富集仓方块类而同时命中两个 Jade component provider。
- 新增 ME 粉碎机，配方严格使用 Mekanism 10.7.19.85 官方 `MekanismRecipeTypes.TYPE_CRUSHING`，没有自行复制或猜测配方判断。方块模型继承 Mekanism 官方粉碎机模型。
- 注册了方块、物品、方块实体、菜单、客户端界面、AE2 节点、NeoForge FE、Mekanism Strict Energy、Jade、创造模式物品、语言、模型、方块状态和战利品表。

实现依据：

- [Mekanism 官方源码](https://github.com/mekanism/Mekanism)：核对 `MekanismRecipeTypes.TYPE_CRUSHING`、`ItemStackToItemStackRecipe` 以及官方粉碎机模型。
- [Jade 官方源码](https://github.com/Snownee/Jade)：核对 component/data provider 的父类层级收集规则。
- 已锁定的 AE2、NeoForge、Jade 接口继续沿用前几轮经实机验证的实现，通用化没有修改 AE2 发配协议或任务账本格式。
- 曾再次尝试把边界明确的粉碎机表现层脚手架交给本机 OpenCode 的 `deepseek/deepseek-v4-flash`；模型完成只读分析后仍被 `unknown certificate verification error` 中断，没有写入工作区。最终实现由主代理完成并逐项审查。

自动验证：

- `git diff --check`：通过。
- `build --offline --no-daemon --rerun-tasks --console=plain`：`BUILD SUCCESSFUL`，全部任务重新执行。
- 新客户端成功进入主菜单；日志确认本模组资源与 Jade 插件正常加载，没有 `me_crusher` 注册、模型、翻译或配置断言错误。
- 运行中的 Render 线程停在 GLFW 等待事件/帧率限制处，确认客户端是在主菜单正常空闲，不是启动卡死。

回归与实机验收清单：

1. 给 ME 粉碎机放入有效粉碎处理样板，确认 AE2 能读取样板并将大批量任务预投递到机器。
2. 同时下发两种粉碎配方，确认当前样板材料耗尽后才切换，两个样板的内部账本不会串料。
3. 分别测试 0、1、8 张并行卡，以及加速卡、能量卡、暂停接入和“返还至 ME”。
4. 任务处理中退出并重进存档，确认缓存数量、活动样板、等待队列、能量和升级卡完整恢复。
5. 回归 ME 富集仓与 ME 冶金灌注机，尤其检查旧存档、化学品隔离、返还和方块拆除保护没有变化。

### 15.16 双层平衡配置与 JEI 机器入口（2026-08-11）

本轮为后续继续增加机器建立统一的平衡参数入口，并保持现有存档默认行为不变：

- 注册 `config/mekanismae-server.toml` 作为整套实例的全局默认配置；NeoForge 在存档的 `serverconfig/` 中发现同名文件时，会原生改用该存档副本。
- 存档覆盖默认关闭（即没有同名副本）；普通玩家只需改一次全局配置。需要单独调节某个存档时，把全局文件复制到该存档的 `serverconfig/` 后修改即可，不再维护重复的 COMMON/SERVER 规范或额外开关。
- 在模组列表注册 NeoForge 标准配置界面，无需手动寻找文件即可查看和编辑当前配置。
- 通用网络参数包含 AE2 频道要求、待机 AE 功耗和红石暂停；升级参数包含每张能量卡的容量/输入增量，以及 0～8 张加速卡、并行卡的完整倍率曲线。
- `machines.defaults` 统一设置基础能量容量、最大 FE 输入、单次耗能、加工刻数和最大预投递缓存；ME 富集仓、ME 粉碎机、ME 冶金灌注机均可选择继承默认值或独立覆写。
- 所有参数采用世界重载语义，并在方块实体构造时形成不可变快照；SERVER 配置由 NeoForge 在连接阶段同步，服务端加工逻辑保持权威，实际加工刻数、缓存上限、速度/并行倍率、能量容量与输入上限继续通过菜单或 Jade 同步给客户端。
- 极端能量配置使用 `long` 中间值并钳制到 NeoForge FE 的 `int` 上限；倍率曲线缺少条目时沿用最后一个值，超过 9 项时忽略多余项；降低缓存上限不会删除已有任务。
- 缺能量时加工进度现在封顶于 100%，避免高加速倍率下长期断电造成进度整数持续累加。

JEI 不复制 Mekanism 配方或分类，而是复用官方分类：

- ME 富集仓、ME 粉碎机、ME 冶金灌注机分别注册为 `ENRICHING`、`CRUSHING`、`METALLURGIC_INFUSING` 的 recipe catalyst。
- 三种 Mekanism 配方类型需要 `RecipeHolder`；注册使用 Mekanism 自己的 `MekanismJEI.genericRecipeType(...)`，避免错误调用基础 `recipeType(...)` 导致运行时异常。
- JEI 在 `neoforge.mods.toml` 中声明为客户端可选依赖；没有安装 JEI 时不会加载兼容类。

实现与审查分工：

- OpenCode `deepseek/deepseek-v4-flash` 完成 JEI catalyst 和中英文配置翻译的边界任务，并运行离线编译。
- OpenCode `deepseek/deepseek-v4-pro` 对配置生命周期、客户端同步、数值边界、ContainerData 索引、JEI Holder 类型、Jade 与旧存档兼容做只读审查；未发现高严重度问题。
- 主代理复核官方 NeoForge、Mekanism 与 JEI 源码，补充 JEI 可选依赖、断电进度封顶、Jade 实际速度倍率以及冗余字段清理。

自动与运行时验证：

- 中英文语言文件及全部资源 JSON 以 UTF-8 解析通过，`git diff --check` 通过。
- `build --offline --no-daemon --rerun-tasks --console=plain`：`BUILD SUCCESSFUL`。
- 最终客户端直接进入既有测试存档，旧方块实体正常加载；服务端载入 3460 个配方，玩家成功加入，未出现本模组配置、方块实体或任务账本异常。
- JEI 完成 recipe catalyst、配方、传输处理器和运行时构建，三种 Mekanism 分类注册阶段无异常。
- 新架构只生成 `config/mekanismae-server.toml`；NeoForge/FML 本地实现已核对为“存档同名文件存在时使用 `serverconfig` 覆盖，否则读取全局文件”。
