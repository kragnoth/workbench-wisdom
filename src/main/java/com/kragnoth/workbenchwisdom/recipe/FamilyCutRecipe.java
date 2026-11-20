package com.kragnoth.workbenchwisdom.recipe;

import com.kragnoth.workbenchwisdom.Config;
import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.rules.RecipeIndex;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

/**
 * A stonecutter cut from a block into a piece of its family (stone into stone stairs, oak planks into an oak door),
 * written by {@link com.kragnoth.workbenchwisdom.pack.GeneratedPack} for every family in the game. It stands aside
 * when the stonecutter already has the same cut from some other recipe, so nothing is listed twice.
 */
public class FamilyCutRecipe extends StonecutterRecipe {
    private final Item input;

    public FamilyCutRecipe(Item input, ItemStack result) {
        super("", Ingredient.of(input), result);
        this.input = input;
    }

    public Item input() {
        return input;
    }

    public ItemStack result() {
        return result;
    }

    /** Whether this cut adds something the stonecutter doesn't already do. */
    public boolean isNeeded(Level level) {
        return Config.FAMILY_CUTTING.get() && !RecipeIndex.of(level).isRealCut(input, result.getItem());
    }

    @Override
    public boolean matches(SingleRecipeInput in, Level level) {
        return super.matches(in, level) && isNeeded(level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WorkbenchWisdom.FAMILY_CUTTING_SERIALIZER.get();
    }

    private static final MapCodec<FamilyCutRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("input").forGetter(FamilyCutRecipe::input),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(FamilyCutRecipe::result)
    ).apply(i, FamilyCutRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, FamilyCutRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), FamilyCutRecipe::input,
            ItemStack.STREAM_CODEC, FamilyCutRecipe::result,
            FamilyCutRecipe::new);

    public static final RecipeSerializer<FamilyCutRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<FamilyCutRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FamilyCutRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };
}
