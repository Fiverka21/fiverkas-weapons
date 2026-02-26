package com.fiv.fiverkas_weapons.event;

import com.fiv.fiverkas_weapons.effect.CeruleanShroudEffect;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.fiv.fiverkas_weapons.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.lang.reflect.Field;

public class ModCombatEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int VAPORIFIED_DURATION_TICKS = 120;
    private static final int SACRILEGIOUS_BLEED_DURATION_TICKS = 100;
    private static final int SACRILEGIOUS_SLOWNESS_DURATION_TICKS = 100;
    private static final int SACRILEGIOUS_PARTICLE_COUNT = 12;
    private static final int MKOPI_DARKNESS_DURATION_TICKS = 80;
    private static final int MKOPI_PARTICLE_COUNT = 40;
    private static final int MKOPI_BLACK_PARTICLE_COUNT = 32;
    private static final int BAYONET_GUNSHOT_PARTICLE_COUNT = 32;
    private static final String MKOPI_SLAM_ANIMATION = "bettercombat:two_handed_slam";
    private static final String BAYONET_GUNSHOT_ANIMATION = "fweapons:bayonet_no_swing";
    private static final String BAYONET_GUNSHOT_HITBOX = "FORWARD_BOX";
    private static final DustParticleOptions MKOPI_BLACK_DUST = new DustParticleOptions(new Vector3f(0.02F, 0.02F, 0.02F), 1.1F);

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
        boolean isMkopiSlamAttack = isMkopiSlamAttack(attacker);
        boolean isBayonetGunshotAttack = isBayonetGunshotAttack(attacker);
        if (!hasVaporwaveSword && !hasSacrilegious && !isMkopiSlamAttack && !isBayonetGunshotAttack) {
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
        if (isMkopiSlamAttack) {
            LOGGER.info("[fweapons] AttackEntityEvent applying mkopi slam effects: attacker={} target={}", attacker.getName().getString(), target.getName().getString());
            applyMkopiSlamEffects(target, attacker);
        }
        if (isBayonetGunshotAttack) {
            LOGGER.info("[fweapons] AttackEntityEvent applying bayonet gunshot particles: attacker={} target={}", attacker.getName().getString(), target.getName().getString());
            applyBayonetGunshotParticles(target);
        }
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0F) {
            return;
        }

        applyHonorStrike(event);
        applyFromSource(event.getEntity(), event.getSource());
    }

    public static void onSweepAttack(SweepAttackEvent event) {
        if (!event.isSweeping()) {
            return;
        }
        if (isHoldingBayonet(event.getEntity())) {
            event.setSweeping(false);
            event.setCanceled(true);
        }
    }

    public static void onServerStarting(ServerStartingEvent event) {
        disableBetterCombatReworkedSweepParticles();
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        // Fallback path for any direct melee damage systems that may skip incoming checks.
        applyFromSource(event.getEntity(), event.getSource());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }

        var data = player.getPersistentData();
        boolean hasShroud = player.hasEffect(ModEffects.CERULEAN_SHROUD);
        boolean markedInvisible = data.getBoolean(CeruleanShroudEffect.INVISIBLE_TAG);

        if (hasShroud) {
            if (!player.isInvisible()) {
                player.setInvisible(true);
            }
            if (!markedInvisible) {
                data.putBoolean(CeruleanShroudEffect.INVISIBLE_TAG, true);
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                CeruleanShroudEffect.spawnFootsteps(serverLevel, player);
                if (player.tickCount % 5 == 0) {
                    clearNearbyMobTargets(serverLevel, player);
                }
            }
            return;
        }

        if (markedInvisible) {
            if (!player.hasEffect(MobEffects.INVISIBILITY)) {
                player.setInvisible(false);
            }
            data.remove(CeruleanShroudEffect.INVISIBLE_TAG);
            data.remove(CeruleanShroudEffect.LAST_X_TAG);
            data.remove(CeruleanShroudEffect.LAST_Y_TAG);
            data.remove(CeruleanShroudEffect.LAST_Z_TAG);
            data.remove(CeruleanShroudEffect.STEP_PROGRESS_TAG);
        }
    }

    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target instanceof Player player && player.hasEffect(ModEffects.CERULEAN_SHROUD)) {
            event.setNewAboutToBeSetTarget(null);
        }
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

    private static void applyHonorStrike(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }
        if (!attacker.hasEffect(ModEffects.CERULEAN_SHROUD)) {
            return;
        }

        event.setAmount(event.getAmount() * 2.0F);
        attacker.removeEffect(ModEffects.CERULEAN_SHROUD);
        attacker.getPersistentData().putDouble(CeruleanShroudEffect.STEP_PROGRESS_TAG, 0.0D);
        attacker.getPersistentData().remove(CeruleanShroudEffect.LAST_X_TAG);
        attacker.getPersistentData().remove(CeruleanShroudEffect.LAST_Y_TAG);
        attacker.getPersistentData().remove(CeruleanShroudEffect.LAST_Z_TAG);
    }

    private static void clearNearbyMobTargets(ServerLevel serverLevel, Player player) {
        AABB area = player.getBoundingBox().inflate(32.0D);
        for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, area, mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
            mob.setLastHurtByPlayer(null);
            mob.setLastHurtByMob(null);
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
        target.addEffect(new MobEffectInstance(ModEffects.BLEED, SACRILEGIOUS_BLEED_DURATION_TICKS, 0), attacker);
    }

    private static boolean isMkopiSlamAttack(LivingEntity attacker) {
        try {
            Object currentAttack = attacker.getClass().getMethod("getCurrentAttack").invoke(attacker);
            if (currentAttack == null) {
                return false;
            }

            Object attackItem = currentAttack.getClass().getMethod("itemStack").invoke(currentAttack);
            if (!(attackItem instanceof ItemStack attackStack) || !attackStack.is(ModItems.MKOPI.get())) {
                return false;
            }

            Object attack = currentAttack.getClass().getMethod("attack").invoke(currentAttack);
            if (attack == null) {
                return false;
            }

            Object hitbox = attack.getClass().getMethod("hitbox").invoke(attack);
            if (hitbox == null || !"VERTICAL_PLANE".equals(hitbox.toString())) {
                return false;
            }

            Object animation = attack.getClass().getMethod("animation").invoke(attack);
            return MKOPI_SLAM_ANIMATION.equals(animation);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isBayonetGunshotAttack(LivingEntity attacker) {
        try {
            Object currentAttack = attacker.getClass().getMethod("getCurrentAttack").invoke(attacker);
            if (currentAttack == null) {
                return false;
            }

            Object attackItem = currentAttack.getClass().getMethod("itemStack").invoke(currentAttack);
            if (!(attackItem instanceof ItemStack attackStack) || !attackStack.is(ModItems.BAYONET.get())) {
                return false;
            }

            Object attack = currentAttack.getClass().getMethod("attack").invoke(currentAttack);
            if (attack == null) {
                return false;
            }

            Object hitbox = attack.getClass().getMethod("hitbox").invoke(attack);
            if (hitbox == null) {
                return false;
            }
            if (!BAYONET_GUNSHOT_HITBOX.equals(hitbox.toString())) {
                return false;
            }

            Object animation = attack.getClass().getMethod("animation").invoke(attack);
            if (!BAYONET_GUNSHOT_ANIMATION.equals(animation)) {
                return false;
            }
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isHoldingBayonet(LivingEntity attacker) {
        return attacker.getMainHandItem().is(ModItems.BAYONET.get())
                || attacker.getOffhandItem().is(ModItems.BAYONET.get());
    }

    private static void disableBetterCombatReworkedSweepParticles() {
        try {
            Class<?> betterCombatModClass = Class.forName("net.bettercombat.BetterCombatMod");
            Field configField = betterCombatModClass.getField("config");
            Object config = configField.get(null);
            if (config == null) {
                return;
            }

            setBooleanField(config, "reworked_sweeping_emits_particles", false);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void setBooleanField(Object target, String fieldName, boolean value) throws ReflectiveOperationException {
        Field field = target.getClass().getField(fieldName);
        field.setBoolean(target, value);
    }

    private static void applyMkopiSlamEffects(LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, MKOPI_DARKNESS_DURATION_TICKS, 0), attacker);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    target.getX(),
                    target.getY(0.6),
                    target.getZ(),
                    MKOPI_PARTICLE_COUNT,
                    0.45,
                    0.35,
                    0.45,
                    0.12
            );
            serverLevel.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    target.getX(),
                    target.getY(0.6),
                    target.getZ(),
                    MKOPI_PARTICLE_COUNT,
                    0.4,
                    0.28,
                    0.4,
                    0.04
            );
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    target.getX(),
                    target.getY(0.6),
                    target.getZ(),
                    MKOPI_PARTICLE_COUNT + 20,
                    0.5,
                    0.35,
                    0.5,
                    0.06
            );
            serverLevel.sendParticles(
                    MKOPI_BLACK_DUST,
                    target.getX(),
                    target.getY(0.6),
                    target.getZ(),
                    MKOPI_BLACK_PARTICLE_COUNT,
                    0.42,
                    0.25,
                    0.42,
                    0.0
            );

            serverLevel.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    ModSounds.MKOPI.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }

    private static void applyBayonetGunshotParticles(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                target.getX(),
                target.getY(0.7),
                target.getZ(),
                BAYONET_GUNSHOT_PARTICLE_COUNT,
                0.45,
                0.35,
                0.45,
                0.12
        );
        serverLevel.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                target.getX(),
                target.getY(0.7),
                target.getZ(),
                BAYONET_GUNSHOT_PARTICLE_COUNT,
                0.4,
                0.28,
                0.4,
                0.04
        );
        serverLevel.sendParticles(
                ParticleTypes.SMOKE,
                target.getX(),
                target.getY(0.7),
                target.getZ(),
                BAYONET_GUNSHOT_PARTICLE_COUNT + 20,
                0.5,
                0.35,
                0.5,
                0.06
        );
        serverLevel.sendParticles(
                MKOPI_BLACK_DUST,
                target.getX(),
                target.getY(0.7),
                target.getZ(),
                BAYONET_GUNSHOT_PARTICLE_COUNT,
                0.42,
                0.25,
                0.42,
                0.0
        );
    }

}
