package com.kragnoth.workbenchwisdom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

/** Applies the recipe changes (stonecutter, slabs and stairs back, Reforging Bench upgrades and salvage) to a freshly (re)loaded recipe manager. */
public final class RecipeChanges {
    private RecipeChanges() {}

    public static void apply(RecipeManager manager) {
        try {
            List<RecipeHolder<?>> recipes = new ArrayList<>(manager.getRecipes());
            if (Config.FAMILY_CUTTING.get()) {
                recipes.addAll(FamilyCutting.generate(recipes)); // before the tweaks, so these are reversed too
            }
            recipes = StonecutterTweaks.apply(recipes);
            if (Config.BACK_TO_BLOCKS.get()) {
                recipes = new ArrayList<>(recipes);
                recipes.addAll(BackToBlocks.generate(recipes));
            }
            if (Config.UPGRADES.get()) {
                Set<ResourceLocation> ids = new HashSet<>();
                recipes.forEach(h -> ids.add(h.id()));
                recipes = new ArrayList<>(recipes);
                recipes.addAll(TierUpgrades.generate(ids, recipes));
            }
            if (Config.SALVAGE.get()) {
                recipes = new ArrayList<>(recipes);
                recipes.addAll(Salvage.generate(recipes));
            }
            manager.replaceRecipes(recipes);
        } catch (RuntimeException e) {
            WorkbenchWisdom.LOG.error("Couldn't change the recipes; they're left as the datapacks made them", e);
        }
    }
}
