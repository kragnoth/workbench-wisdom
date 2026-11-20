# Workbench Wisdom

A small crafting quality-of-life mod for NeoForge 1.21.1 by Kragnoth. Server-side data, so it goes on the server and
every client (clients need it for the recipes to show up). One new block, the Reforging Bench. Everything can be turned off in
`config/workbenchwisdom-common.toml`. JEI support is built in (optional).

## The Reforging Bench
One block for carrying gear forward: a smithing table, a grindstone and a block of iron (shapeless). Put the item on
the left and something on the right:

- **The next tier's material: an upgrade.** The item becomes the next tier and **keeps its enchantments**, name and
  wear, for as much material as crafting the new item takes: 3 diamonds for a pickaxe, 8 for a chestplate, 1 for a
  shovel. Too little and the bench says how many more it needs. A perfectly enchanted iron pickaxe and 3 diamonds make
  that pickaxe in diamond; the vanilla smithing table (netherite template and an ingot) takes it on to netherite.
- **Another tool (or armour piece): an enchantment transfer.** The item gets the second one's enchantments, name,
  trim, dye colour and the rest of its data; where both have something, the second's wins. The second is used up.
  It costs experience like the anvil (each enchantment's anvil cost times its level, plus how often the items have
  been worked) and counts as a rework for next time. There's no "too expensive": however much it costs, a player
  with the levels can do it. Tools pair
  with tools and armour with armour, any material either way. Durability stays the item's own: its wear, how much it has, and whether it's unbreakable
  (`transfer.carryDamage` carries the second's over instead).

**Which gear counts:** everything worn in an armour slot that wears out (any mod's armour, elytra) is armour;
everything else that wears out and takes enchantments (tools, swords, bows, tridents, maces, shields, shears,
fishing rods, any mod's weapons) is a tool. Tools pair with tools and armour with armour, from any mods.

**Upgrade paths** come two ways:
- **The chain**, `config/workbenchwisdom-upgrades.json` (written with the defaults the first time), matched by name,
  `<material>_<type>` (`iron_pickaxe`, `refined_obsidian_helmet`): which material upgrades into which, what's added
  (an item or a tag) and how many each kind of item takes (`costs`). The defaults: wood > stone > iron > diamond,
  leather > chainmail > iron, flint > copper > bronze > iron, gold > diamond, plus Mekanism, Applied Energistics 2,
  Ender IO and Just Dire Things materials.
- **Automatically**, for every other tool and armour piece (`upgrades.automatic`): each item goes up to the **next
  tier of its kind**. Pickaxes, axes, shovels, hoes and swords are grouped by what they are, other tools by name ending
  (paxel, hammer...), armour by slot. Tools rank by what they can mine (wood/gold, stone, iron, diamond), armour by
  protection then toughness. Where several mods have something at the next tier, each is offered and the player
  picks. The cost is the new tier's repair material, as many as `costs` says. Gear of the same tier doesn't upgrade
  into each other (that's a transfer), and nothing upgrades into gear the smithing table makes (netherite stays the
  smithing table's, template and all). What was worked out is listed in `config/workbenchwisdom-automatic-upgrades.txt`
  on every load; to change one, give its material a chain entry.

Gear made unbreakable by a "no durability" mod still counts and still transfers and upgrades: nothing here needs
durability.

Gear assembled from parts (Tinkers' Construct, Silent Gear) or one-off items with no tier can't be upgraded, but can
still have enchantments moved onto and off them. Changes apply on `/reload`.

- **Nothing on the right: salvage.** Tools and armour break back down into their material: two thirds of what crafting
  them took (`salvage.returns`), less for worn gear in proportion. A diamond pickaxe gives 2 diamonds, an iron
  chestplate 5 iron ingots, a half-worn one 3. Enchantments are refunded as experience (half what they'd cost on the
  anvil, curses not counted; `salvage.experience`). Gear that would give nothing back (a shovel made of one ingot,
  bows and other gear with no repair material) can't be salvaged. Netherite gear gives back one netherite ingot while
  it's at least half whole; the template and the diamond gear underneath stay lost (`salvage.smithedGear`, on by
  default). Only tools and armour salvage, never other items.

**JEI** shows every upgrade (with its material count), every salvage and examples of transfers, with the bench as
the catalyst.

## Stonecutter
- **One-to-one:** every stonecutter recipe that made more than one item makes one (stone to slabs is 1:1 now).
- **Reversible:** every stonecutter recipe also works backwards (`<recipe id>_reversed`): stairs back to stone, cut
  copper back to a copper block, and the same for every mod's stonecutter recipes.
- **No duplication:** a result isn't made reversible when the crafting table already makes more of it than it takes
  (three stone make six slabs there, so slabs aren't cut back into stone). The log says how many were changed,
  reversed and left alone.
- **Block families:** any block with `_stairs`, `_slab` or `_wall` variants can be cut into them, from every mod
  (`stonecutter.families`). Planks cut into their whole wood set: stairs, slabs, fences, fence gates, doors,
  trapdoors, buttons, pressure plates and signs. All of it reverses too.
- `stonecutter.skip` leaves chosen recipes or whole mods (`"modid:*"`) untouched.

## Crafting
- **Slabs and stairs back to blocks** (`crafting.slabsAndStairsToBlocks`): two slabs side by side make the block, four
  stairs in a square make three. Only for slabs and stairs crafted from a single block, and never where another recipe
  already uses that arrangement (two stone brick slabs stacked still make chiseled stone bricks).

## Building
`./gradlew build`. The jar is in `build/libs`.

MIT licensed.
