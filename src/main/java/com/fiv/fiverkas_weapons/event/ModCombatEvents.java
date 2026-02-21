package com.fiv.fiverkas_weapons.event;

import com.fiv.fiverkas_weapons.registry.ModEffects;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
        if (!hasVaporwaveSword) {
            return;
        }
        LOGGER.info("[fweapons] AttackEntityEvent applying vaporified: attacker={} target={}", attacker.getName().getString(), target.getName().getString());
        target.addEffect(new MobEffectInstance(ModEffects.VAPORIFIED, VAPORIFIED_DURATION_TICKS, 0), attacker);
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

        if (!isVaporwaveSword) {
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

        LOGGER.info("[fweapons] Damage hook applying vaporified: msgId={} attacker={} target={}",
                source.getMsgId(),
                attacker == null ? "<none>" : attacker.getName().getString(),
                target.getName().getString());
        target.addEffect(new MobEffectInstance(ModEffects.VAPORIFIED, VAPORIFIED_DURATION_TICKS, 0), attacker);
    }
}
