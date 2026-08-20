# Mekanism ME Integration

Mekanism ME Integration 把 Mekanism 的加工线接入 Applied Energistics 2。模组提供 19 台连接 ME 的 Mekanism 机器：玩家可以把 Mekanism 配方编码为 AE2 处理样板，从 AE2 合成 CPU 下单，并让成品自动返回 ME 存储。

## 主要内容

- 每台机器拥有 9 个处理样板槽。
- 直接接收 AE2 发配的任务，并提供大容量内部任务缓存，让 CPU 可以一次发配大量加工任务。
- 以编码样板为单位隔离物品、流体和化学品输入，避免不同配方串料。
- 当前配方还有材料时持续加工，减少无意义的配方切换。
- 支持 FE 和 Mekanism 能量，并在 GUI 中显示清晰的机器状态。
- 支持速度卡、并行卡和能量卡；每种机器最多安装 8 张，物品栏中可以堆叠至 64 个。
- 支持 Mekanism 等级安装器；安装 Mekanism Extras 后，可兼容它提供的额外等级安装器。
- 支持 JEI 配方分类、AE2 JEI Integration 样板填充、Jade 状态查看，以及中英文 GuideME 指南。
- 可配置加工速度、能耗、缓存容量、每周期处理量、卡片倍率和等级倍率。

## 使用方法

把对应的 Mekanism 原版机器和 AE2 样板供应器放入无序合成栏，即可合成 ME 版本机器。将机器接入 AE2 线缆，并单独提供 FE。为该机器支持的配方编码处理样板，将样板放入机器顶部的 9 个槽位之一，然后从 AE2 终端发起合成。

AE2 发配的任务会进入机器内部缓存，实际产物会返回 ME 存储。每个编码样板都有独立的资源账本，即使不同配方使用相同物品或化学品，也不会互相串料。

## 前置与下载

必须安装 Minecraft 1.21.1、NeoForge、Mekanism、Applied Energistics 2、Applied Mekanistics 和 GuideME。JEI、AE2 JEI Integration、Jade 与 Mekanism Extras 为可选兼容模组。

完整机器列表、安装说明和配置说明请查看[项目 README](../README.md)。模组下载位于 [GitHub Releases](https://github.com/shenFNX/mekanism-ae-bridge/releases)。
