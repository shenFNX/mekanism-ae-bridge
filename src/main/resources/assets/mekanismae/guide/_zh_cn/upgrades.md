---
navigation:
  title: 升级与吞吐
  position: 50
---

# 升级与吞吐

右侧升级抽屉支持三类卡，每类最多八张：

三种卡在玩家物品栏中均可堆叠 64 个；每台机器仍然最多接受每类八张。

三张卡都有无序合成配方：速度卡组合 AE2 速度卡与 Mekanism 速度升级；能量卡组合 AE2 能量卡与 Mekanism 能量升级；并行卡组合 AE2 容量卡、Mekanism 速度升级和高级控制电路。JEI 中可直接查看配方。

* 速度卡缩短单次加工周期。默认 0～8 张倍率为 `1/2/3/4/5/6/7/8/9`。
* 并行卡增加每周期完成的操作数。默认 0～8 张倍率为 4 的幂：`1/4/16/64/256/1024/4096/16384/65536`。
* 能量卡提高 FE 容量和 FE 输入速度。默认每张增加 500,000 FE 容量和 200,000 FE/t 输入。

安装 Applied Flux 后，升级抽屉还会接受最多一张 Applied Flux 感应卡。机器在线时会直接从当前 ME 网络的 FE 存储中补充内部能量，传输速度同时受机器 FE 输入上限与 Applied Flux 访问器上限约束。

左上角独立槽可以放入一个 Mekanism 等级安装器：

* <ItemImage id="mekanism:basic_tier_installer" scale="2" /> 基础等级安装器
* <ItemImage id="mekanism:advanced_tier_installer" scale="2" /> 高级等级安装器
* <ItemImage id="mekanism:elite_tier_installer" scale="2" /> 精英等级安装器
* <ItemImage id="mekanism:ultimate_tier_installer" scale="2" /> 终极等级安装器

安装 Mekanism Extras 后，同一个槽位还会接受它的可选等级安装器：
`mekanism_extras:absolute_tier_installer`、
`mekanism_extras:supreme_tier_installer`、
`mekanism_extras:cosmic_tier_installer` 和
`mekanism_extras:infinite_tier_installer`（中文名为“悖论无限”）。

八个等级默认并行倍率为 `3/6/10/16/32/64/128/256`，依次对应原版四级和 Mekanism Extras 的绝对、至尊、寰宇支配、悖论无限。能量容量、FE 输入和任务缓存倍率默认都是 `2/4/8/16/32/64/128/256`。等级安装器不改变加工时间。

GUI 与 Jade 显示的并行倍率等于并行卡倍率和等级倍率的乘积。部分低用量流体/化学品配方还会使用机器内部的基础批量（默认为 1000），但这个实现细节不会再乘进玩家看到的并行数字。按默认配置，终极安装器加四张并行卡显示 `4096`，加八张显示 `1048576`。
