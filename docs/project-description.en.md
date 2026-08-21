# Mekanism ME Integration

Mekanism ME Integration brings Mekanism's processing line into Applied Energistics 2. It adds ME-connected versions of 19 Mekanism machines, allowing you to encode their recipes as AE2 processing patterns, request jobs from an AE2 crafting CPU, and send the finished products back to ME storage.

## Highlights

- 27 processing-pattern slots on every machine, arranged in three rows.
- Central pattern management through AE2's Pattern Access Terminal while machines are idle.
- Direct AE2 delivery with a large internal job buffer, so crafting CPUs can dispatch many operations at once.
- Strict isolation for every encoded pattern, including item, fluid, and chemical inputs.
- Continuous processing of the active recipe instead of unnecessary recipe swapping.
- FE and Mekanism energy support with a readable machine status screen.
- Speed, Parallel, and Energy Cards, plus Mekanism tier installers. Each card type supports up to eight installed cards; the items stack to 64 in player inventories.
- Optional compatibility with Mekanism Extras tier installers.
- Optional Applied Flux support: an Induction Card lets a machine draw FE directly from ME storage.
- Optional AE2 Lightning Tech support for powering machines with an Overloaded Power Supply.
- JEI recipe catalysts, AE2 JEI Integration pattern transfer, Jade status information, and a GuideME manual in English and Chinese.
- Configurable processing speed, energy, buffer capacity, throughput, card curves, and tier multipliers.

## How to use it

Craft the ME version of a Mekanism machine by combining the original machine with an AE2 Pattern Provider in a shapeless recipe. Connect the block to an AE2 cable and provide FE separately. Encode a processing pattern for a recipe supported by that machine, place it in one of the 27 top slots, and start the craft from an AE2 terminal.

The machine keeps delivered work in its internal buffer and returns actual recipe products to ME storage. Different patterns have separate resource ledgers, so similar item or chemical recipes cannot cross-contaminate one another.

## Requirements

Minecraft 1.21.1, NeoForge, Mekanism, Applied Energistics 2, Applied Mekanistics, and GuideME are required. JEI, AE2 JEI Integration, Jade, Mekanism Extras, Applied Flux, and AE2 Lightning Tech are optional integrations.

For installation details and the complete machine list, see the [project README](../README.md). Downloads are available on the [GitHub Releases page](https://github.com/shenFNX/mekanism-ae-bridge/releases).
