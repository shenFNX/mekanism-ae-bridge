---
navigation:
  title: Upgrades and Throughput
  position: 50
---

# Upgrades and Throughput

The right-hand drawer has eight slots for each supported card type:

All three card items stack to 64 in player inventories. A machine still accepts at most eight cards of each type.

All three cards have shapeless recipes. The Speed Card combines an AE2 Speed Card with a Mekanism Speed Upgrade; the Energy Card combines the corresponding AE2 and Mekanism energy upgrades; and the Parallel Card combines an AE2 Capacity Card, a Mekanism Speed Upgrade, and an Advanced Control Circuit. The recipes are visible in JEI.

* Speed Cards shorten the processing cycle. The default 0-8 card curve is `1/2/3/4/5/6/7/8/9`.
* Parallel Cards multiply operations completed per cycle. The default curve is `1/2/3/4/6/8/10/12/16`.
* Energy Cards add FE capacity and FE input rate. By default, each card adds 500,000 FE capacity and 200,000 FE/t input.

With Applied Flux installed, the upgrade drawer also accepts up to one Applied Flux Induction Card. While the machine is online, it fills its internal buffer directly from FE stored on the current ME network. Transfer is limited by both the machine's FE input rate and Applied Flux's accessor limit.

The separate upper-left slot accepts one Mekanism tier installer:

* <ItemImage id="mekanism:basic_tier_installer" scale="2" /> Basic Tier Installer
* <ItemImage id="mekanism:advanced_tier_installer" scale="2" /> Advanced Tier Installer
* <ItemImage id="mekanism:elite_tier_installer" scale="2" /> Elite Tier Installer
* <ItemImage id="mekanism:ultimate_tier_installer" scale="2" /> Ultimate Tier Installer

When Mekanism Extras is installed, its optional tier installers are accepted in
the same slot: `mekanism_extras:absolute_tier_installer`,
`mekanism_extras:supreme_tier_installer`, `mekanism_extras:cosmic_tier_installer`,
and `mekanism_extras:infinite_tier_installer` (the Chinese localization calls
the last one 悖论无限).

The default tier parallel multipliers are `3/6/10/16/32/64/128/256` for
Basic through Ultimate and then Mekanism Extras Absolute through Infinite.
Energy capacity, FE input, and task-buffer multipliers use the same tier curve
with `2/4/8/16/32/64/128/256`. Tier installers do not change processing time.

Final parallel throughput is the machine's base operations per cycle multiplied by the Parallel Card multiplier and the tier multiplier. The GUI and Jade show this final value directly. With default settings, an Ultimate Tier Installer and eight Parallel Cards give item machines 256 operations per cycle.
