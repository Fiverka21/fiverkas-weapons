package com.fiv.fiverkas_weapons.network;
import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.event.client.ModCombatClientEvents;
import com.fiv.fiverkas_weapons.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
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
        registrar.playToServer(
                SacrilegiousSlamRequestPayload.TYPE,
                SacrilegiousSlamRequestPayload.STREAM_CODEC,
                ModNetwork::handleSacrilegiousSlamRequest
        );
        registrar.playToClient(
                BayonetImpactFramePayload.TYPE,
                BayonetImpactFramePayload.STREAM_CODEC,
                ModNetwork::handleBayonetImpactFrame
        );
        registrar.playToClient(
                SacrilegiousSlamPayload.TYPE,
                SacrilegiousSlamPayload.STREAM_CODEC,
                ModNetwork::handleSacrilegiousSlam
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
    private static void handleBayonetImpactFrame(BayonetImpactFramePayload payload, IPayloadContext context) {
        context.enqueueWork(ModCombatClientEvents::triggerBayonetImpactFrame);
    }
    private static void handleSacrilegiousSlam(SacrilegiousSlamPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ModCombatClientEvents.handleSacrilegiousSlamClient(payload.playerId(), payload.animationName()));
    }
    private static void handleSacrilegiousSlamRequest(SacrilegiousSlamRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            // Ensure player is actually holding Sacrilegious
            if (!player.getMainHandItem().is(ModItems.SACRILEGIOUS.get()) && !player.getOffhandItem().is(ModItems.SACRILEGIOUS.get())) {
                return;
            }
            ItemStack stack = player.getMainHandItem().is(ModItems.SACRILEGIOUS.get()) ? player.getMainHandItem() : player.getOffhandItem();
            Item item = stack.getItem();
            if (player.getCooldowns().isOnCooldown(item)) {
                return;
            }
            player.getCooldowns().addCooldown(item, com.fiv.fiverkas_weapons.item.Sacrilegious.SLAM_COOLDOWN_TICKS);
            // Launch player first (adds velocity, particles, sound)
            com.fiv.fiverkas_weapons.item.Sacrilegious.launchPlayer(player);
            // Attempt to register this action as a BetterCombat attack on the server so it counts as the
            // weapon's last attack in the pattern. Construct a C2S_AttackRequest and invoke ServerNetwork.handleAttackRequest.
            try {
                Class<?> attackReqClass = Class.forName("net.bettercombat.network.Packets$C2S_AttackRequest");
                Class<?> serverNetworkClass = Class.forName("net.bettercombat.network.ServerNetwork");
                java.lang.reflect.Constructor<?> ctor = attackReqClass.getConstructor(int.class, boolean.class, int.class, int.class, int[].class);
                // sacrilegious last attack index (zero-based)
                int lastIndex = 5;
                int selectedSlot = 0;
                try {
                    selectedSlot = player.getInventory().selected;
                } catch (Throwable ignored) {
                    try {
                        Object sel = player.getClass().getMethod("getSelectedSlot").invoke(player);
                        if (sel instanceof Integer) selectedSlot = (Integer) sel;
                    } catch (Throwable ignored2) {
                    }
                }
                Object attackReq = ctor.newInstance(lastIndex, false, selectedSlot, 0, new int[0]);
                // Try to obtain player's ServerGamePacketListenerImpl instance (connection)
                Object listener = null;
                try {
                    java.lang.reflect.Field f = player.getClass().getDeclaredField("connection");
                    f.setAccessible(true);
                    listener = f.get(player);
                } catch (Throwable ignored) {
                    try {
                        listener = player.getClass().getMethod("connection").invoke(player);
                    } catch (Throwable ignored2) {
                        for (java.lang.reflect.Field ff : player.getClass().getDeclaredFields()) {
                            if (ff.getType().getName().equals("net.minecraft.server.network.ServerGamePacketListenerImpl")) {
                                ff.setAccessible(true);
                                listener = ff.get(player);
                                break;
                            }
                        }
                    }
                }
                try {
                    if (listener != null) {
                        serverNetworkClass.getMethod("handleAttackRequest", attackReqClass, net.minecraft.server.MinecraftServer.class, net.minecraft.server.level.ServerPlayer.class, Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl")).invoke(null, attackReq, player.getServer(), player, listener);
                    } else {
                        // fallback to overload without listener if present
                        serverNetworkClass.getMethod("handleAttackRequest", attackReqClass, net.minecraft.server.MinecraftServer.class, net.minecraft.server.level.ServerPlayer.class).invoke(null, attackReq, player.getServer(), player);
                    }
                } catch (NoSuchMethodException ignored) {
                    // ignore
                }
            } catch (ReflectiveOperationException ignored) {
                // ignore; no BetterCombat integration available
            }
            // Always send a client-side SacrilegiousSlamPayload as a fallback so the client can run local visual playback
            try {
                PacketDistributor.sendToPlayer(player, new com.fiv.fiverkas_weapons.network.SacrilegiousSlamPayload(player.getId(), "bettercombat:two_handed_slam"));
            } catch (Throwable t) {
            }
        });
    }
}
