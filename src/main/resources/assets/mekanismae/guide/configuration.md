---
navigation:
  title: Configuration
  position: 70
---

# Configuration

Open the mod configuration screen, or edit `config/mekanismae-server.toml`. To give one save different values, copy that file to `saves/<world>/serverconfig/mekanismae-server.toml` and edit the copy. When no per-world copy exists, the global file is used.

Available settings include:

* AE2 channel requirement, idle AE power, and redstone pause behavior.
* FE capacity, maximum FE input, energy per operation, and processing duration.
* Maximum pre-delivered operations and machine-specific base operations per cycle.
* Complete 0-8 card curves for speed and parallel throughput.
* Basic through Ultimate curves plus optional Mekanism Extras Absolute, Supreme, Cosmic, and Infinite curves for parallel throughput, energy capacity, FE input, and buffer capacity.
* Shared machine defaults plus optional per-machine energy, time, and buffer overrides.

Configuration changes apply after the world is reloaded. Lowering a buffer limit never deletes accepted work; the machine simply stops accepting more until usage falls below the new limit. Lowering energy capacity clamps stored energy to the new capacity.
