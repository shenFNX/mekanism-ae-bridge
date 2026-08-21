---
navigation:
  title: Getting Started
  position: 10
---

# Getting Started

## Build a Machine

Every ME machine has a shapeless crafting recipe: combine the corresponding Mekanism machine with an <ItemLink id="ae2:pattern_provider" />.

<RecipeFor id="me_enrichment_chamber" />

## Connect Both Systems

1. Connect an AE2 cable directly to the machine. By default, every machine consumes one channel.
2. Supply NeoForge FE with a compatible power cable, including a Mekanism Universal Cable.
3. Open the machine and confirm that the ME tab reports online and the energy panel contains FE.

AE power and machine FE are separate. A machine can appear in the AE network while its internal energy remains at zero.

With Applied Flux and AE2 Lightning Tech installed, an Overloaded Power Supply can feed the machine wirelessly. Same-grid delivery is supported: the supply and this machine may belong to the same ME network.

## Install a Pattern

Encode a processing pattern for the matching Mekanism recipe and place it in one of the nine pattern slots across the top of the machine. With JEI and AE2 JEI Integration installed, the JEI `+` button can fill the Pattern Encoding Terminal directly.

Once the pattern is installed, the machine publishes it to AE2. Start the craft from an AE terminal; ingredients are delivered into the machine's internal task buffer rather than visible inventory slots.
