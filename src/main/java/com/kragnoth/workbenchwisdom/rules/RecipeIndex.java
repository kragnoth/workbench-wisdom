package com.kragnoth.workbenchwisdom.rules;

import com.kragnoth.workbenchwisdom.recipe.BackToBlockRecipe;
import com.kragnoth.workbenchwisdom.recipe.FamilyCutRecipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;

/**
 * What the loaded recipes say about how things are made, worked out once per recipe set (each reload makes a new
 * {@link RecipeManager}, so the cache follows it) and on each side separately.
 * <ul>
 *   <li><b>Single-material sources:</b> every stonecutter recipe, and every crafting recipe whose ingredients are all
 *       the same one item, as "{@code inputs} of this make {@code outputs} of that".</li>
 *   <li>Crafting recipes by result, for how much of a material a piece of gear took.</li>
 *   <li>Smithing transforms: what they add, and which upgrades they already do.</li>
 *   <li>Which stonecutter cuts exist without Workbench Wisdom, so its family cuts never double one.</li>
 * </ul>
 * Recipes Workbench Wisdom adds back to the crafting table (slabs to blocks) aren't sources: they'd make every block
 * "made from slabs".
 */
public final class RecipeIndex {
    /** {@code inputs} of {@code input} make {@code outputs} of the item; {@code cut}: at the stonecutter. */
    public record Source(Item input, int inputs, int outputs, boolean cut) {
        public double cost() {
            return inputs / (double) outputs;
        }
    }

    private record Pair(Item from, Item to) {}

    private static final Map<RecipeManager, RecipeIndex> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    public static RecipeIndex of(Level level) {
        return CACHE.computeIfAbsent(level.getRecipeManager(), m -> new RecipeIndex(m, level.registryAccess()));
    }

    private final Map<Item, List<Source>> sources = new HashMap<>();
    private final Map<Item, List<CraftingRecipe>> craftedBy = new HashMap<>();
    private final List<RecipeHolder<CraftingRecipe>> crafting = new ArrayList<>();
    private final Set<Pair> realCuts = new HashSet<>();
    private final Map<Item, Ingredient> smithingAdds = new HashMap<>();
    private final Set<Pair> smithingMakes = new HashSet<>();

    @SuppressWarnings("unchecked")
    private RecipeIndex(RecipeManager manager, RegistryAccess ra) {
        for (RecipeHolder<?> h : manager.getRecipes()) {
            try {
                if (h.value() instanceof SingleItemRecipe cut && cut.getType() == net.minecraft.world.item.crafting.RecipeType.STONECUTTING) {
                    ItemStack result = cut.getResultItem(ra);
                    Ingredient in = cut.getIngredients().get(0);
                    if (!(cut instanceof FamilyCutRecipe)) {
                        for (ItemStack s : in.getItems()) {
                            realCuts.add(new Pair(s.getItem(), result.getItem()));
                        }
                    }
                    Item single = cut instanceof FamilyCutRecipe ? null : single(in); // those follow names, not recipes
                    if (single != null && !result.isEmpty() && single != result.getItem()) {
                        add(result.getItem(), new Source(single, 1, result.getCount(), true));
                    }
                } else if (h.value() instanceof CraftingRecipe c && !(c instanceof BackToBlockRecipe)) {
                    crafting.add((RecipeHolder<CraftingRecipe>) h);
                    ItemStack result = c.isSpecial() ? ItemStack.EMPTY : c.getResultItem(ra);
                    if (result.isEmpty()) {
                        continue;
                    }
                    craftedBy.computeIfAbsent(result.getItem(), k -> new ArrayList<>()).add(c);
                    Item same = null;
                    int count = 0;
                    for (Ingredient i : c.getIngredients()) {
                        if (i.isEmpty()) {
                            continue;
                        }
                        Item one = single(i);
                        if (one == null || (same != null && one != same)) {
                            same = null;
                            count = 0;
                            break;
                        }
                        same = one;
                        count++;
                    }
                    if (same != null && same != result.getItem()) {
                        add(result.getItem(), new Source(same, count, result.getCount(), false));
                    }
                } else if (h.value() instanceof SmithingTransformRecipe s) {
                    Item result = s.getResultItem(ra).getItem();
                    smithingAdds.putIfAbsent(result, s.addition);
                    for (ItemStack base : s.base.getItems()) {
                        smithingMakes.add(new Pair(base.getItem(), result));
                    }
                }
            } catch (RuntimeException e) {
                // a modded recipe that can't answer these questions: leave it out
            }
        }
    }

    private void add(Item result, Source s) {
        sources.computeIfAbsent(result, k -> new ArrayList<>()).add(s);
    }

    /** The one item an ingredient stands for, or null if it's several (a tag of planks) or none. */
    private static Item single(Ingredient i) {
        ItemStack[] items = i.getItems();
        if (items.length == 0) {
            return null;
        }
        Item first = items[0].getItem();
        for (ItemStack s : items) {
            if (s.getItem() != first) {
                return null;
            }
        }
        return first;
    }

    public List<Source> sources(Item item) {
        return sources.getOrDefault(item, List.of());
    }

    public Set<Item> madeItems() {
        return sources.keySet();
    }

    /** Whether some recipe makes {@code piece} out of {@code block} alone. */
    public boolean isMadeFrom(Item piece, Item block) {
        for (Source s : sources(piece)) {
            if (s.input() == block) {
                return true;
            }
        }
        return false;
    }

    /**
     * How many of {@code material} the cheapest crafting recipe for {@code result} takes: empty if nothing crafts it,
     * 0 if it's crafted without that material.
     */
    public OptionalInt madeFrom(Item result, Ingredient material) {
        List<CraftingRecipe> recipes = craftedBy.get(result);
        if (recipes == null) {
            return OptionalInt.empty();
        }
        ItemStack[] mats = material.getItems();
        int best = Integer.MAX_VALUE;
        for (CraftingRecipe r : recipes) {
            int n = 0;
            for (Ingredient i : r.getIngredients()) {
                if (!i.isEmpty() && overlaps(i, mats)) {
                    n++;
                }
            }
            best = Math.min(best, n);
        }
        return OptionalInt.of(best);
    }

    private static boolean overlaps(Ingredient i, ItemStack[] mats) {
        for (ItemStack m : mats) {
            if (i.test(m)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the stonecutter cuts {@code from} into {@code to} without Workbench Wisdom's help. */
    public boolean isRealCut(Item from, Item to) {
        return realCuts.contains(new Pair(from, to));
    }

    /** What the smithing table adds to make {@code result} (a netherite ingot), or null if it doesn't make it. */
    public Ingredient smithingAddition(Item result) {
        return smithingAdds.get(result);
    }

    public boolean isSmithed(Item item) {
        return smithingAdds.containsKey(item);
    }

    /** Whether the smithing table already turns {@code from} into {@code to}. */
    public boolean smithingMakes(Item from, Item to) {
        return smithingMakes.contains(new Pair(from, to));
    }

    /** Whether a crafting recipe other than Workbench Wisdom's slab-and-stair ones takes this grid. */
    public boolean otherCraftMatches(CraftingInput input, Level level) {
        for (RecipeHolder<CraftingRecipe> h : crafting) {
            try {
                if (h.value().matches(input, level)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // a recipe that can't be asked out of its own menu
            }
        }
        return false;
    }
}
