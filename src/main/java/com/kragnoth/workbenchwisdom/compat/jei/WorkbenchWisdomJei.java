package com.kragnoth.workbenchwisdom.compat.jei;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.bench.EnchantTransfer;
import com.kragnoth.workbenchwisdom.recipe.BackToBlockRecipe;
import com.kragnoth.workbenchwisdom.recipe.FamilyCutRecipe;
import com.kragnoth.workbenchwisdom.rules.BenchRules;
import com.kragnoth.workbenchwisdom.rules.ClientRules;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * JEI: the Reforging Bench's upgrades and salvage, straight from the rules the server sent (so JEI shows exactly what
 * the bench does), a few examples of enchantment transfers, and the block-family recipes cleaned up: family cuts the
 * stonecutter already had, and slab-and-stair recipes that don't apply, are hidden.
 */
@JeiPlugin
public class WorkbenchWisdomJei implements IModPlugin {
    static final RecipeType<BenchRules.Upgrade> UPGRADES = RecipeType.create(WorkbenchWisdom.MODID, "upgrade", BenchRules.Upgrade.class);
    static final RecipeType<BenchRules.Salvage> SALVAGES = RecipeType.create(WorkbenchWisdom.MODID, "salvage", BenchRules.Salvage.class);
    static final RecipeType<TransferExample> TRANSFERS = RecipeType.create(WorkbenchWisdom.MODID, "enchantment_transfer", TransferExample.class);

    /** One transfer to show: the item, the one giving its enchantments, what comes out and what it costs. */
    public record TransferExample(ItemStack base, ItemStack donor, ItemStack result, int cost) {}

    @Override
    public ResourceLocation getPluginUid() {
        return WorkbenchWisdom.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        var gui = reg.getJeiHelpers().getGuiHelper();
        reg.addRecipeCategories(new UpgradeCategory(gui), new TransferCategory(gui), new SalvageCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        BenchRules rules = ClientRules.get();
        reg.addRecipes(UPGRADES, rules.upgradeList());
        reg.addRecipes(SALVAGES, rules.salvageList());
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            reg.addRecipes(TRANSFERS, examples(level.registryAccess()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(WorkbenchWisdom.REFORGING_BENCH_ITEM.get()), UPGRADES, TRANSFERS, SALVAGES);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        List<RecipeHolder<StonecutterRecipe>> cuts = new ArrayList<>();
        List<RecipeHolder<CraftingRecipe>> crafts = new ArrayList<>();
        for (RecipeHolder<?> h : level.getRecipeManager().getRecipes()) {
            if (h.value() instanceof FamilyCutRecipe c && !c.isNeeded(level)) {
                cuts.add((RecipeHolder) h);
            } else if (h.value() instanceof BackToBlockRecipe b && !b.isValid(level)) {
                crafts.add((RecipeHolder) h);
            }
        }
        runtime.getRecipeManager().hideRecipes(RecipeTypes.STONECUTTING, cuts);
        runtime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING, crafts);
    }

    private static List<TransferExample> examples(RegistryAccess ra) {
        Registry<Enchantment> reg = ra.registryOrThrow(Registries.ENCHANTMENT);
        return List.of(
                example(Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE, reg, Enchantments.EFFICIENCY, 5, Enchantments.UNBREAKING, 3, Enchantments.FORTUNE, 3),
                example(Items.DIAMOND_SWORD, Items.IRON_SWORD, reg, Enchantments.SHARPNESS, 5, Enchantments.LOOTING, 3, Enchantments.UNBREAKING, 3),
                example(Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE, reg, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1));
    }

    @SuppressWarnings("unchecked")
    private static TransferExample example(Item baseItem, Item donorItem, Registry<Enchantment> reg, Object... enchants) {
        ItemStack base = new ItemStack(baseItem);
        ItemStack donor = new ItemStack(donorItem);
        for (int i = 0; i + 1 < enchants.length; i += 2) {
            Holder<Enchantment> h = reg.getHolderOrThrow((ResourceKey<Enchantment>) enchants[i]);
            donor.enchant(h, (Integer) enchants[i + 1]);
        }
        return new TransferExample(base, donor, EnchantTransfer.result(base, donor), EnchantTransfer.cost(base, donor));
    }
}
