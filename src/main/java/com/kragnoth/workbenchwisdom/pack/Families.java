package com.kragnoth.workbenchwisdom.pack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * Block families by name, across every mod: a block {@code X} with {@code X_stairs}, {@code X_slab} or {@code X_wall}
 * (also when {@code X} ends in "s" or "_block": stone_bricks / stone_brick_slab, quartz_block / quartz_slab), and
 * planks with their wood set ({@code <wood>_planks} with {@code <wood>_stairs}, {@code _fence}, {@code _door}...).
 */
public final class Families {
    private Families() {}

    /** The stonecutter can cut {@code count} of {@code result} from one {@code input}. */
    public record Cut(Item input, Item result, int count) {}

    /** {@code piece} (a slab or stairs) goes back into {@code block}. */
    public record Piece(Item piece, Item block) {}

    private static final String[] BLOCK_PIECES = {"stairs", "slab", "wall"};
    private static final String[] WOOD_PIECES = {"stairs", "slab", "fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate", "sign"};

    public static List<Cut> cuts() {
        List<Cut> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem)) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String path = id.getPath();
            if (path.endsWith("_planks")) {
                String wood = path.substring(0, path.length() - "_planks".length());
                for (String piece : WOOD_PIECES) {
                    block(id.getNamespace(), wood + "_" + piece).ifPresent(r -> out.add(new Cut(item, r, piece.equals("slab") ? 2 : 1)));
                }
                continue;
            }
            for (String stem : stems(path)) {
                boolean any = false;
                for (String piece : BLOCK_PIECES) {
                    var found = block(id.getNamespace(), stem + "_" + piece);
                    if (found.isPresent() && found.get() != item) {
                        out.add(new Cut(item, found.get(), piece.equals("slab") ? 2 : 1));
                        any = true;
                    }
                }
                if (any) {
                    break; // the closest stem with pieces is this block's family
                }
            }
        }
        return out;
    }

    public static List<Piece> pieces() {
        List<Piece> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem)) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String path = id.getPath();
            String stem = path.endsWith("_slab") ? path.substring(0, path.length() - 5)
                    : path.endsWith("_stairs") ? path.substring(0, path.length() - 7) : null;
            if (stem == null) {
                continue;
            }
            for (String base : new String[] {stem, stem + "s", stem + "_block", stem + "_planks"}) {
                var found = block(id.getNamespace(), base);
                if (found.isPresent()) {
                    out.add(new Piece(item, found.get()));
                    break;
                }
            }
        }
        return out;
    }

    /** The names a block's pieces might be named after: itself, without a plural "s", without "_block". */
    private static List<String> stems(String path) {
        List<String> out = new ArrayList<>();
        out.add(path);
        if (path.endsWith("s")) {
            out.add(path.substring(0, path.length() - 1));
        }
        if (path.endsWith("_block")) {
            out.add(path.substring(0, path.length() - "_block".length()));
        }
        return out;
    }

    private static Optional<Item> block(String namespace, String path) {
        ResourceLocation rl = ResourceLocation.tryBuild(namespace, path);
        if (rl == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(rl).filter(i -> i instanceof BlockItem);
    }
}
