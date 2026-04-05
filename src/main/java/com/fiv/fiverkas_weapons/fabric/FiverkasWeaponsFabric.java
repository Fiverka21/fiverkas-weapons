package com.fiv.fiverkas_weapons.fabric;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SimpleEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class FiverkasWeaponsFabric implements ModInitializer {
    public static final SimpleEventBus MOD_EVENT_BUS = new SimpleEventBus();
    private static boolean initialized;

    @Override
    public void onInitialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        new FiverkasWeapons(MOD_EVENT_BUS);
        MOD_EVENT_BUS.post(new RegisterPayloadHandlersEvent());

        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                NeoForge.EVENT_BUS.post(new ServerStartingEvent())
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) ->
                NeoForge.EVENT_BUS.post(new RegisterCommandsEvent(dispatcher, context))
        );

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            NeoForge.EVENT_BUS.post(new AttackEntityEvent(player, entity));
            return InteractionResult.PASS;
        });

        // Fabric replacement for NeoForge global loot modifiers.
        LootTableEvents.MODIFY.register((key, tableBuilder, source, provider) -> {
            if (!source.isBuiltin()) {
                return;
            }
            if (!key.identifier().equals(Identifier.withDefaultNamespace("entities/warden"))) {
                return;
            }
            tableBuilder.pool(
                    LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .add(LootItem.lootTableItem(ModItems.DREAM_ESSENCE.get()))
                            .build()
            );
        });
    }
}
