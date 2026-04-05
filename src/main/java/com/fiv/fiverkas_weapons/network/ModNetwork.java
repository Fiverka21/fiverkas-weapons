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
        registrar.playToServer(
                ClientAttackFlagPayload.TYPE,
                ClientAttackFlagPayload.STREAM_CODEC,
                ModNetwork::handleClientAttackFlag
        );
        registrar.playToServer(
                BayonetComboAttackPayload.TYPE,
                BayonetComboAttackPayload.STREAM_CODEC,
                ModNetwork::handleBayonetComboAttack
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
            ModCombatEvents.recordClientAttackFlag(player, ModCombatEvents.ClientAttackFlag.BAYONET_GUNSHOT);
            ModCombatEvents.spawnBayonetGunshotMuzzleParticles(player);
        });
    }

    private static void handleClientAttackFlag(ClientAttackFlagPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ModCombatEvents.recordClientAttackFlag(player, payload.flag());
        });
    }

    private static void handleBayonetComboAttack(BayonetComboAttackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ModCombatEvents.onBayonetComboAttack(player);
        });
    }
}
