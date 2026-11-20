package com.kragnoth.workbenchwisdom.bench;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.gear.Gear;
import com.kragnoth.workbenchwisdom.rules.BenchRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Reforging Bench: an item on the left, and on the right
 * <ul>
 *   <li>the next tier's material: an upgrade that keeps its enchantments;</li>
 *   <li>another tool or armour piece: an enchantment transfer, for experience;</li>
 *   <li>nothing: salvage back into what it's made of.</li>
 * </ul>
 * What happens is a {@link BenchAction}; the server's is the one that counts, and its mode and numbers are synced to
 * the screen.
 */
public class ReforgingMenu extends ItemCombinerMenu {
    public static final int BASE = 0;
    public static final int ADDITION = 1;
    public static final int RESULT = 2;

    private final Level level;
    private final DataSlot mode = DataSlot.standalone();
    /** Transfer: levels. Upgrade short of material: how many more. Salvage: experience back. */
    private final DataSlot number = DataSlot.standalone();
    /** Salvage: how many of the left item it takes. */
    private final DataSlot uses = DataSlot.standalone();
    private BenchAction action = BenchAction.NONE;

    public ReforgingMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public ReforgingMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(WorkbenchWisdom.REFORGING_MENU.get(), id, inv, access);
        this.level = inv.player.level();
        addDataSlot(mode);
        addDataSlot(number);
        addDataSlot(uses);
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

    @Override
    public void createResult() {
        action = BenchAction.of(inputSlots.getItem(BASE), inputSlots.getItem(ADDITION), BenchRules.of(level));
        mode.set(action.mode().ordinal());
        number.set(switch (action.mode()) {
            case TRANSFER -> action.levels();
            case UPGRADE -> action.missing();
            case SALVAGE -> action.xp();
            case NONE -> 0;
        });
        uses.set(action.baseUse());
        resultSlots.setItem(0, action.result().copy());
        broadcastChanges();
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack) {
        if (!hasStack || action.result().isEmpty()) {
            return false;
        }
        return action.mode() != BenchAction.Mode.TRANSFER || player.hasInfiniteMaterials() || player.experienceLevel >= action.levels();
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        BenchAction done = action; // shrinking the slots works the result out again
        stack.onCraftedBy(player.level(), player, stack.getCount());
        if (done.mode() == BenchAction.Mode.TRANSFER && !player.hasInfiniteMaterials()) {
            player.giveExperienceLevels(-done.levels());
        }
        shrink(ADDITION, done.addUse());
        shrink(BASE, done.baseUse());
        access.execute((lvl, pos) -> {
            lvl.levelEvent(switch (done.mode()) {
                case UPGRADE -> 1044;  // the smithing table
                case TRANSFER -> 1030; // the anvil
                default -> 1042;       // the grindstone
            }, pos, 0);
            if (done.xp() > 0 && lvl instanceof ServerLevel sl) {
                ExperienceOrb.award(sl, Vec3.atCenterOf(pos), done.xp());
            }
        });
    }

    private void shrink(int slot, int n) {
        if (n <= 0) {
            return;
        }
        ItemStack s = inputSlots.getItem(slot);
        s.shrink(n);
        inputSlots.setItem(slot, s);
    }

    @Override
    public int getSlotToQuickMoveTo(ItemStack stack) {
        boolean leftFirst = Gear.isGear(stack) || BenchRules.of(level).hasRules(stack.getItem());
        return inputSlots.getItem(BASE).isEmpty() && leftFirst ? BASE : ADDITION;
    }

    public BenchAction.Mode mode() {
        return BenchAction.Mode.values()[Math.max(0, Math.min(mode.get(), 3))];
    }

    public int number() {
        return number.get();
    }

    public int uses() {
        return uses.get();
    }

    /** This side's own reading of the bench (on the client: for naming an upgrade's material). */
    public BenchAction action() {
        return action;
    }
}
