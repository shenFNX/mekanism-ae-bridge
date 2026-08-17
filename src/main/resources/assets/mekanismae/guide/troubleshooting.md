---
navigation:
  title: Troubleshooting
  position: 90
---

# Troubleshooting

## AE2 Sees the Machine, but Energy Is Zero

The AE cable only powers the network node. Connect a separate FE source, such as a Mekanism Universal Cable, and inspect the energy tab for stored FE and input rate.

## The Pattern Is Not Visible to AE2

Confirm that it is an encoded processing pattern, is installed in a top slot, and describes a recipe belonging to this exact machine. Check that every input uses the same whole-number multiplier and every declared output quantity matches the real recipe.

## A Large Multiplied Pattern Will Not Dispatch

The multiplier must represent complete operations. Also check the configured task-buffer limit and available CPU ingredients. The nine pattern slots are not a nine-operation dispatch limit.

## Crafting Is Waiting

Inspect the status panel and Jade for missing FE, redstone pause, disabled/offline ME, a full buffer, blocked ME storage, or a protected processing fault. Clear storage space or network access, then use the return button if the job needs to be cancelled.

## Two Recipes Use Similar Inputs

This is safe. Each encoded pattern has a separate task ledger containing its exact item, fluid, chemical, and output ratios. Resources are not pooled merely because they have the same type.

## Returned Resources Do Not Fit

Nothing is discarded. The machine retains resources that ME storage cannot accept and keeps its fault protection active until insertion succeeds.
