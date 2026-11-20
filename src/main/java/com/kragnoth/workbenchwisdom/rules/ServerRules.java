package com.kragnoth.workbenchwisdom.rules;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;

/** The server's rules, worked out the first time they're needed after each load of recipes and tags. */
public final class ServerRules {
    private ServerRules() {}

    private static RecipeManager builtFor;
    private static BenchRules rules = BenchRules.EMPTY;

    public static synchronized BenchRules get(MinecraftServer server) {
        if (server == null) {
            return BenchRules.EMPTY;
        }
        if (builtFor != server.getRecipeManager()) {
            builtFor = server.getRecipeManager();
            rules = RulesBuilder.build(server.overworld());
        }
        return rules;
    }
}
