# Workbench Wisdom

A crafting quality-of-life mod for NeoForge 1.21.1 by Kragnoth. Everything works across every mod you have installed:
the mod reads the recipes, tags and items that are loaded and works out what to do, with no per-mod lists to keep
up to date. It goes on the server and every client. JEI support is built in (optional).

## The Reforging Bench
Crafted from a smithing table, a grindstone and a block of iron (shapeless). Put an item on the left, and on the
right:

- **The next tier's material: an upgrade.** The item becomes the next tier up and **keeps its enchantments**, name and
  wear. It costs as much material as crafting the new item takes (3 diamonds for a pickaxe, 8 for a chestplate, 1 for a
  shovel), read from the crafting recipe itself. With too little, the bench says how many more it needs. A perfectly
  enchanted iron pickaxe and 3 diamonds make that pickaxe in diamond. Diamond to netherite stays the smithing table's
  (template and all).
- **Another tool or armour piece: an enchantment transfer.** The item gets the second one's enchantments, name, trim,
  dye colour and the rest of its data; where both have something, the second's wins. The second is used up. It costs
  experience like the anvil (each enchantment's anvil cost times its level, plus how often the items have been worked)
  and counts as a rework next time. **There's no "too expensive"**: a player with the levels can always do it. Tools
  pair with tools and armour with armour, any material and any mod. Durability stays the item's own.
- **Nothing: salvage.** The item breaks back down into what it's made of:
  - **Gear** gives back two thirds of the material crafting it took (`salvage.returns`), less for worn gear in
    proportion: a diamond pickaxe gives 2 diamonds, an iron chestplate 5 iron, a half-worn one 3. Enchantments come
    back as experience (half of what they're worth, curses nothing). Netherite gear gives one netherite ingot while
    it's at least half whole (`salvage.smithedGear`); the template and the diamond gear underneath stay lost.
  - **Blocks made from one other block** go back into it at the same two-thirds rate, a whole stack at a time:
    slabs, stairs, walls and polished, chiseled and brick variants back to their stone, panes back to glass, carpets
    to wool, and every mod's catwalks, panels and trims cut from andesite, iron blocks and so on. 64 stone slabs give
    21 stone (using 63 of them). The price is always the cheapest recipe that makes the item, so salvage can never
    give back more than making it cost, however it was made.
  - Left alone: things the game already turns back (iron blocks into ingots, ingots into nuggets), blocks made from
    items (bricks from brick, doors from ingots), and anything in the `workbenchwisdom:no_salvage` item tag.

**Which gear counts:** anything worn in an armour slot that wears out or takes enchantments is armour; everything
else that wears out and takes enchantments (tools, swords, bows, tridents, maces, shields, shears, fishing rods,
any mod's weapons) is a tool. Gear made unbreakable by a "no durability" mod still counts.

**Upgrade paths come from the game itself**, so they fit whatever mods the pack has. Each tool or armour piece
goes up to the next tier of its kind, for the new tier's repair material (what the anvil mends it with):
- **Tools** rank by what they can mine (iron ore, diamond ore, obsidian): wood and gold → stone → iron → diamond.
  Pickaxes, axes, shovels, hoes and swords are grouped by what they are, any other tiered tool by the last word of its
  name (every mod's `_paxel` together).
- **Armour** ranks by its material across the whole set: total protection, then toughness, knockback resistance and
  durability. In vanilla that's leather → gold → chainmail → iron → diamond. A lone piece like the turtle shell isn't
  a step on the ladder, but upgrades onward itself.
- Where several mods have gear at the next tier, the bench offers the one from the same mod first, then vanilla's.
  Netherite stays the smithing table's.

Packs can override any of it with datapack data (see below).

Gear built from parts (Tinkers' Construct, Silent Gear) can't be upgraded or salvaged, but can still trade
enchantments.

**`config/workbenchwisdom-rules.txt`** lists every upgrade and salvage the bench worked out, rewritten on each world
load and `/reload`.

## Block families
A built-in datapack, **Workbench Wisdom: block families**, is written from the item registry when the world loads, so
it covers every mod installed. It's on by default and can be turned off like any datapack.

- **Stonecutter:** any block with `_stairs`, `_slab` or `_wall` variants cuts into them (`stone_bricks` →
  `stone_brick_stairs`, `quartz_block` → `quartz_slab`), and planks cut into their whole wood set: stairs, slabs,
  fences, fence gates, doors, trapdoors, buttons, pressure plates and signs. Cuts the stonecutter already has aren't
  listed twice.
- **Crafting:** two slabs side by side make their block back, four stairs in a square make three blocks. Only where
  the piece really is made from that block, and never where another recipe already uses that arrangement (two stone
  brick slabs stacked still make chiseled stone bricks).

## JEI
Upgrades (with material counts), salvage (the smallest batch that gives something back, and what a full stack gives)
and example transfers, with the bench as the catalyst. Block-family recipes that don't apply are hidden. JEI shows
exactly what the server will do: it reads the same rules the server sends.

## Config
`config/workbenchwisdom-common.toml` (also in the mod list's config screen):

| Setting | Default | |
|---|---|---|
| `transfer.enabled` | true | Enchantment transfers |
| `transfer.costsExperience` | true | Off: transfers are free |
| `transfer.carryDamage` | false | Carry the second item's wear over too |
| `upgrades.enabled` | true | Tier upgrades |
| `upgrades.automatic` | true | Work out upgrades for gear the upgrade paths don't name |
| `salvage.gear` | true | Salvage tools and armour |
| `salvage.blocks` | true | Salvage blocks made from one other block |
| `salvage.returns` | 0.67 | How much of what an item cost salvage gives back |
| `salvage.experience` | true | Enchantments give experience back |
| `salvage.smithedGear` | true | Netherite gear salvages to one netherite ingot |
| `blocks.familyCutting` | true | Block-family stonecutting |
| `blocks.slabsAndStairsToBlocks` | true | Slabs and stairs back to blocks |

## For pack makers
Everything else is datapack data, so it can ship with a pack and changes on `/reload`.

- **Upgrade paths** (overrides; none ship with the mod), `data/<namespace>/workbenchwisdom/upgrade_paths/*.json`:
  ```json
  { "materials": {
      "copper": [ { "to": "bronze", "ingredient": { "tag": "c:ingots/bronze" } } ],
      "mekasuit": []
  } }
  ```
  Gear is matched by name, `<material>_<type>`, and named gear follows these paths instead of the game's tiers. An
  empty list stops a line: that material doesn't upgrade at all. Entries naming items from mods that aren't installed
  are skipped. A later pack's entry for a material replaces an earlier one.
- **Gear types**, `data/<namespace>/workbenchwisdom/gear_types/*.json`: `{"tools": {"paxel": 7}, "armor": {"ring": 1}}`.
  Extra type names for upgrade paths to match by (vanilla's tool kinds and armour slots are built in), each with the
  material cost to charge when the new item has no crafting recipe to read it from.
- **Upgrade recipes**, `"type": "workbenchwisdom:upgrade"`: `base`, `addition` (ingredients), `count`, `result`.
  These come before anything the bench works out for the same item.
- **Salvage recipes**, `"type": "workbenchwisdom:salvage"`: `input` (ingredient), `result` (item stack, given per
  item, `salvage.returns` not applied).
- **`workbenchwisdom:no_salvage`** item tag: never salvaged.
- Any block-family recipe can be replaced or removed by a datapack with the same id
  (`workbenchwisdom:cutting/<namespace>/<result>_from_<input>`, `workbenchwisdom:back_to_block/<namespace>/<piece>`).

## Building
`./gradlew build`; the jar is in `build/libs`. Every build bumps `mod_version` in `gradle.properties` by one
(2.0.0 → 2.0.1), each place rolling over at 10 into the next (2.0.9 → 2.1.0, 2.9.9 → 3.0.0); the jar and the version
the game shows use it. Commit `gradle.properties` with your changes so the count carries on from any machine.
`./gradlew runGameTestServer` runs the GameTests.

MIT licensed.
