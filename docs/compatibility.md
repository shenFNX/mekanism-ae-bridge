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
| ModDevGradle | `2.0.143` |

## Local development jars

The jars under `libs/` are development/runtime inputs for this first compatibility spike. Their SHA-1 values are:

| File | SHA-1 |
| --- | --- |
| `Mekanism-1.21.1-10.7.19.85.jar` | `b78945c40cfe7640408f3fd1e44da385a8c8b805` |
| `appliedenergistics2-19.2.17.jar` | `49c18d6a4af487957d7e5a6ad5dcbf71090b8e14` |
| `Applied-Mekanistics-1.6.3.jar` | `bec4a47269ec23bca2329742e13409bfde69c5c3` |
| `guideme-21.1.17.jar` | `060e374f578db694a0b8f3ad409bd424cae86359` |

Applied Mekanistics metadata confirms the runtime IDs `appmek`, `ae2`, and `mekanism`. AE2 requires `guideme` for this version.
