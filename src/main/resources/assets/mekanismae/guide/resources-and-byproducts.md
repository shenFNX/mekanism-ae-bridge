---
navigation:
  title: Resources and Byproducts
  position: 40
---

# Resources and Byproducts

The internal ledgers use AE2 resource keys. With Applied Mekanistics present, a single pattern can therefore contain items, fluids, and Mekanism chemicals without converting them to placeholder items.

For multi-output recipes, a processing pattern may intentionally omit a secondary product. The machine still generates every real recipe product and inserts the omitted byproduct into ME storage; AE2 only waits for the outputs declared by the pattern.

If a byproduct is declared, its quantity must be correct. A pattern that claims an impossible quantity is not published to AE2, which prevents a crafting CPU from waiting forever for a result the recipe cannot produce.

The Precision Sawmill settles probabilistic secondary products by exact expected value. Fractional remainder is retained per encoded pattern until it becomes a complete resource, so large batches are deterministic and different patterns remain isolated.
