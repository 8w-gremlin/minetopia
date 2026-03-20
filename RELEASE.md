# Minetopia — Release Notes

Versioning: MAJOR.MINOR.PATCH — PATCH and MINOR roll 0–99 before incrementing the next digit.

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
