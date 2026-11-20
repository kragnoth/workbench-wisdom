package com.kragnoth.workbenchwisdom;

import com.kragnoth.workbenchwisdom.bench.ReforgingBenchBlock;
import com.kragnoth.workbenchwisdom.bench.ReforgingMenu;
import com.kragnoth.workbenchwisdom.pack.GeneratedPack;
import com.kragnoth.workbenchwisdom.recipe.BackToBlockRecipe;
import com.kragnoth.workbenchwisdom.recipe.FamilyCutRecipe;
import com.kragnoth.workbenchwisdom.recipe.SalvageRecipe;
import com.kragnoth.workbenchwisdom.recipe.UpgradeRecipe;
import com.kragnoth.workbenchwisdom.rules.RulesSync;
import com.kragnoth.workbenchwisdom.rules.UpgradePaths;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Workbench Wisdom: the Reforging Bench (upgrades that keep enchantments, enchantment transfers, salvage) and
 * block-family conveniences for the stonecutter and crafting table. Everything works across every mod's items: the
 * bench works out its rules from the recipes and tags the server has loaded ({@link com.kragnoth.workbenchwisdom.rules}),
 * and the block-family recipes come from a built-in datapack generated from the item registry
 * ({@link GeneratedPack}).
 */
@Mod(WorkbenchWisdom.MODID)
public class WorkbenchWisdom {
    public static final String MODID = "workbenchwisdom";
    public static final Logger LOG = LogUtils.getLogger();

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    /** Items salvage never breaks down (on top of what it leaves alone by itself). */
    public static final TagKey<Item> NO_SALVAGE = TagKey.create(Registries.ITEM, id("no_salvage"));

    public static final DeferredHolder<RecipeType<?>, RecipeType<UpgradeRecipe>> UPGRADE = RECIPE_TYPES.register("upgrade",
            () -> RecipeType.simple(id("upgrade")));
    public static final DeferredHolder<RecipeType<?>, RecipeType<SalvageRecipe>> SALVAGE = RECIPE_TYPES.register("salvage",
            () -> RecipeType.simple(id("salvage")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UpgradeRecipe>> UPGRADE_SERIALIZER =
            SERIALIZERS.register("upgrade", () -> UpgradeRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SalvageRecipe>> SALVAGE_SERIALIZER =
            SERIALIZERS.register("salvage", () -> SalvageRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FamilyCutRecipe>> FAMILY_CUTTING_SERIALIZER =
            SERIALIZERS.register("family_cutting", () -> FamilyCutRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BackToBlockRecipe>> BACK_TO_BLOCK_SERIALIZER =
            SERIALIZERS.register("back_to_block", () -> BackToBlockRecipe.SERIALIZER);

    public static final DeferredHolder<MenuType<?>, MenuType<ReforgingMenu>> REFORGING_MENU =
            MENUS.register("reforging_bench", () -> new MenuType<>(ReforgingMenu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredBlock<ReforgingBenchBlock> REFORGING_BENCH = BLOCKS.registerBlock("reforging_bench", ReforgingBenchBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> REFORGING_BENCH_ITEM = ITEMS.registerSimpleBlockItem(REFORGING_BENCH);

    public WorkbenchWisdom(IEventBus modBus, ModContainer container) {
        SERIALIZERS.register(modBus);
        RECIPE_TYPES.register(modBus);
        MENUS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
                e.accept(REFORGING_BENCH_ITEM.get());
            }
        });
        modBus.addListener((AddPackFindersEvent e) -> GeneratedPack.register(e));
        modBus.addListener((RegisterPayloadHandlersEvent e) -> RulesSync.register(e));

        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new UpgradePaths()));
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> RulesSync.onSync(e));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
