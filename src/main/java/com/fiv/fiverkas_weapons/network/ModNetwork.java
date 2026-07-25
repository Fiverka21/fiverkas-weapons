package com.fiv.fiverkas_weapons.network;

import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.event.client.ModCombatClientEvents;
import com.fiv.fiverkas_weapons.item.DShieldItem;
import com.fiv.fiverkas_weapons.registry.ModItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(
                BayonetMuzzleFlashPayload.TYPE,
                BayonetMuzzleFlashPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                ClientAttackFlagPayload.TYPE,
                ClientAttackFlagPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                BayonetComboAttackPayload.TYPE,
                BayonetComboAttackPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                SacrilegiousSlamRequestPayload.TYPE,
                SacrilegiousSlamRequestPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                DShieldResiliencePayload.TYPE,
                DShieldResiliencePayload.STREAM_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                BayonetImpactFramePayload.TYPE,
                BayonetImpactFramePayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                SacrilegiousSlamPayload.TYPE,
                SacrilegiousSlamPayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                BayonetMuzzleFlashPayload.TYPE,
                (payload, context) -> handleBayonetMuzzleFlash(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                ClientAttackFlagPayload.TYPE,
                (payload, context) -> ModCombatEvents.recordClientAttackFlag(context.player(), payload.flag())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                BayonetComboAttackPayload.TYPE,
                (payload, context) -> ModCombatEvents.onBayonetComboAttack(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                SacrilegiousSlamRequestPayload.TYPE,
                (payload, context) -> handleSacrilegiousSlamRequest(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                DShieldResiliencePayload.TYPE,
                (payload, context) -> DShieldItem.activateResilience(context.player())
        );
    }

    public static void registerClientPayloads() {
        ClientPlayNetworking.registerGlobalReceiver(
                BayonetImpactFramePayload.TYPE,
                (payload, context) -> ModCombatClientEvents.triggerBayonetImpactFrame()
        );
        ClientPlayNetworking.registerGlobalReceiver(
                SacrilegiousSlamPayload.TYPE,
                (payload, context) -> ModCombatClientEvents.handleSacrilegiousSlamClient(
                        payload.playerId(),
                        payload.animationName()
                )
        );
    }

    public static void sendSacrilegiousSlamToClient(ServerPlayer player, int playerId, String animationName) {
        if (ServerPlayNetworking.canSend(player, SacrilegiousSlamPayload.TYPE)) {
            ServerPlayNetworking.send(player, new SacrilegiousSlamPayload(playerId, animationName));
        }
    }

    private static void handleBayonetMuzzleFlash(ServerPlayer player) {
        if (!player.getMainHandItem().is(ModItems.BAYONET.get())
                && !player.getOffhandItem().is(ModItems.BAYONET.get())) {
            return;
        }

        ModCombatEvents.recordClientAttackFlag(player, ModCombatEvents.ClientAttackFlag.BAYONET_GUNSHOT);
        ModCombatEvents.spawnBayonetGunshotMuzzleParticles(player);
    }

    private static void handleSacrilegiousSlamRequest(ServerPlayer player) {
        if (!player.getMainHandItem().is(ModItems.SACRILEGIOUS.get())
                && !player.getOffhandItem().is(ModItems.SACRILEGIOUS.get())) {
            return;
        }

        ItemStack stack = player.getMainHandItem().is(ModItems.SACRILEGIOUS.get())
                ? player.getMainHandItem()
                : player.getOffhandItem();
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }

        player.getCooldowns().addCooldown(
                stack,
                com.fiv.fiverkas_weapons.item.Sacrilegious.SLAM_COOLDOWN_TICKS
        );
        com.fiv.fiverkas_weapons.item.Sacrilegious.launchPlayer(player);

        tryForwardSacrilegiousToBetterCombat(player);
        sendSacrilegiousSlamToClient(player, player.getId(), "bettercombat:two_handed_slam");
    }

    private static void tryForwardSacrilegiousToBetterCombat(ServerPlayer player) {
        try {
            Class<?> attackReqClass = Class.forName("net.bettercombat.network.Packets$C2S_AttackRequest");
            Class<?> serverNetworkClass = Class.forName("net.bettercombat.network.ServerNetwork");
            java.lang.reflect.Constructor<?> ctor = attackReqClass.getConstructor(
                    int.class,
                    boolean.class,
                    int.class,
                    int.class,
                    int[].class
            );

            int lastIndex = 5;
            int selectedSlot = player.getInventory().getSelectedSlot();
            Object attackReq = ctor.newInstance(lastIndex, false, selectedSlot, 0, new int[0]);

            try {
                serverNetworkClass
                        .getMethod(
                                "handleAttackRequest",
                                attackReqClass,
                                net.minecraft.server.MinecraftServer.class,
                                net.minecraft.server.level.ServerPlayer.class,
                                Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl")
                        )
                        .invoke(null, attackReq, player.level().getServer(), player, player.connection);
            } catch (NoSuchMethodException ignored) {
                serverNetworkClass
                        .getMethod(
                                "handleAttackRequest",
                                attackReqClass,
                                net.minecraft.server.MinecraftServer.class,
                                net.minecraft.server.level.ServerPlayer.class
                        )
                        .invoke(null, attackReq, player.level().getServer(), player);
            }
        } catch (ReflectiveOperationException ignored) {
            // Better Combat integration is optional at compile time; gameplay fallback still runs.
        }
    }
}
