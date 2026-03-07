package com.fiv.fiverkas_weapons.network;

import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                BayonetMuzzleFlashPayload.TYPE,
                BayonetMuzzleFlashPayload.STREAM_CODEC,
                ModNetwork::handleBayonetMuzzleFlash
        );
    }

    private static void handleBayonetMuzzleFlash(BayonetMuzzleFlashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.getMainHandItem().is(ModItems.BAYONET.get())
                    && !player.getOffhandItem().is(ModItems.BAYONET.get())) {
                return;
            }
            ModCombatEvents.spawnBayonetGunshotMuzzleParticles(player);
        });
    }
}
