package com.kragnoth.workbenchwisdom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

/**
 * Works out what each tool and armour piece salvages into: its repair material (what the anvil mends it with, the
 * same as {@link AutoTiers} reads), times how much of it the item took to make, times {@code salvage.returns}
 * (two thirds by default), rounded down. "How much it took" is counted from its crafting recipe where it has one (so
 * salvage can never give back more than crafting used), else the per-type {@code costs}. Gear made at the smithing
 * table (netherite) salvages to exactly one of what the smithing table added (a netherite ingot): the template and the
 * diamond gear it was made from stay lost, so it never gives back more than went in.
 */
public final class Salvage {
    private Salvage() {}

    static List<RecipeHolder<?>> generate(List<RecipeHolder<?>> recipes) {
        TierUpgrades.Chain chain = TierUpgrades.load();
        List<String> named = new ArrayList<>(chain.toolTypes());
        named.sort(java.util.Comparator.comparingInt(String::length).reversed());
        java.util.Map<Item, Ingredient> smithed = new java.util.HashMap<>();
        List<CraftingRecipe> crafting = new ArrayList<>();
        Set<ResourceLocation> ids = new HashSet<>();
        for (RecipeHolder<?> h : recipes) {
            ids.add(h.id());
            try {
                if (h.value() instanceof SmithingTransformRecipe s) {
                    Ingredient added = addition(s);
                    if (added != null) {
                        smithed.putIfAbsent(s.getResultItem(RegistryAccess.EMPTY).getItem(), added);
                    }
                } else if (h.value().getType() == RecipeType.CRAFTING && h.value() instanceof CraftingRecipe c) {
                    crafting.add(c);
                }
            } catch (RuntimeException ignored) {
                // a result that needs the real registries
            }
        }
        double returns = Config.SALVAGE_RETURNS.get();
        List<RecipeHolder<?>> out = new ArrayList<>();
        for (AutoTiers.Entry e : AutoTiers.scan(named)) {
            Item item = e.item();
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(WorkbenchWisdom.MODID, "salvage/" + key.getNamespace() + "/" + key.getPath());
            Ingredient added = smithed.get(item);
            if (added != null) {
                if (!Config.SALVAGE_SMITHED.get()) {
                    continue;
                }
                ItemStack[] one = added.getItems();
                Integer made = madeFrom(crafting, item, added);
                if (one.length > 0 && (made == null || made >= 1) && ids.add(id)) {
                    out.add(new RecipeHolder<>(id, new SalvageRecipe(Ingredient.of(item), one[0].copyWithCount(1))));
                }
                continue;
            }
            if (e.material().isEmpty()) {
                continue;
            }
            ItemStack[] mats = e.material().getItems();
            if (mats.length == 0) {
                continue;
            }
            Integer made = madeFrom(crafting, item, e.material());
            if (made != null && made == 0) {
                continue; // crafted from something else entirely: no telling what it's worth
            }
            int took = made != null ? made : chain.cost(AutoTiers.costKey(e));
            int back = (int) Math.floor(took * returns);
            if (back <= 0) {
                continue;
            }
            if (ids.add(id)) {
                out.add(new RecipeHolder<>(id, new SalvageRecipe(Ingredient.of(item), mats[0].copyWithCount(back))));
            }
        }
        WorkbenchWisdom.LOG.info("Reforging: {} pieces of gear can be salvaged", out.size());
        return out;
    }

    /** What a smithing transform adds (its third slot); the recipe keeps it to itself, so read the field. */
    private static Ingredient addition(SmithingTransformRecipe r) {
        try {
            java.lang.reflect.Field f = SmithingTransformRecipe.class.getDeclaredField("addition");
            f.setAccessible(true);
            return (Ingredient) f.get(r);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /**
     * How many of {@code material} the cheapest crafting recipe for {@code item} takes; 0 if it's crafted but not
     * from that material; null if nothing crafts it.
     */
    private static Integer madeFrom(List<CraftingRecipe> crafting, Item item, Ingredient material) {
        Integer best = null;
        for (CraftingRecipe c : crafting) {
            ItemStack result;
            try {
                result = c.getResultItem(RegistryAccess.EMPTY);
            } catch (RuntimeException ex) {
                continue;
            }
            if (!result.is(item)) {
                continue;
            }
            int n = 0;
            for (Ingredient ing : c.getIngredients()) {
                ItemStack[] options = ing.getItems();
                for (ItemStack o : options) {
                    if (material.test(o)) {
                        n++;
                        break;
                    }
                }
            }
            n /= Math.max(1, result.getCount());
            if (best == null || n < best) {
                best = n;
            }
        }
        return best;
    }
}
