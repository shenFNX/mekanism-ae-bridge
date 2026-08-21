# Mekanism ME Integration

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/Loader-NeoForge-orange?style=flat-square)](https://neoforged.net/)
[![Latest release](https://img.shields.io/github/v/release/shenFNX/mekanism-ae-bridge?style=flat-square&label=Release)](https://github.com/shenFNX/mekanism-ae-bridge/releases)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

**Mekanism ME Integration** connects Mekanism processing machines to Applied Energistics 2 automation. It adds ME versions of Mekanism machines that accept AE2 processing patterns, receive complete crafting jobs from a ME network, and return the real products to ME storage.

The goal is simple: keep Mekanism's recipes and machines, while letting AE2 handle pattern encoding, crafting requests, input delivery, and output storage.

[中文说明](docs/project-description.zh-CN.md) · [English description](docs/project-description.en.md) · [Releases](https://github.com/shenFNX/mekanism-ae-bridge/releases)

## What it adds

- **19 ME-connected Mekanism machines**, covering item processing, item + chemical recipes, fluid processing, and chemical processing.
- **Nine encoded-pattern slots per machine**, so one block can serve multiple recipes.
- **Direct AE2 crafting integration**: patterns are published to the network and jobs are delivered from an AE2 crafting CPU.
- **Large internal job buffers** inspired by the GTNH pattern-provider workflow. A crafting CPU can dispatch many operations at once instead of stopping at the machine's current parallel amount.
- **Per-pattern resource isolation** for items, fluids, and chemicals. Inputs and products from different encoded patterns cannot be mixed together.
- **Continuous recipe processing**: a machine keeps working on the current pattern while that pattern still has enough resources to continue.
- **FE and Mekanism energy support**, with a separate energy buffer shown in the GUI.
- **Speed, Parallel, and Energy Cards**, up to eight of each per machine. The cards can be tuned in configuration and work together with Mekanism tier installers.
- **Optional Mekanism Extras support** for Absolute, Supreme, Cosmic, and Infinite tier installers.
- **Optional Applied Flux and AE2 Lightning Tech support**. An Overloaded Power Supply can power these machines even when it and the machine belong to the same ME network.
- **GuideME, JEI, AE2 JEI Integration, and Jade support** for in-game documentation, recipe lookup, pattern transfer, and machine status inspection.

## Included machines

### Item processors

Enrichment Chamber, Crusher, Energized Smelter, Metallurgic Infuser, Osmium Compressor, Combiner, and Precision Sawmill.

### Item and chemical processors

Purification Chamber, Chemical Injection Chamber, and Antiprotonic Nucleosynthesizer.

### Fluid and chemical processors

Chemical Oxidizer, Chemical Crystallizer, Chemical Dissolution Chamber, Chemical Infuser, Electrolytic Separator, Rotary Condensentrator, Chemical Washer, Nutritional Liquifier, and Pressurized Reaction Chamber.

Each machine exposes the recipes of its corresponding Mekanism machine. Multi-output recipes can return products that were not listed in the pattern; those by-products are still handled by the machine and sent back to ME storage.

## Getting started

1. Craft an ME machine by combining the matching Mekanism machine with an AE2 Pattern Provider in a shapeless recipe.
2. Connect the machine to an AE2 cable. By default, it uses one ME channel.
3. Supply the machine with FE through a compatible cable, such as a Mekanism Universal Cable. The AE2 connection and the machine's internal FE buffer are separate systems.
4. Encode an **AE2 processing pattern** for a recipe supported by that machine, then place it in one of the nine pattern slots across the top.
5. Start the craft from an AE2 terminal. AE2 sends the complete job into the machine's internal buffer; the materials do not need to appear in ordinary item slots.
6. Install cards or a Mekanism tier installer when more speed, energy capacity, input rate, buffer space, or parallel processing is needed.

With JEI and AE2 JEI Integration installed, use JEI's `+` button to transfer a Mekanism recipe to the AE2 Pattern Encoding Terminal.

## Upgrades and configuration

The three ME cards stack to 64 in a player's inventory, while each machine accepts up to eight cards of each type. The tier installer has its own slot and does not consume card slots. Mekanism Extras installers are recognized automatically when Mekanism Extras is present.

The mod provides both global defaults and per-world server overrides. Configure energy capacity and input rate, processing time, card multiplier curves, tier multipliers, buffer limits, per-machine throughput, AE2 channel use, idle power, and redstone behavior in:

```text
config/mekanismae-server.toml
saves/<world>/serverconfig/mekanismae-server.toml
```

World-specific values are optional; without an override file, the global server configuration is used.

## Requirements

The following mods are required on both the client and the server:

| Dependency | Minimum version | Purpose |
| --- | --- | --- |
| Minecraft | `1.21.1` | Game version |
| NeoForge | `21.1.248` | Mod loader |
| Mekanism | `10.7.19` | Machines, recipes, and energy |
| Applied Energistics 2 | `19.2.17` | ME network and processing patterns |
| Applied Mekanistics | `1.6.3` | AE2 storage and transport for Mekanism chemicals |
| GuideME | `21.1.17` | In-game guide pages |

Optional integrations:

| Optional mod | Purpose |
| --- | --- |
| Just Enough Items (JEI) | Recipe and usage lookup |
| AE2 JEI Integration | Fill AE2 processing patterns from JEI |
| Jade | View machine status, energy, buffer, and parallel value |
| Mekanism Extras | Additional high-tier installers |
| Applied Flux | Store and move FE through the ME network |
| AE2 Lightning Tech | Power machines wirelessly with the Overloaded Power Supply; requires Applied Flux for this feature |

This project targets **Java 21** and **NeoForge 1.21.1**.

## Downloads and support

Download the latest build from the [GitHub Releases page](https://github.com/shenFNX/mekanism-ae-bridge/releases). If you find a problem, check the in-game GuideME troubleshooting page first, then open a [GitHub issue](https://github.com/shenFNX/mekanism-ae-bridge/issues) with the Minecraft version, mod versions, log, and a short reproduction sequence.

## License

Mekanism ME Integration is available under the [MIT License](LICENSE).
