package com.fiv.fiverkas_weapons;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import com.fiv.fiverkas_weapons.event.client.ModCombatClientEvents;
import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.event.ModCommandEvents;
import com.fiv.fiverkas_weapons.network.ModNetwork;
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
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, ModNetwork::onRegisterPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(AttackEntityEvent.class, ModCombatEvents::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(ProjectileImpactEvent.class, ModCombatEvents::onProjectileImpact);
        NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, ModCombatEvents::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Pre.class, ModCombatEvents::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Post.class, ModCombatEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, ModCombatEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EntityTickEvent.Post.class, ModCombatEvents::onEntityTickPost);
        NeoForge.EVENT_BUS.addListener(LivingChangeTargetEvent.class, ModCombatEvents::onLivingChangeTarget);
        NeoForge.EVENT_BUS.addListener(SweepAttackEvent.class, ModCombatEvents::onSweepAttack);
        NeoForge.EVENT_BUS.addListener(ServerStartingEvent.class, ModCombatEvents::onServerStarting);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, ModCombatEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(AnvilUpdateEvent.class, ModCombatEvents::onAnvilUpdate);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, ModCommandEvents::onRegisterCommands);

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(FMLClientSetupEvent.class, ModCombatClientEvents::onClientSetup);
        }
    }
}
