package com.kragnoth.workbenchwisdom.compat.jei;

import com.kragnoth.workbenchwisdom.EnchantTransfer;
import com.kragnoth.workbenchwisdom.ReforgingRecipe;
import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** JEI: the Reforging Bench's upgrades, and a few examples of what an enchantment transfer does. */
@JeiPlugin
public class WorkbenchWisdomJei implements IModPlugin {
    static final RecipeType<RecipeHolder<ReforgingRecipe>> UPGRADES = RecipeType.createRecipeHolderType(
            ResourceLocation.fromNamespaceAndPath(WorkbenchWisdom.MODID, "reforging"));
    static final RecipeType<RecipeHolder<com.kragnoth.workbenchwisdom.SalvageRecipe>> SALVAGES = RecipeType.createRecipeHolderType(
            ResourceLocation.fromNamespaceAndPath(WorkbenchWisdom.MODID, "salvage"));
    static final RecipeType<TransferExample> TRANSFERS = RecipeType.create(WorkbenchWisdom.MODID, "enchantment_transfer", TransferExample.class);

    /** One transfer to show: the item, the one giving its enchantments, what comes out and what it costs. */
    public record TransferExample(ItemStack base, ItemStack donor, ItemStack result, int cost) {}

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(WorkbenchWisdom.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        var gui = reg.getJeiHelpers().getGuiHelper();
        reg.addRecipeCategories(new UpgradeCategory(gui), new TransferCategory(gui), new SalvageCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        reg.addRecipes(UPGRADES, List.copyOf(level.getRecipeManager().getAllRecipesFor(WorkbenchWisdom.REFORGING.get())));
        reg.addRecipes(TRANSFERS, examples(level.registryAccess()));
        reg.addRecipes(SALVAGES, List.copyOf(level.getRecipeManager().getAllRecipesFor(WorkbenchWisdom.SALVAGE.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        ItemStack bench = new ItemStack(WorkbenchWisdom.REFORGING_BENCH_ITEM.get());
        reg.addRecipeCatalyst(bench, UPGRADES, TRANSFERS, SALVAGES);
    }

    private static List<TransferExample> examples(RegistryAccess ra) {
        var reg = ra.registryOrThrow(Registries.ENCHANTMENT);
        List<TransferExample> out = new ArrayList<>();
        out.add(example(new ItemStack(Items.DIAMOND_PICKAXE), Items.IRON_PICKAXE, reg, Enchantments.EFFICIENCY, 5, Enchantments.UNBREAKING, 3,
                Enchantments.FORTUNE, 3));
        out.add(example(new ItemStack(Items.DIAMOND_SWORD), Items.IRON_SWORD, reg, Enchantments.SHARPNESS, 5, Enchantments.LOOTING, 3,
                Enchantments.UNBREAKING, 3));
        out.add(example(new ItemStack(Items.DIAMOND_CHESTPLATE), Items.IRON_CHESTPLATE, reg, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1));
        return out;
    }

    private static TransferExample example(ItemStack base, net.minecraft.world.item.Item donorItem,
            net.minecraft.core.Registry<Enchantment> reg, Object... enchants) {
        ItemStack donor = new ItemStack(donorItem);
        for (int i = 0; i + 1 < enchants.length; i += 2) {
            @SuppressWarnings("unchecked")
            ResourceKey<Enchantment> key = (ResourceKey<Enchantment>) enchants[i];
            Holder<Enchantment> h = reg.getHolderOrThrow(key);
            donor.enchant(h, (Integer) enchants[i + 1]);
        }
        return new TransferExample(base, donor, EnchantTransfer.result(base, donor), EnchantTransfer.cost(base, donor));
    }
}
