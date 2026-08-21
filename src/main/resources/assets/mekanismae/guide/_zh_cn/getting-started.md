---
navigation:
  title: 快速开始
  position: 10
---

# 快速开始

## 合成机器

每台 ME 机器都使用无序配方：把对应的 Mekanism 普通机器与一个 <ItemLink id="ae2:pattern_provider" /> 放进合成栏。

<RecipeFor id="me_enrichment_chamber" />

## 连接两个系统

1. 把 AE2 线缆直接连接到机器。默认情况下，每台机器消耗一个频道。
2. 使用兼容的能量线缆输入 NeoForge FE，例如 Mekanism 通用线缆。
3. 打开机器，确认左侧 ME 标签显示在线，并且能量面板中已经有 FE。

AE 节点供能与机器 FE 是两套系统。机器可能已经被 AE 网络识别，但内部能量仍然是零。

安装 Applied Flux 与 AE2 闪电科技后，可以使用过载供电仪为机器无线供能。供电仪与机器可以接入同一个 ME 网络，不会再被同网格检查拦截。

## 放入样板

为对应的 Mekanism 配方写入处理样板，然后把它放进机器顶部九个样板槽之一。安装 JEI 和 AE2 JEI Integration 后，可以在 JEI 点击 `+`，直接向样板编码终端填入配方。

样板放入后，机器会把它发布给 AE2。从 AE 终端下单后，材料进入机器内部的任务缓存，不会出现在普通物品槽里。
