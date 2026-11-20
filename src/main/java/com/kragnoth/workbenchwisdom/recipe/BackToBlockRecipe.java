package com.kragnoth.workbenchwisdom.recipe;

import com.kragnoth.workbenchwisdom.Config;
import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.rules.RecipeIndex;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * Slabs and stairs back into their block at the crafting table: two slabs side by side make one block, four stairs in
 * a square make three (what six blocks made four of). Written by {@link com.kragnoth.workbenchwisdom.pack.GeneratedPack}
 * for every slab and stair named after a block. It only works where the piece really is made from that block, and
 * stands aside wherever another recipe takes the same grid (two stone brick slabs stacked still make chiseled stone
 * bricks).
 */
public class BackToBlockRecipe extends ShapedRecipe {
    private final Item piece;
    private final Item block;

    public BackToBlockRecipe(Item piece, Item block) {
        super("", CraftingBookCategory.BUILDING, pattern(piece), new ItemStack(block, isStairs(piece) ? 3 : 1));
        this.piece = piece;
        this.block = block;
    }

    private static boolean isStairs(Item piece) {
        return BuiltInRegistries.ITEM.getKey(piece).getPath().endsWith("_stairs");
    }

    private static ShapedRecipePattern pattern(Item piece) {
        Map<Character, Ingredient> key = Map.of('#', Ingredient.of(piece));
        return isStairs(piece) ? ShapedRecipePattern.of(key, "##", "##") : ShapedRecipePattern.of(key, "##");
    }

    public Item piece() {
        return piece;
    }

    public Item block() {
        return block;
    }

    /** Whether the piece really is made from the block (by some recipe other than this one). */
    public boolean isValid(Level level) {
        return Config.BACK_TO_BLOCKS.get() && RecipeIndex.of(level).isMadeFrom(piece, block);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && isValid(level) && !RecipeIndex.of(level).otherCraftMatches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WorkbenchWisdom.BACK_TO_BLOCK_SERIALIZER.get();
    }

    private static final MapCodec<BackToBlockRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("piece").forGetter(BackToBlockRecipe::piece),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("block").forGetter(BackToBlockRecipe::block)
    ).apply(i, BackToBlockRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, BackToBlockRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), BackToBlockRecipe::piece,
            ByteBufCodecs.registry(Registries.ITEM), BackToBlockRecipe::block,
            BackToBlockRecipe::new);

    public static final RecipeSerializer<BackToBlockRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<BackToBlockRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BackToBlockRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };
}
