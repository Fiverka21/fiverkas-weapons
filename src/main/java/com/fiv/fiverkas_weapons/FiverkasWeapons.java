package com.fiv.fiverkas_weapons;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import com.fiv.fiverkas_weapons.event.client.ModCombatClientEvents;
import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.event.ModCommandEvents;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import com.fiv.fiverkas_weapons.registry.ModCreativeTabs;
import com.fiv.fiverkas_weapons.registry.ModSounds;

@Mod(FiverkasWeapons.MODID)
public class FiverkasWeapons {
    public static final String MODID = "fweapons";

    // Constructor called with NeoForge mod event bus
    public FiverkasWeapons(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);   // Register items
        ModEffects.EFFECTS.register(modEventBus); // Register effects
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus); // Register creative tabs
        ModSounds.SOUND_EVENTS.register(modEventBus); // Register sounds
        NeoForge.EVENT_BUS.addListener(ModCombatEvents::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(ModCombatEvents::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(ModCombatEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(ModCombatEvents::onLivingChangeTarget);
        NeoForge.EVENT_BUS.addListener(ModCombatEvents::onSweepAttack);
        NeoForge.EVENT_BUS.addListener(ModCombatEvents::onServerStarting);
        NeoForge.EVENT_BUS.addListener(ModCombatEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ModCombatEvents::onAnvilUpdate);
        NeoForge.EVENT_BUS.addListener(ModCommandEvents::onRegisterCommands);

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(ModCombatClientEvents::onClientSetup);
        }
    }
}
