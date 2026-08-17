---
navigation:
  title: Task Buffer and Isolation
  position: 30
---

# Task Buffer and Pattern Isolation

These machines use GTNH-style pre-delivery. AE2 can dispatch many complete operations from a crafting CPU at once, up to the configured task-buffer limit. Parallel upgrades control how quickly buffered work is processed; they do not set the delivery depth.

Every accepted task is recorded against its encoded pattern and its exact input/output ratios. Materials belonging to different patterns never share a processing ledger, even when the recipes use the same item with different chemicals.

While a task still has complete inputs, the machine keeps processing that pattern. It changes to another queued pattern only after the current task can no longer continue and its ready products have been handled. This prevents rapid round-robin switching between recipes.

The return button attempts to insert buffered inputs, finished products, and queued resources back into ME storage. If the network cannot accept everything, the remaining resources stay in the machine and it enters a protected state rather than deleting them.
