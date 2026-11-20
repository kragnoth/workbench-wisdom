package com.kragnoth.workbenchwisdom;

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
 * A tier upgrade at the Reforging Bench: {@code base} plus {@code count} of {@code addition} becomes {@code result},
 * keeping everything on the base (enchantments, name, wear), as a smithing upgrade does. The generated ones cost what
 * crafting the new item would (three diamonds for a pickaxe); datapacks can add their own.
 */
public record ReforgingRecipe(Ingredient base, Ingredient addition, int count, ItemStack result) implements Recipe<ReforgingInput> {
    @Override
    public boolean matches(ReforgingInput input, Level level) {
        return base.test(input.base()) && addition.test(input.addition()) && input.addition().getCount() >= count;
    }

    /** Whether it's the right pair, enough of the addition or not (to say how many more it needs). */
    public boolean matchesItems(ReforgingInput input) {
        return base.test(input.base()) && addition.test(input.addition());
    }

    @Override
    public ItemStack assemble(ReforgingInput input, HolderLookup.Provider registries) {
        ItemStack out = input.base().transmuteCopy(result.getItem(), 1);
        out.applyComponents(result.getComponentsPatch());
        return out;
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
        return true; // not in the recipe book
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WorkbenchWisdom.REFORGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return WorkbenchWisdom.REFORGING.get();
    }

    private static final MapCodec<ReforgingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(ReforgingRecipe::base),
            Ingredient.CODEC_NONEMPTY.fieldOf("addition").forGetter(ReforgingRecipe::addition),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ReforgingRecipe::count),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ReforgingRecipe::result)
    ).apply(i, ReforgingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, ReforgingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, ReforgingRecipe::base,
            Ingredient.CONTENTS_STREAM_CODEC, ReforgingRecipe::addition,
            ByteBufCodecs.VAR_INT, ReforgingRecipe::count,
            ItemStack.STREAM_CODEC, ReforgingRecipe::result,
            ReforgingRecipe::new);

    static final RecipeSerializer<ReforgingRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<ReforgingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ReforgingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };
}
