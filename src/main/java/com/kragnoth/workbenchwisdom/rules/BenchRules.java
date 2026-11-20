package com.kragnoth.workbenchwisdom.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/**
 * Everything the Reforging Bench can do, item by item: which upgrades each piece of gear has, and what each item
 * salvages into. Worked out on the server ({@link RulesBuilder}) whenever recipes or tags load, and sent to every
 * client so the bench screen and JEI show exactly what the server will do.
 */
public final class BenchRules {
    /** {@code from} plus {@code count} of {@code material} becomes {@code to}, keeping its enchantments. */
    public record Upgrade(Item from, Item to, Ingredient material, int count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Upgrade> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), Upgrade::from,
                ByteBufCodecs.registry(Registries.ITEM), Upgrade::to,
                Ingredient.CONTENTS_STREAM_CODEC, Upgrade::material,
                ByteBufCodecs.VAR_INT, Upgrade::count,
                Upgrade::new);
    }

    /**
     * {@code item} breaks down into {@code perItem} of {@code material} each (a fraction for blocks: 0.335 stone per
     * slab). Gear ({@code gear}) salvages one at a time, scaled by how worn it is and rounded, and gives experience
     * back for its enchantments; everything else salvages by the stack, rounded down.
     */
    public record Salvage(Item item, ItemStack material, double perItem, boolean gear) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Salvage> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), Salvage::item,
                ItemStack.STREAM_CODEC, Salvage::material,
                ByteBufCodecs.DOUBLE, Salvage::perItem,
                ByteBufCodecs.BOOL, Salvage::gear,
                Salvage::new);

        /** The fewest items that give back at least one material (for showing in JEI). */
        public int smallestBatch() {
            return gear ? 1 : Math.max(1, (int) Math.ceil(1.0 / perItem - 1e-9));
        }
    }

    public static final BenchRules EMPTY = new BenchRules(List.of(), List.of());

    public static final StreamCodec<RegistryFriendlyByteBuf, BenchRules> STREAM_CODEC = StreamCodec.composite(
            Upgrade.STREAM_CODEC.apply(ByteBufCodecs.list()), BenchRules::upgradeList,
            Salvage.STREAM_CODEC.apply(ByteBufCodecs.list()), BenchRules::salvageList,
            BenchRules::new);

    private final List<Upgrade> upgradeList;
    private final List<Salvage> salvageList;
    private final Map<Item, List<Upgrade>> upgrades = new HashMap<>();
    private final Map<Item, Salvage> salvage = new HashMap<>();

    public BenchRules(List<Upgrade> upgrades, List<Salvage> salvage) {
        this.upgradeList = List.copyOf(upgrades);
        this.salvageList = List.copyOf(salvage);
        for (Upgrade u : upgradeList) {
            this.upgrades.computeIfAbsent(u.from(), k -> new ArrayList<>()).add(u);
        }
        for (Salvage s : salvageList) {
            this.salvage.putIfAbsent(s.item(), s);
        }
    }

    /** The rules for this side: the server's own, or the copy it sent this client. */
    public static BenchRules of(Level level) {
        return level.isClientSide ? ClientRules.get() : ServerRules.get(level.getServer());
    }

    public List<Upgrade> upgrades(Item from) {
        return upgrades.getOrDefault(from, List.of());
    }

    public Salvage salvage(Item item) {
        return salvage.get(item);
    }

    public boolean hasRules(Item item) {
        return upgrades.containsKey(item) || salvage.containsKey(item);
    }

    public List<Upgrade> upgradeList() {
        return upgradeList;
    }

    public List<Salvage> salvageList() {
        return salvageList;
    }

    public Collection<Item> upgradeable() {
        return upgrades.keySet();
    }
}
