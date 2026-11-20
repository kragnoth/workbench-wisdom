package com.kragnoth.workbenchwisdom.rules;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Sends the bench rules to clients. The datapack sync event comes just before the recipes are sent, both on joining
 * and on {@code /reload}, so the rules are in place before JEI (which waits for the recipes) builds its lists.
 */
public final class RulesSync {
    private RulesSync() {}

    public record Payload(BenchRules rules) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(WorkbenchWisdom.id("rules"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC =
                BenchRules.STREAM_CODEC.map(Payload::new, Payload::rules);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").optional().playToClient(Payload.TYPE, Payload.STREAM_CODEC, (p, ctx) -> ClientRules.set(p.rules()));
    }

    public static void onSync(OnDatapackSyncEvent e) {
        BenchRules rules = ServerRules.get(e.getPlayerList().getServer());
        Payload p = new Payload(rules);
        e.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, p));
    }
}
