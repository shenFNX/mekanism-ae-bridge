---
navigation:
  title: 升级与吞吐
  position: 50
---

# 升级与吞吐

右侧升级抽屉支持三类卡，每类最多八张：

* 速度卡缩短单次加工周期。默认 0～8 张倍率为 `1/2/3/4/5/6/7/8/9`。
* 并行卡增加每周期完成的操作数。默认倍率为 `1/2/3/4/6/8/10/12/16`。
* 能量卡提高 FE 容量和 FE 输入速度。默认每张增加 500,000 FE 容量和 200,000 FE/t 输入。

左上角独立槽可以放入一个 Mekanism 等级安装器：

* <ItemImage id="mekanism:basic_tier_installer" scale="2" /> 基础等级安装器
* <ItemImage id="mekanism:advanced_tier_installer" scale="2" /> 高级等级安装器
* <ItemImage id="mekanism:elite_tier_installer" scale="2" /> 精英等级安装器
* <ItemImage id="mekanism:ultimate_tier_installer" scale="2" /> 终极等级安装器

四个等级默认并行倍率为 `3/6/10/16`；能量容量、FE 输入和任务缓存倍率默认都是 `2/4/8/16`。等级安装器不改变加工时间。

最终并行等于机器每周期基础处理份数、并行卡倍率和等级倍率的乘积。GUI 与 Jade 直接显示这个最终值。按默认配置，终极安装器加八张并行卡可以让普通物品机每周期处理 256 份。
