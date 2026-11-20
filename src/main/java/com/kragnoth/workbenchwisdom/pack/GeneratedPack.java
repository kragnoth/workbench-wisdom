package com.kragnoth.workbenchwisdom.pack;

import com.google.gson.JsonObject;
import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * A built-in datapack, "Workbench Wisdom: block families", written from the item registry when datapacks load, so it
 * covers every mod installed. It holds the family stonecutter cuts ({@code recipe/cutting/...}) and the slabs and
 * stairs back to blocks ({@code recipe/back_to_block/...}). It's on by default in every world, sits below other
 * datapacks (so a datapack can replace any of its recipes by id), and can be turned off like any datapack.
 */
public final class GeneratedPack extends AbstractPackResources {
    public static final String ID = WorkbenchWisdom.MODID + "_block_families";

    private Map<ResourceLocation, byte[]> files;

    private GeneratedPack(PackLocationInfo info) {
        super(info);
    }

    public static void register(AddPackFindersEvent e) {
        if (e.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        PackLocationInfo info = new PackLocationInfo(ID, Component.translatable("pack.workbenchwisdom.block_families"),
                PackSource.BUILT_IN, Optional.empty());
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo location) {
                return new GeneratedPack(location);
            }

            @Override
            public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                return new GeneratedPack(location);
            }
        };
        Pack pack = Pack.readMetaAndCreate(info, supplier, PackType.SERVER_DATA, new PackSelectionConfig(false, Pack.Position.BOTTOM, false));
        if (pack != null) {
            e.addRepositorySource(add -> add.accept(pack));
        }
    }

    private synchronized Map<ResourceLocation, byte[]> files() {
        if (files == null) {
            files = new TreeMap<>();
            int cuts = 0;
            for (Families.Cut c : Families.cuts()) {
                JsonObject o = new JsonObject();
                o.addProperty("type", WorkbenchWisdom.MODID + ":family_cutting");
                o.addProperty("input", key(c.input()).toString());
                JsonObject result = new JsonObject();
                result.addProperty("id", key(c.result()).toString());
                result.addProperty("count", c.count());
                o.add("result", result);
                ResourceLocation r = key(c.result());
                put("recipe/cutting/" + r.getNamespace() + "/" + r.getPath() + "_from_" + key(c.input()).getPath() + ".json", o);
                cuts++;
            }
            int pieces = 0;
            for (Families.Piece p : Families.pieces()) {
                JsonObject o = new JsonObject();
                o.addProperty("type", WorkbenchWisdom.MODID + ":back_to_block");
                o.addProperty("piece", key(p.piece()).toString());
                o.addProperty("block", key(p.block()).toString());
                ResourceLocation r = key(p.piece());
                put("recipe/back_to_block/" + r.getNamespace() + "/" + r.getPath() + ".json", o);
                pieces++;
            }
            WorkbenchWisdom.LOG.info("Block families: {} stonecutter cuts and {} slabs and stairs back to blocks written (the ones the game already has are skipped in play)",
                    cuts, pieces);
        }
        return files;
    }

    private void put(String path, JsonObject json) {
        files.put(WorkbenchWisdom.id(path), json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static ResourceLocation key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length == 1 && path[0].equals(PACK_META)) {
            JsonObject pack = new JsonObject();
            pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
            pack.addProperty("description", "Workbench Wisdom: block families");
            JsonObject root = new JsonObject();
            root.add("pack", pack);
            byte[] bytes = root.toString().getBytes(StandardCharsets.UTF_8);
            return () -> new ByteArrayInputStream(bytes);
        }
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.SERVER_DATA) {
            return null;
        }
        byte[] bytes = files().get(location);
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.SERVER_DATA || !namespace.equals(WorkbenchWisdom.MODID)) {
            return;
        }
        String prefix = path.endsWith("/") ? path : path + "/";
        for (Map.Entry<ResourceLocation, byte[]> f : files().entrySet()) {
            if (f.getKey().getPath().startsWith(prefix)) {
                byte[] bytes = f.getValue();
                output.accept(f.getKey(), () -> new ByteArrayInputStream(bytes));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? Set.of(WorkbenchWisdom.MODID) : Set.of();
    }

    @Override
    public void close() {}
}
