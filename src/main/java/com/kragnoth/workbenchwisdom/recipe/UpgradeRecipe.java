package com.kragnoth.workbenchwisdom.recipe;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.bench.BenchInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A datapack upgrade for the Reforging Bench ({@code "type": "workbenchwisdom:upgrade"}): {@code base} plus
 * {@code count} of {@code addition} becomes {@code result}, keeping the base's enchantments, name and wear. Takes
 * priority over the upgrades the bench works out by itself for the same base item.
 */
public record UpgradeRecipe(Ingredient base, Ingredient addition, int count, ItemStack result) implements Recipe<BenchInput> {
    @Override
    public boolean matches(BenchInput input, Level level) {
        return base.test(input.base()) && addition.test(input.addition()) && input.addition().getCount() >= count;
    }

    @Override
    public ItemStack assemble(BenchInput input, HolderLookup.Provider registries) {
        return input.base().transmuteCopy(result.getItem(), 1);
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
        return NonNullList.of(Ingredient.EMPTY, base, addition);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WorkbenchWisdom.UPGRADE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return WorkbenchWisdom.UPGRADE.get();
    }

    private static final MapCodec<UpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(UpgradeRecipe::base),
            Ingredient.CODEC_NONEMPTY.fieldOf("addition").forGetter(UpgradeRecipe::addition),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(UpgradeRecipe::count),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(UpgradeRecipe::result)
    ).apply(i, UpgradeRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, UpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, UpgradeRecipe::base,
            Ingredient.CONTENTS_STREAM_CODEC, UpgradeRecipe::addition,
            ByteBufCodecs.VAR_INT, UpgradeRecipe::count,
            ItemStack.STREAM_CODEC, UpgradeRecipe::result,
            UpgradeRecipe::new);

    public static final RecipeSerializer<UpgradeRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<UpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };
}
