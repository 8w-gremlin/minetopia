# Minetopia — Release Notes

Versioning: MAJOR.MINOR.PATCH — PATCH and MINOR roll 0–99 before incrementing the next digit.

---

## [1.0.2] — 2026-03-21

### Fixed
- Summoning a villager token with no village nearby now shows an error message instead of silently doing nothing
- Farmer no longer tills random dirt/grass patches around the world — `GoalTillSoil` now only targets soil blocks that have a water source within 4 blocks (standard irrigation distance)
- Farmer no longer plants crops on unirrigated farmland — `GoalFarmCrops` now requires water within 4 blocks of any farmland it targets; search radius expanded to 20 blocks so the farmer can reach the field from anywhere in the village

---

## [1.0.1] — 2026-03-21

### Fixed
- All 7 professions with 128×64 textures (Bard, Captain, Chef, Cleric, Druid, Guard, Rancher) cropped to standard 64×64 player-skin format — previously rendered broken in-game

### Added
- `giveStartingItems()` implemented for all remaining professions: Miner (pickaxe + torches), Lumberjack (axe), Blacksmith (iron ingots + coal), Chef (coal + beef), Rancher (wheat), Cleric (glass bottles + nether wart + blaze powder), Druid (bone meal), Teacher (books + sugar cane)
- `getWorkToolClass()` implemented for Miner (`PickaxeItem`) and Lumberjack (`AxeItem`) — tools now auto-equip to mainhand on spawn

---

## [1.0.0] — 2026-03-21

### Initial release

Full feature set at launch:

**Village system**
- Place ItemFrame tokens to define village structures (Town Hall, House, Barracks, Farm, Mine, etc.)
- VillageManager detects and persists villages via SavedData (survives world reload)
- Structure validation on token placement — minimum floor space enforced per structure type
- Sneak + right-click any structure token to rescan interior (update chests/beds without reload)
- Village border overlay and `/minetopia` commands (`list`, `info`, `recall`, `version`)

**Villagers — 17 professions**
- Farmer, Miner, Lumberjack, Blacksmith, Butcher, Chef, Rancher, Cleric, Druid, Enchanter, Bard, Teacher, Guard, Captain, Mayor, Nitwit, Nomad
- Each villager has a name (e.g. "Sarah the Farmer"), custom skin, and profession-specific AI goals
- Hunger/eat cycle, sleep in beds at night, wander within village bounds, flee monsters
- Guards and Captains fight hostile mobs (melee + ranged bow attack, auto-equip armour)
- Mayor stays awake 24/7, sells all profession tokens for emeralds, respawns after death
- Population growth: VillagerChild spawns when beds available and average happiness ≥ 30

**Economy**
- Each profession has trade definitions; demand refreshes every 4000 ticks
- Players open vanilla trade screen by right-clicking any villager
- VillageEconomy tracks stock levels; prices shift with supply/demand

**Storage & item flow**
- Villagers retrieve required items from nearby chests and deliver excess back
- Per-structure chest routing: each profession works from their building's chests first
- Farmers till soil, plant seeds, harvest crops; Miners dig at shaft blocks; Lumberjacks chop trees; etc.

**Textures & rendering**
- All 36 villager skins (18 professions × male/female) are standard 64×64 player-skin format
- Enchantment glow applied to registered structure tokens in ItemFrames

---
