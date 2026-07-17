package com.fiv.fiverkas_weapons;

import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.event.ModCommandEvents;
import com.fiv.fiverkas_weapons.network.ModNetwork;
import com.fiv.fiverkas_weapons.registry.ModCreativeTabs;
import com.fiv.fiverkas_weapons.registry.ModDataComponents;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.fiv.fiverkas_weapons.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;

public class FiverkasWeapons implements ModInitializer {

    public static final String MODID = "fweapons";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        // On Fabric, registries write straight to the vanilla Registry — there's
        // no mod event bus to register against. Referencing/calling these
        // classes forces their contents to actually register. We'll define
        // init() in each once we port the registry classes themselves.
        ModDataComponents.init();
        ModItems.init();
        ModEffects.init();
        ModCreativeTabs.init();
        ModSounds.init();

        // Config: NeoForge's ModConfig.Type.CLIENT + ModContainer#registerConfig
        // has no Fabric equivalent — we'll rebuild ModClientConfig on top of
        // Cloth Config (already a dependency for Better Combat) when we get
        // to that file.
        // ModClientConfig.load();

        ModNetwork.registerPayloads();

        CommandRegistrationCallback.EVENT.register(ModCommandEvents::onRegisterCommands);
        ServerTickEvents.END_SERVER_TICK.register(ModCombatEvents::onServerTick);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ModCombatEvents.onAttackEntity(player, entity);
            return InteractionResult.PASS;
        });
    }
}
