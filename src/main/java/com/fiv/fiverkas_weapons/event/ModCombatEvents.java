package com.fiv.fiverkas_weapons.event;

import com.fiv.fiverkas_weapons.registry.ModEffects;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.slf4j.Logger;

public class ModCombatEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int VAPORIFIED_DURATION_TICKS = 80;
    private static final int SACRILEGIOUS_SLOWNESS_DURATION_TICKS = 80;
    private static final int SACRILEGIOUS_PARTICLE_COUNT = 12;

    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (target.level().isClientSide) {
            return;
        }
        LivingEntity attacker = event.getEntity();
        boolean hasVaporwaveSword = attacker.getMainHandItem().is(ModItems.VAPORWAVE_SWORD.get())
                || attacker.getOffhandItem().is(ModItems.VAPORWAVE_SWORD.get());
        boolean hasSacrilegious = attacker.getMainHandItem().is(ModItems.SACRILEGIOUS.get())
                || attacker.getOffhandItem().is(ModItems.SACRILEGIOUS.get());
        if (!hasVaporwaveSword && !hasSacrilegious) {
            return;
        }
        if (hasVaporwaveSword) {
            LOGGER.info("[fweapons] AttackEntityEvent applying vaporified: attacker={} target={}", attacker.getName().getString(), target.getName().getString());
            target.addEffect(new MobEffectInstance(ModEffects.VAPORIFIED, VAPORIFIED_DURATION_TICKS, 0), attacker);
        }
        if (hasSacrilegious) {
            LOGGER.info("[fweapons] AttackEntityEvent applying sacrilegious effects: attacker={} target={}", attacker.getName().getString(), target.getName().getString());
            applySacrilegiousHitEffects(target, attacker);
        }
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0F) {
            return;
        }
        applyFromSource(event.getEntity(), event.getSource());
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        // Fallback path for any direct melee damage systems that may skip incoming checks.
        applyFromSource(event.getEntity(), event.getSource());
    }

    private static void applyFromSource(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide) {
            return;
        }

        ItemStack weaponFromSource = source.getWeaponItem();
        boolean isVaporwaveSword = weaponFromSource != null && !weaponFromSource.isEmpty() && weaponFromSource.is(ModItems.VAPORWAVE_SWORD.get());
        boolean isSacrilegious = weaponFromSource != null && !weaponFromSource.isEmpty() && weaponFromSource.is(ModItems.SACRILEGIOUS.get());

        Entity causing = source.getEntity();
        Entity direct = source.getDirectEntity();
        LivingEntity attacker = null;
        if (causing instanceof LivingEntity causingLiving) {
            attacker = causingLiving;
        } else if (direct instanceof LivingEntity directLiving) {
            attacker = directLiving;
        }

        if (!isVaporwaveSword && attacker != null) {
            isVaporwaveSword = attacker.getMainHandItem().is(ModItems.VAPORWAVE_SWORD.get())
                    || attacker.getOffhandItem().is(ModItems.VAPORWAVE_SWORD.get());
        }
        if (!isSacrilegious && attacker != null) {
            isSacrilegious = attacker.getMainHandItem().is(ModItems.SACRILEGIOUS.get())
                    || attacker.getOffhandItem().is(ModItems.SACRILEGIOUS.get());
        }

        if (!isVaporwaveSword && !isSacrilegious) {
            if (attacker != null && attacker.getType().toString().contains("player")) {
                LOGGER.info(
                        "[fweapons] skipped source match: msgId={} weapon={} main={} off={}",
                        source.getMsgId(),
                        weaponFromSource,
                        attacker.getMainHandItem(),
                        attacker.getOffhandItem()
                );
            }
            return;
        }

        if (isVaporwaveSword) {
            LOGGER.info("[fweapons] Damage hook applying vaporified: msgId={} attacker={} target={}",
                    source.getMsgId(),
                    attacker == null ? "<none>" : attacker.getName().getString(),
                    target.getName().getString());
            target.addEffect(new MobEffectInstance(ModEffects.VAPORIFIED, VAPORIFIED_DURATION_TICKS, 0), attacker);
        }
        if (isSacrilegious) {
            LOGGER.info("[fweapons] Damage hook applying sacrilegious effects: msgId={} attacker={} target={}",
                    source.getMsgId(),
                    attacker == null ? "<none>" : attacker.getName().getString(),
                    target.getName().getString());
            applySacrilegiousHitEffects(target, attacker);
        }
    }

    private static void applySacrilegiousHitEffects(LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SACRILEGIOUS_SLOWNESS_DURATION_TICKS, 0), attacker);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    DustParticleOptions.REDSTONE,
                    target.getX(),
                    target.getY(0.6),
                    target.getZ(),
                    SACRILEGIOUS_PARTICLE_COUNT,
                    0.3,
                    0.25,
                    0.3,
                    0.0
            );
        }
    }
}
