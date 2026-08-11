# Compatibility Matrix

## Locked baseline

| Component | Version |
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
| ModDevGradle | `2.0.143` |

## Local development jars

The jars under `libs/` are development/runtime inputs for this first compatibility spike. Their SHA-1 values are:

| File | SHA-1 |
| --- | --- |
| `Mekanism-1.21.1-10.7.19.85.jar` | `b78945c40cfe7640408f3fd1e44da385a8c8b805` |
| `appliedenergistics2-19.2.17.jar` | `49c18d6a4af487957d7e5a6ad5dcbf71090b8e14` |
| `Applied-Mekanistics-1.6.3.jar` | `bec4a47269ec23bca2329742e13409bfde69c5c3` |
| `guideme-21.1.17.jar` | `060e374f578db694a0b8f3ad409bd424cae86359` |
| `jei-1.21.1-neoforge-19.27.0.335.jar` | `4e18d321bdc23762ff0bc19e9e7a08b49866c449` |
| `ae2jeiintegration-1.2.1.jar` | `e99898b3e9c32bf6ac757e7ad50247746622cdfc` |
| `Jade-1.21.1-NeoForge-15.10.5.jar` | `d5bf134b3dbde9f5258666823900e21341dc0a50` |

Applied Mekanistics metadata confirms the runtime IDs `appmek`, `ae2`, and `mekanism`. AE2 requires `guideme` for this version.

AE2 19.2.17 does not contain JEI integration. The separate open-source
[AE2 JEI Integration](https://github.com/Tamaized/AE2-JEI-Integration) mod restores recipe transfer for the AE2
Pattern Encoding Terminal, including using JEI's `+` button to encode non-crafting recipes as processing patterns.
Mekanism supplies its own JEI recipe categories, so no Mekanism recipe duplication is implemented in this project.
The seven ME machines are registered as recipe catalysts for Mekanism's existing enriching, crushing, smelting, metallurgic infusing, compressing, purifying, and injecting categories. The energized smelter deliberately uses Mekanism's native smelting input cache, which includes its synthetic wrappers around vanilla furnace recipes; querying only `TYPE_SMELTING` through Minecraft's recipe manager would miss those recipes. JEI remains an optional client dependency; without JEI, the integration class is not loaded.

Release sources:

- [JEI official source](https://github.com/mezz/JustEnoughItems)
- [AE2 JEI Integration 1.2.1](https://www.curseforge.com/minecraft/mc-mods/ae2-jei-integration/files/7727898)
- [Jade 15.10.5](https://www.curseforge.com/minecraft/mc-mods/jade/files/7545219)
