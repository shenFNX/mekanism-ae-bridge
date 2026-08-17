---
navigation:
  title: Upgrades and Throughput
  position: 50
---

# Upgrades and Throughput

The right-hand drawer has eight slots for each supported card type:

* Speed Cards shorten the processing cycle. The default 0-8 card curve is `1/2/3/4/5/6/7/8/9`.
* Parallel Cards multiply operations completed per cycle. The default curve is `1/2/3/4/6/8/10/12/16`.
* Energy Cards add FE capacity and FE input rate. By default, each card adds 500,000 FE capacity and 200,000 FE/t input.

The separate upper-left slot accepts one Mekanism tier installer:

* <ItemImage id="mekanism:basic_tier_installer" scale="2" /> Basic Tier Installer
* <ItemImage id="mekanism:advanced_tier_installer" scale="2" /> Advanced Tier Installer
* <ItemImage id="mekanism:elite_tier_installer" scale="2" /> Elite Tier Installer
* <ItemImage id="mekanism:ultimate_tier_installer" scale="2" /> Ultimate Tier Installer

The default tier parallel multipliers are `3/6/10/16`. Energy capacity, FE input, and task-buffer multipliers default to `2/4/8/16`. Tier installers do not change processing time.

Final parallel throughput is the machine's base operations per cycle multiplied by the Parallel Card multiplier and the tier multiplier. The GUI and Jade show this final value directly. With default settings, an Ultimate Tier Installer and eight Parallel Cards give item machines 256 operations per cycle.
