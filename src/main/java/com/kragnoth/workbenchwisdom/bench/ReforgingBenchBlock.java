package com.kragnoth.workbenchwisdom.bench;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The Reforging Bench. Keeps nothing, like a smithing table. */
public class ReforgingBenchBlock extends CraftingTableBlock {
    public static final MapCodec<ReforgingBenchBlock> CODEC = simpleCodec(ReforgingBenchBlock::new);
    private static final Component TITLE = Component.translatable("container.workbenchwisdom.reforging_bench");

    public ReforgingBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CraftingTableBlock> codec() {
        return CODEC;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, player) -> new ReforgingMenu(id, inv, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
