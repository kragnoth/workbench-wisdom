package com.kragnoth.workbenchwisdom.recipe;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.bench.BenchInput;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A datapack salvage for the Reforging Bench ({@code "type": "workbenchwisdom:salvage"}): each {@code input} breaks
 * down into {@code result} (worn gear gives less in proportion). Takes priority over what the bench works out by
 * itself; {@code salvage.returns} doesn't apply, the result is exactly what the pack says.
 */
public record SalvageRecipe(Ingredient input, ItemStack result) implements Recipe<BenchInput> {
    @Override
    public boolean matches(BenchInput in, Level level) {
        return in.addition().isEmpty() && input.test(in.base());
    }

    @Override
    public ItemStack assemble(BenchInput in, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, input);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WorkbenchWisdom.SALVAGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return WorkbenchWisdom.SALVAGE.get();
    }

    private static final MapCodec<SalvageRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(SalvageRecipe::input),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SalvageRecipe::result)
    ).apply(i, SalvageRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, SalvageRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SalvageRecipe::input,
            ItemStack.STREAM_CODEC, SalvageRecipe::result,
            SalvageRecipe::new);

    public static final RecipeSerializer<SalvageRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<SalvageRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SalvageRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };
}
