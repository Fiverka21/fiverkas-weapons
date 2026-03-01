package com.fiv.fiverkas_weapons.event;

import com.fiv.fiverkas_weapons.effect.CeruleanShroudEffect;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.fiv.fiverkas_weapons.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.util.StringUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
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
import java.util.HashSet;
import java.util.Set;

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
    private static final String AIRMACE_FALL_DISTANCE_TAG = "fweapons_airmace_fall_distance";
    private static final String AIRMACE_FALL_TICK_TAG = "fweapons_airmace_fall_tick";
    private static final int AIRMACE_FALL_TICK_WINDOW = 2;
    private static final DustParticleOptions AIRMACE_LIGHT_YELLOW =
            new DustParticleOptions(new Vector3f(241 / 255F, 206 / 255F, 106 / 255F), 1.25F);
    private static final DustParticleOptions AIRMACE_BLAND_CYAN =
            new DustParticleOptions(new Vector3f(146 / 255F, 191 / 255F, 186 / 255F), 1.1F);
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
        boolean isAirmaceAttack = isAirmaceAttack(attacker);
        if (isAirmaceAttack || isHoldingAirmace(attacker)) {
            recordAirmaceSmash(attacker);
        }
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
        applyAirmaceFallBonus(event);
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

    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (!left.is(ModItems.AIRMACE.get())) {
            return;
        }
        ItemStack right = event.getRight();
        if (right.isEmpty()) {
            return;
        }

        ItemEnchantments leftEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(left);
        ItemEnchantments rightEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(right);
        if (rightEnchantments.isEmpty()) {
            return;
        }

        // Allow the Airmace to combine Breach + Density even though they're normally exclusive.
        boolean hasSpecialConflict = false;
        Set<net.minecraft.core.Holder<Enchantment>> seen = new HashSet<>(leftEnchantments.keySet());
        for (var entry : rightEnchantments.entrySet()) {
            var enchantment = entry.getKey();
            if (!left.supportsEnchantment(enchantment) && !isBreachDensityEnchantment(enchantment)) {
                return;
            }
            for (var existing : seen) {
                if (Enchantment.areCompatible(enchantment, existing)) {
                    continue;
                }
                if (isBreachDensityPair(enchantment, existing)) {
                    hasSpecialConflict = true;
                    continue;
                }
                return;
            }
            seen.add(enchantment);
        }

        if (!hasSpecialConflict) {
            return;
        }

        ItemStack output = left.copy();
        boolean rightIsBook = right.has(DataComponents.STORED_ENCHANTMENTS);
        int cost = 0;

        if (!rightIsBook && output.isDamageableItem() && output.is(right.getItem())) {
            int remaining = output.getMaxDamage() - output.getDamageValue();
            int rightRemaining = right.getMaxDamage() - right.getDamageValue();
            int bonus = rightRemaining + output.getMaxDamage() * 12 / 100;
            int combined = remaining + bonus;
            int newDamage = output.getMaxDamage() - combined;
            if (newDamage < 0) {
                newDamage = 0;
            }
            if (newDamage < output.getDamageValue()) {
                output.setDamageValue(newDamage);
                cost += 2;
            }
        }

        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(leftEnchantments);
        for (var entry : rightEnchantments.entrySet()) {
            var holder = entry.getKey();
            Enchantment enchantment = holder.value();
            int currentLevel = merged.getLevel(holder);
            int incomingLevel = entry.getIntValue();
            int newLevel = currentLevel == incomingLevel ? incomingLevel + 1 : Math.max(currentLevel, incomingLevel);
            if (newLevel > enchantment.getMaxLevel()) {
                newLevel = enchantment.getMaxLevel();
            }
            merged.set(holder, newLevel);
            int addCost = enchantment.getAnvilCost();
            if (rightIsBook) {
                addCost = Math.max(1, addCost / 2);
            }
            cost += addCost * newLevel;
        }

        if (cost <= 0) {
            return;
        }

        EnchantmentHelper.setEnchantments(output, merged.toImmutable());

        int renameCost = 0;
        String name = event.getName();
        if (name != null) {
            if (!StringUtil.isBlank(name)) {
                if (!name.equals(output.getHoverName().getString())) {
                    renameCost = 1;
                    output.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                }
            } else if (output.has(DataComponents.CUSTOM_NAME)) {
                renameCost = 1;
                output.remove(DataComponents.CUSTOM_NAME);
            }
        }

        long baseCost = (long) left.getOrDefault(DataComponents.REPAIR_COST, 0)
                + (long) right.getOrDefault(DataComponents.REPAIR_COST, 0);
        long totalCost = Mth.clamp(baseCost + cost + renameCost, 0L, Integer.MAX_VALUE);
        if (totalCost >= 40 && !event.getPlayer().getAbilities().instabuild) {
            return;
        }

        int repairCost = output.getOrDefault(DataComponents.REPAIR_COST, 0);
        int rightRepairCost = right.getOrDefault(DataComponents.REPAIR_COST, 0);
        if (repairCost < rightRepairCost) {
            repairCost = rightRepairCost;
        }
        output.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(repairCost));

        event.setOutput(output);
        event.setCost(totalCost);
        event.setMaterialCost(1);
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

    private static void applyAirmaceFallBonus(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        DamageSource source = event.getSource();
        LivingEntity attacker = null;
        Entity causing = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (causing instanceof LivingEntity causingLiving) {
            attacker = causingLiving;
        } else if (direct instanceof LivingEntity directLiving) {
            attacker = directLiving;
        }
        if (attacker == null) {
            return;
        }

        ItemStack weaponFromSource = source.getWeaponItem();
        ItemStack airmaceStack = null;
        if (weaponFromSource != null && !weaponFromSource.isEmpty() && weaponFromSource.is(ModItems.AIRMACE.get())) {
            airmaceStack = weaponFromSource;
        } else if (attacker.getMainHandItem().is(ModItems.AIRMACE.get())) {
            airmaceStack = attacker.getMainHandItem();
        } else if (attacker.getOffhandItem().is(ModItems.AIRMACE.get())) {
            airmaceStack = attacker.getOffhandItem();
        }

        boolean isAirmaceAttack = airmaceStack != null || isAirmaceAttack(attacker) || hasRecentAirmaceSmash(attacker);
        if (!isAirmaceAttack) {
            return;
        }

        float fallDistance = attacker.fallDistance;
        if (hasRecentAirmaceSmash(attacker)) {
            fallDistance = Math.max(fallDistance, getStoredAirmaceFallDistance(attacker));
        }
        if (!canAirmaceSmash(attacker, fallDistance)) {
            clearAirmaceSmash(attacker);
            return;
        }

        float bonus = calculateMaceFallBonus(attacker, event.getEntity(), source, fallDistance, airmaceStack);
        if (bonus > 0.0F) {
            event.setAmount(event.getAmount() + bonus);
            if (attacker.level() instanceof ServerLevel serverLevel) {
                spawnAirmaceSmashParticles(serverLevel, event.getEntity(), fallDistance);
            }
        }
        clearAirmaceSmash(attacker);
    }

    private static float calculateMaceFallBonus(
            LivingEntity attacker,
            LivingEntity target,
            DamageSource source,
            float fallDistance,
            ItemStack airmaceStack
    ) {
        if (!canAirmaceSmash(attacker, fallDistance)) {
            return 0.0F;
        }

        float baseBonus;
        if (fallDistance <= 3.0F) {
            baseBonus = 4.0F * fallDistance;
        } else if (fallDistance <= 8.0F) {
            baseBonus = 12.0F + 2.0F * (fallDistance - 3.0F);
        } else {
            baseBonus = 22.0F + fallDistance - 8.0F;
        }

        if (attacker.level() instanceof ServerLevel serverLevel) {
            ItemStack weaponStack = airmaceStack == null || airmaceStack.isEmpty()
                    ? attacker.getWeaponItem()
                    : airmaceStack;
            float enchantBonus = net.minecraft.world.item.enchantment.EnchantmentHelper
                    .modifyFallBasedDamage(serverLevel, weaponStack, target, source, 0.0F)
                    * fallDistance;
            return baseBonus + enchantBonus;
        }

        return baseBonus;
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

    private static boolean isAirmaceAttack(LivingEntity attacker) {
        try {
            Object currentAttack = attacker.getClass().getMethod("getCurrentAttack").invoke(attacker);
            if (currentAttack == null) {
                return false;
            }

            Object attackItem = currentAttack.getClass().getMethod("itemStack").invoke(currentAttack);
            if (!(attackItem instanceof ItemStack attackStack)) {
                return false;
            }
            return attackStack.is(ModItems.AIRMACE.get());
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isHoldingBayonet(LivingEntity attacker) {
        return attacker.getMainHandItem().is(ModItems.BAYONET.get())
                || attacker.getOffhandItem().is(ModItems.BAYONET.get());
    }

    private static boolean isHoldingAirmace(LivingEntity attacker) {
        return attacker.getMainHandItem().is(ModItems.AIRMACE.get())
                || attacker.getOffhandItem().is(ModItems.AIRMACE.get());
    }

    private static boolean isBreachDensityPair(net.minecraft.core.Holder<Enchantment> first, net.minecraft.core.Holder<Enchantment> second) {
        return (first.is(Enchantments.BREACH) && second.is(Enchantments.DENSITY))
                || (first.is(Enchantments.DENSITY) && second.is(Enchantments.BREACH));
    }

    private static boolean isBreachDensityEnchantment(net.minecraft.core.Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.BREACH) || enchantment.is(Enchantments.DENSITY);
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

    private static void spawnAirmaceSmashParticles(ServerLevel serverLevel, LivingEntity target, float fallDistance) {
        int totalCount = Math.min(140, 14 + Math.round(fallDistance * 9.0F));
        int primaryCount = totalCount / 2;
        int secondaryCount = totalCount - primaryCount;
        double spread = Math.min(1.6D, 0.35D + fallDistance * 0.08D);
        double speed = Math.min(0.35D, 0.08D + fallDistance * 0.02D);
        double x = target.getX();
        double y = target.getY(0.6D);
        double z = target.getZ();

        serverLevel.sendParticles(AIRMACE_LIGHT_YELLOW, x, y, z, primaryCount, spread, spread * 0.6D, spread, speed);
        serverLevel.sendParticles(AIRMACE_BLAND_CYAN, x, y, z, secondaryCount, spread, spread * 0.6D, spread, speed);

        int burstCount = totalCount;
        double burstSpeed = Math.min(0.55D, 0.2D + fallDistance * 0.04D);
        RandomSource random = serverLevel.getRandom();
        for (int i = 0; i < burstCount; i++) {
            double dx = random.nextGaussian();
            double dy = random.nextGaussian() * 0.6D;
            double dz = random.nextGaussian();
            Vec3 direction = new Vec3(dx, dy, dz);
            if (direction.lengthSqr() < 1.0E-6) {
                continue;
            }
            direction = direction.normalize();
            DustParticleOptions particle = (i & 1) == 0 ? AIRMACE_LIGHT_YELLOW : AIRMACE_BLAND_CYAN;
            serverLevel.sendParticles(particle, x, y, z, 0, direction.x, direction.y, direction.z, burstSpeed);
        }
    }

    private static void recordAirmaceSmash(LivingEntity attacker) {
        if (!canAirmaceSmash(attacker, attacker.fallDistance)) {
            return;
        }
        var data = attacker.getPersistentData();
        data.putFloat(AIRMACE_FALL_DISTANCE_TAG, attacker.fallDistance);
        data.putInt(AIRMACE_FALL_TICK_TAG, attacker.tickCount);
    }

    private static boolean hasRecentAirmaceSmash(LivingEntity attacker) {
        int recordedTick = attacker.getPersistentData().getInt(AIRMACE_FALL_TICK_TAG);
        return recordedTick != 0 && attacker.tickCount - recordedTick <= AIRMACE_FALL_TICK_WINDOW;
    }

    private static float getStoredAirmaceFallDistance(LivingEntity attacker) {
        return attacker.getPersistentData().getFloat(AIRMACE_FALL_DISTANCE_TAG);
    }

    private static void clearAirmaceSmash(LivingEntity attacker) {
        var data = attacker.getPersistentData();
        data.remove(AIRMACE_FALL_DISTANCE_TAG);
        data.remove(AIRMACE_FALL_TICK_TAG);
    }

    private static boolean canAirmaceSmash(LivingEntity attacker, float fallDistance) {
        return fallDistance > 1.5F && !attacker.isFallFlying();
    }

}
