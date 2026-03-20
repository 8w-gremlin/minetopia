# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this mod does

Minetopia is a Minecraft mod where players build their own villages by placing ItemFrames containing token items. Villagers populate those structures, work, trade, and need defending from raids. Fully coexists with vanilla villagers as separate entity types.

## Build

```bash
JAVA_HOME=../tekshift/jdk21/jdk-21.0.7+6 ./gradlew build
```

The `../tekshift/jdk21` path is just the location of the JDK from a sibling project — it has no relation to this mod.

Output: `build/libs/minetopia-1.0.0.jar`

Run in-game:
```bash
JAVA_HOME=../tekshift/jdk21/jdk-21.0.7+6 ./gradlew runClient
```

## Stack

- **Minecraft 1.21.10** + **NeoForge 21.10.64**
- **NeoForge Gradle plugin:** `net.neoforged.gradle.userdev` version `7.1.21` (requires Gradle 8.14)
- **Java 21** at `../tekshift/jdk21/jdk-21.0.7+6`
- Mod ID: `minetopia` | Package: `com.minetopia` | Author: `8wGremlin`

## Naming conventions

- No "Tek" anywhere — not in class names, file names, comments, or docs
- Base villager class: `MinetopiaVillager`
- Profession classes: `VillagerFarmer`, `VillagerMiner`, `VillagerGuard`, etc.
- Raid entities: `Necromancer`, `SpiritSkull`
- AI goals: `GoalWander`, `GoalSleep`, `GoalEatFood`, `GoalRetrieveFromStorage`, etc.

## Architecture

### Core concept
Village data lives on the **Town Hall token item's DataComponents** (Phase 5). On every world load, `VillageManager` rescans all `ItemFrame` entities in the level, matches their contained items to `VillageStructureType` tokens, and rebuilds the village list. Village state is derived from the world, not stored redundantly.

### Key systems

| System | Files |
|---|---|
| Village state | `village/Village.java`, `village/VillageManager.java` |
| Structure detection | `village/VillageStructureType.java`, `village/VillageStructure.java` |
| Storage / chest scanning | `village/storage/StructureInteriorScanner.java`, `village/storage/ItemDesire.java`, `village/storage/ItemDesireSet.java` |
| Economy | `economy/ProfessionTrade.java`, `economy/VillageEconomy.java` |
| Item registration | `registry/ModItems.java`, `registry/ModEntities.java`, `registry/ModSounds.java` |
| ItemFrame events | `event/VillageEventHandler.java` |
| Villager base (Phase 3+) | `entity/MinetopiaVillager.java` |

### How to add a new villager profession (established pattern)
1. `entity/VillagerXxx.java` extends `MinetopiaVillager` — override `registerGoals()`, `getTradeDefinitions()`, `getTradeName()`, add static `createAttributes()`
2. `ModEntities.java` — add `DeferredHolder` for the new type (copy FARMER pattern)
3. `MinetopiaMod.registerAttributes()` — add `event.put(ModEntities.XXX.get(), VillagerXxx.createAttributes().build())`
4. `ClientSetup.onRegisterRenderers()` — add `event.registerEntityRenderer(ModEntities.XXX.get(), PlaceholderRenderer::new)`
5. `EntityEventHandler.professionTokenToEntityType()` — map `ModItems.TOKEN_XXX` → `ModEntities.XXX`

### Villager AI loop (per tick)
1. Eat from inventory if hungry
2. Retrieve needed items from storage
3. Execute profession job goal
4. Deliver excess back to storage
5. Sleep (ticks 16000–24000) or leisure otherwise

### NeoForge 1.21.10 API — things that differ from older versions

- **SavedData is Codec-based** — no `save(CompoundTag)` / `load(CompoundTag)`. Use `SavedDataType<T>` with a `Codec<T>` and `DimensionDataStorage.computeIfAbsent(SavedDataType<T>)`
- **No `event.hanging` package** — use `EntityLeaveLevelEvent` for ItemFrame destruction
- **`UUIDUtil` is `net.minecraft.core.UUIDUtil`**, not `net.minecraft.util.UUIDUtil`
- **`ItemStack.getTag()` is gone** — use DataComponents: `stack.get(DataComponentType<T>)`
- **`Container` interface has no `addItem()`** — deposit items manually with `getItem`/`setItem` loops; `addItem` is only on `ChestBlockEntity`
- **`BlockState.blocksMotion()`** — returns true for solid/opaque blocks; use `!state.blocksMotion()` for BFS flood-fill walkability
- **`ChestBlock.getConnectedBlockPos(BlockPos, BlockState)`** — static method to get the partner half of a double chest
- **javap: use the 32MB outputs.jar**, not the 58MB one (bundler only). Find with:
  ```bash
  ls -S ~/.gradle/caches/ng_execute/*/outputs.jar | head -3
  # Use the second-largest (≈32 MB); largest is the server bundler with no MC classes
  ```
- **Always verify method names with `javap`** before writing code:
  ```bash
  JAVAP=../tekshift/jdk21/jdk-21.0.7+6/bin/javap.exe
  JAR=~/.gradle/caches/ng_execute/<32MB-hash>/outputs.jar
  "$JAVAP" -classpath "$JAR" fully.qualified.ClassName
  ```

## Phase status

See memory file `project_minetopia_phases.md` for full checklist. Current status:
- **Phase 1** COMPLETE — mod loads, all tokens registered
- **Phase 2** COMPLETE — village detection, VillageManager SavedData, ItemFrame events
- **Phase 3** COMPLETE — MinetopiaVillager base, VillagerFarmer, hunger/sleep AI
- **Phase 4** COMPLETE — StructureInteriorScanner, ItemDesire/ItemDesireSet, GoalRetrieveFromStorage, GoalDeliverToStorage; farmer retrieves seeds + hoe, delivers harvest
- **Phase 5** COMPLETE — VillageEconomy (4000-tick demand refresh), ProfessionTrade, MinetopiaVillager implements Merchant (vanilla trade screen reused), VillagerFarmer sells produce/buys seeds when low stock
