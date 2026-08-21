---
navigation:
  title: Processing Patterns
  position: 20
---

# Processing Patterns

The 27 top slots accept encoded processing patterns. A machine only publishes patterns that match its own Mekanism recipe type.

Connected idle machines and all 27 slots are available from AE2's Pattern Access Terminal, grouped by machine name and icon. A machine with accepted processing work is temporarily hidden there because its patterns are locked until the isolated task ledger drains or is returned safely.

For a pattern to be valid:

* Its inputs must describe one or more complete recipe operations.
* All input quantities must use the same whole-number multiplier.
* Every output written into the pattern must use the correct quantity for that multiplier.
* The resource direction must match the machine, such as item to chemical or fluid to two chemicals.

A pattern may be multiplied by a large whole number. The 27 physical pattern slots do not limit a crafting request to 27 operations; accepted work is stored in the internal buffer.

JEI shows the authoritative Mekanism recipe amounts. Changing speed or parallel upgrades does not change those amounts. For recipes that Mekanism expresses as per-tick chemical use, this mod fixes the total material requirement to the amount represented by the recipe.
