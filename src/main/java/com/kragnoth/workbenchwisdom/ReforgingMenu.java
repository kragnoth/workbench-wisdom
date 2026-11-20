package com.kragnoth.workbenchwisdom;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Reforging Bench: an item on the left, something added on the right.
 * <ul>
 *   <li>The next tier's material ({@link ReforgingRecipe}): the item upgraded, keeping its enchantments, for as much
 *       material as crafting the new item takes.</li>
 *   <li>Another enchanted tool or armour piece ({@link EnchantTransfer}): its enchantments onto the item, for
 *       experience.</li>
 *   <li>Nothing: the item salvaged into some of its material ({@link SalvageRecipe}), with experience back for its
 *       enchantments.</li>
 * </ul>
 */
public class ReforgingMenu extends ItemCombinerMenu {
    public static final int BASE = 0;
    public static final int ADDITION = 1;
    public static final int RESULT = 2;

    /** What's showing: 0 nothing, 1 an upgrade, 2 a transfer. */
    public static final int NONE = 0;
    public static final int UPGRADE = 1;
    public static final int TRANSFER = 2;
    public static final int SALVAGE = 3;

    private final Level level;
    private final DataSlot mode = DataSlot.standalone();
    /** Levels a transfer costs; for an upgrade short of material, how many more it needs (negative). */
    private final DataSlot cost = DataSlot.standalone();
    private RecipeHolder<ReforgingRecipe> selected;

    public ReforgingMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public ReforgingMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(WorkbenchWisdom.REFORGING_MENU.get(), id, inv, access);
        this.level = inv.player.level();
        addDataSlot(mode);
        addDataSlot(cost);
    }

    @Override
    protected ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
        return ItemCombinerMenuSlotDefinition.create()
                .withSlot(BASE, 27, 47, s -> true)
                .withSlot(ADDITION, 76, 47, s -> true)
                .withResultSlot(RESULT, 134, 47)
                .build();
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(WorkbenchWisdom.REFORGING_BENCH.get());
    }

    private ReforgingInput input() {
        return new ReforgingInput(inputSlots.getItem(BASE), inputSlots.getItem(ADDITION));
    }

    @Override
    public void createResult() {
        selected = null;
        mode.set(NONE);
        cost.set(0);
        ItemStack base = inputSlots.getItem(BASE);
        ItemStack add = inputSlots.getItem(ADDITION);
        resultSlots.setItem(0, ItemStack.EMPTY);
        if (!base.isEmpty() && add.isEmpty() && Config.SALVAGE.get()) {
            for (RecipeHolder<SalvageRecipe> h : level.getRecipeManager().getAllRecipesFor(WorkbenchWisdom.SALVAGE.get())) {
                if (h.value().matches(input(), level)) {
                    mode.set(SALVAGE);
                    cost.set(salvageXp(base));
                    resultSlots.setItem(0, h.value().salvageOf(base));
                    break;
                }
            }
            broadcastChanges();
            return;
        }
        if (base.isEmpty() || add.isEmpty()) {
            broadcastChanges();
            return;
        }
        ReforgingInput in = input();
        if (Config.UPGRADES.get()) {
            List<RecipeHolder<ReforgingRecipe>> all = level.getRecipeManager().getAllRecipesFor(WorkbenchWisdom.REFORGING.get());
            for (RecipeHolder<ReforgingRecipe> h : all) {
                if (h.value().matchesItems(in) && base.getCount() == 1) {
                    selected = h;
                    mode.set(UPGRADE);
                    int need = h.value().count();
                    if (add.getCount() >= need) {
                        resultSlots.setItem(0, h.value().assemble(in, level.registryAccess()));
                        resultSlots.setRecipeUsed(h);
                    } else {
                        cost.set(-(need - add.getCount()));
                    }
                    broadcastChanges();
                    return;
                }
            }
        }
        if (Config.TRANSFER.get() && EnchantTransfer.pairs(base, add)) {
            mode.set(TRANSFER);
            int c = EnchantTransfer.cost(base, add);
            cost.set(c);
            resultSlots.setItem(0, EnchantTransfer.result(base, add)); // no "too expensive": the levels are the only limit
        }
        broadcastChanges();
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack) {
        if (mode.get() == UPGRADE) {
            return selected != null && selected.value().matches(input(), level);
        }
        if (mode.get() == TRANSFER) {
            return player.hasInfiniteMaterials() || player.experienceLevel >= cost.get();
        }
        return mode.get() == SALVAGE && hasStack;
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        // read everything first: shrinking a slot works the result out again, which clears these
        int what = mode.get();
        int levels = cost.get();
        RecipeHolder<ReforgingRecipe> recipe = selected;
        stack.onCraftedBy(player.level(), player, stack.getCount());
        if (what == UPGRADE && recipe != null) {
            shrink(ADDITION, recipe.value().count());
            shrink(BASE, 1);
            access.execute((lvl, pos) -> lvl.levelEvent(1044, pos, 0)); // the smithing table's sound
        } else if (what == SALVAGE) {
            shrink(BASE, 1);
            access.execute((lvl, pos) -> {
                lvl.levelEvent(1042, pos, 0); // the grindstone's sound
                if (levels > 0 && lvl instanceof net.minecraft.server.level.ServerLevel sl) {
                    net.minecraft.world.entity.ExperienceOrb.award(sl, net.minecraft.world.phys.Vec3.atCenterOf(pos), levels);
                }
            });
        } else if (what == TRANSFER) {
            if (!player.hasInfiniteMaterials()) {
                player.giveExperienceLevels(-levels);
            }
            shrink(ADDITION, 1);
            shrink(BASE, 1);
            access.execute((lvl, pos) -> lvl.levelEvent(1030, pos, 0)); // the anvil's
        }
    }

    /**
     * Experience points salvaging gives back for its enchantments: half of what they'd take to put on (like the
     * grindstone, without its randomness). Curses give nothing.
     */
    public static int salvageXp(ItemStack gear) {
        if (!Config.SALVAGE_XP.get()) {
            return 0;
        }
        int xp = 0;
        var ench = gear.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (var e : ench.entrySet()) {
            if (!e.getKey().is(net.minecraft.tags.EnchantmentTags.CURSE)) {
                xp += e.getKey().value().getMinCost(e.getIntValue());
            }
        }
        return xp / 2;
    }

    private void shrink(int slot, int n) {
        ItemStack s = inputSlots.getItem(slot);
        if (!s.isEmpty()) {
            s.shrink(n);
            inputSlots.setItem(slot, s);
        }
    }

    @Override
    public int getSlotToQuickMoveTo(ItemStack stack) {
        return inputSlots.getItem(BASE).isEmpty() && (EnchantTransfer.isGear(stack) || isBase(stack)) ? BASE : ADDITION;
    }

    private boolean isBase(ItemStack stack) {
        for (RecipeHolder<ReforgingRecipe> h : level.getRecipeManager().getAllRecipesFor(WorkbenchWisdom.REFORGING.get())) {
            if (h.value().base().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public int mode() {
        return mode.get();
    }

    public int cost() {
        return cost.get();
    }

    /** The upgrade showing, for the screen to name its material. */
    public Optional<ReforgingRecipe> upgrade() {
        return mode.get() == UPGRADE ? level.getRecipeManager().getAllRecipesFor(WorkbenchWisdom.REFORGING.get()).stream()
                .map(RecipeHolder::value).filter(r -> r.matchesItems(input())).findFirst() : Optional.empty();
    }
}
