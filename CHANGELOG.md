# 更新日志 / Changelog

## v0.3.0

### 中文

这次更新让样板管理和供能方式更方便，同时保留了原有存档与机器的使用方式。

#### 更大的样板空间

- 每台机器的处理样板槽由 9 个增加到 27 个，并采用三排布局。
- 连接到 ME 网络且处于空闲状态的机器会显示在 AE2 样板访问终端中，可以集中查看、放入或取出样板。
- 机器加工期间会暂时锁定样板，并从样板访问终端中隐藏，避免正在执行的任务因样板被更换而出错。

#### 更多供能选择

- 新增 Applied Flux 感应卡支持。把感应卡放入升级区域后，机器可以直接使用 ME 网络中存储的 FE。
- 兼容闪电科技的过载供电仪；即使供电仪和机器连接在同一个 ME 网络中，也能正常为机器供能。

#### 升级与界面改进

- 为速度卡、并行卡和能量卡加入了生存模式合成配方。
- 并行卡默认倍率调整为 4 的幂：`1 / 4 / 16 / 64 / 256 / 1024 / 4096 / 16384 / 65536`。
- GUI 与 Jade 现在显示玩家实际使用的卡片和等级并行倍率。低用量流体、化学品机器的内部批处理倍率不再把显示数字放大，但实际加工能力不会降低。
- 鼠标悬停在当前加工区域的物品、流体或化学品图标上时，会显示对应名称。
- 样板区域标题和整体布局已针对 27 个槽位重新整理。

#### 兼容性

- 必需前置没有变化：Minecraft 1.21.1、NeoForge、Mekanism、Applied Energistics 2、Applied Mekanistics 和 GuideME。
- Applied Flux、闪电科技、Mekanism Extras、JEI、AE2 JEI Integration 与 Jade 仍为可选兼容模组。
- 可直接从 v0.2.1 升级；已有机器、样板和内部任务缓存会继续保留。

### English

This update makes pattern management and power delivery more convenient while keeping existing worlds and machines familiar to use.

#### More pattern space

- Every machine now has 27 processing-pattern slots instead of 9, arranged in three rows.
- Connected, idle machines appear in AE2's Pattern Access Terminal, where their patterns can be viewed, inserted, or removed from one place.
- Patterns remain locked while a machine is processing. Busy machines are temporarily hidden from the Pattern Access Terminal to protect active jobs from accidental pattern changes.

#### More ways to power machines

- Added Applied Flux Induction Card support. Install one in the upgrade area to let the machine draw FE directly from ME network storage.
- Added compatibility with AE2 Lightning Tech's Overloaded Power Supply, including when the supply and machine share the same ME network.

#### Upgrade and interface improvements

- Added survival crafting recipes for Speed, Parallel, and Energy Cards.
- The default Parallel Card curve now uses powers of four: `1 / 4 / 16 / 64 / 256 / 1024 / 4096 / 16384 / 65536`.
- The GUI and Jade now show the practical card-and-tier parallel multiplier. Internal batching used by low-volume fluid and chemical machines no longer inflates the displayed number, without reducing actual throughput.
- Hovering an item, fluid, or chemical in the current-processing area now shows its name.
- The pattern area and overall machine layout have been reorganized for the new 27-slot capacity.

#### Compatibility

- Required dependencies are unchanged: Minecraft 1.21.1, NeoForge, Mekanism, Applied Energistics 2, Applied Mekanistics, and GuideME.
- Applied Flux, AE2 Lightning Tech, Mekanism Extras, JEI, AE2 JEI Integration, and Jade remain optional integrations.
- Existing v0.2.1 worlds can be upgraded directly; placed machines, patterns, and buffered jobs are preserved.
