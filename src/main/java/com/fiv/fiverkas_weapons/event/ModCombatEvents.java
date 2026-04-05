package com.fiv.fiverkas_weapons.event;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.effect.CeruleanShroudEffect;
import com.fiv.fiverkas_weapons.item.LScythe;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.fiv.fiverkas_weapons.registry.ModSounds;
import com.fiv.fiverkas_weapons.util.CompatIds;
import com.fiv.fiverkas_weapons.util.EntityDataUtil;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.util.StringUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ModCombatEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG_COMBAT_LOGS = Boolean.getBoolean("fweapons.debugCombatLogs");
    private static final int VAPORIFIED_DURATION_TICKS = 120;
    private static final int SUNSET_DURATION_TICKS = 80;
    private static final int SACRILEGIOUS_BLEED_DURATION_TICKS = 100;
    private static final int SACRILEGIOUS_SLOWNESS_DURATION_TICKS = 100;
    private static final int SACRILEGIOUS_PARTICLE_COUNT = 12;
    private static final int MKOPI_DARKNESS_DURATION_TICKS = 80;
    private static final int MKOPI_PARTICLE_COUNT = 40;
    private static final int MKOPI_BLACK_PARTICLE_COUNT = 32;
    private static final int BAYONET_GUNSHOT_PARTICLE_COUNT = 32;
    private static final int BAYONET_GUNSHOT_MUZZLE_COUNT = 18;
    private static final long CLIENT_ATTACK_FLAG_WINDOW_TICKS = 8L;
    private static final int THE_FOOL_SPECTRAL_DURATION_TICKS = 200;
    private static final String THE_FOOL_SPECTRAL_BONUS_TAG = "fweapons_thefool_spectral_bonus";
    private static final Map<UUID, EnumMap<ClientAttackFlag, Long>> CLIENT_ATTACK_FLAGS = new HashMap<>();
    private static final EquipmentSlot[] CERULEAN_EQUIPMENT_SLOTS = {
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final String MKOPI_SLAM_ANIMATION = "bettercombat:two_handed_slam";
    private static final String BAYONET_GUNSHOT_ANIMATION = "fweapons:bayonet_no_swing";
    private static final String BAYONET_IMPACT_ANIMATION = "fweapons:bayonet_impact";
    private static final String BAYONET_GUNSHOT_HITBOX = "FORWARD_BOX";
    private static final String DUSK_THIRD_ANIMATION_PRIMARY = "bettercombat:dual_handed_stab";
    private static final String DUSK_THIRD_ANIMATION_FALLBACK = "bettercombat:one_handed_stab";
    private static final String DUSK_THIRD_HITBOX = "FORWARD_BOX";
    private static final String AIRMACE_FALL_DISTANCE_TAG = "fweapons_airmace_fall_distance";
    private static final String AIRMACE_FALL_TICK_TAG = "fweapons_airmace_fall_tick";
    private static final int AIRMACE_FALL_TICK_WINDOW = 2;
    private static final ResourceKey<DamageType> VAPORIFIED_DAMAGE = CompatIds.resourceKey(
            Registries.DAMAGE_TYPE,
            FiverkasWeapons.MODID,
            "vaporified"
    );
    private static final ResourceKey<DamageType> SUNSET_DAMAGE = CompatIds.resourceKey(
            Registries.DAMAGE_TYPE,
            FiverkasWeapons.MODID,
            "sunset"
    );
    private static final ResourceKey<MobEffect> SLOWNESS_EFFECT = CompatIds.resourceKey(
            Registries.MOB_EFFECT,
            "minecraft",
            "slowness"
    );
    private static final DustParticleOptions AIRMACE_LIGHT_YELLOW = new DustParticleOptions(0xF1CE6A, 1.25F);
    private static final DustParticleOptions AIRMACE_BLAND_CYAN = new DustParticleOptions(0x92BFBA, 1.1F);
    private static final DustParticleOptions DUSK_SUNSET_DUST = new DustParticleOptions(0x5B3C88, 1.35F);
    private static final DustParticleOptions DUSK_SUNSET_LIGHT_DUST = new DustParticleOptions(0x78BEFF, 1.25F);
    private static final DustParticleOptions MKOPI_BLACK_DUST = new DustParticleOptions(0x050505, 1.1F);
    private static final Field ARROW_IN_GROUND_FIELD = resolveArrowField("inGround");
    private static final Field ARROW_IN_GROUND_TIME_FIELD = resolveArrowField("inGroundTime");
    private static final Method ARROW_GET_BASE_DAMAGE_METHOD = resolveArrowMethod("getBaseDamage");
    private static final Field ARROW_BASE_DAMAGE_FIELD = resolveArrowField("baseDamage");
    private static final ParticleSpec[] BAYONET_MUZZLE_SPECS = new ParticleSpec[]{
            new ParticleSpec(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), 1, 0.0, 0.0, 0.0, 0.0),
            new ParticleSpec(ParticleTypes.FLAME, BAYONET_GUNSHOT_MUZZLE_COUNT, 0.02, 0.02, 0.02, 0.1),
            new ParticleSpec(ParticleTypes.LARGE_SMOKE, BAYONET_GUNSHOT_MUZZLE_COUNT, 0.02, 0.02, 0.02, 0.08),
            new ParticleSpec(ParticleTypes.CRIT, BAYONET_GUNSHOT_MUZZLE_COUNT, 0.02, 0.02, 0.02, 0.1),
            new ParticleSpec(ParticleTypes.FIREWORK, BAYONET_GUNSHOT_MUZZLE_COUNT, 0.02, 0.02, 0.02, 0.12),
            new ParticleSpec(ParticleTypes.CLOUD, BAYONET_GUNSHOT_MUZZLE_COUNT, 0.02, 0.02, 0.02, 0.1),
            new ParticleSpec(ParticleTypes.SMOKE, BAYONET_GUNSHOT_MUZZLE_COUNT, 0.02, 0.02, 0.02, 0.1)
    };

    private static final class ParticleSpec {
        private final ParticleOptions particle;
        private final int count;
        private final double xOffset;
        private final double yOffset;
        private final double zOffset;
        private final double speed;

        private ParticleSpec(
                ParticleOptions particle,
                int count,
                double xOffset,
                double yOffset,
                double zOffset,
                double speed
        ) {
            this.particle = particle;
            this.count = count;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
            this.speed = speed;
        }
    }

    public enum ClientAttackFlag {
        BAYONET_GUNSHOT((byte) 0),
        MKOPI_SLAM((byte) 1),
        DUSK_THIRD((byte) 2);

        private final byte id;

        ClientAttackFlag(byte id) {
            this.id = id;
        }

        public byte id() {
            return id;
        }

        public static ClientAttackFlag fromId(byte id) {
            for (ClientAttackFlag flag : values()) {
                if (flag.id == id) {
                    return flag;
                }
            }
            return BAYONET_GUNSHOT;
        }
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (target.level().isClientSide()) {
            return;
        }
        LivingEntity attacker = event.getEntity();
        boolean hasVaporwaveSword = attacker.getMainHandItem().is(ModItems.VAPORWAVE_SWORD.get())
                || attacker.getOffhandItem().is(ModItems.VAPORWAVE_SWORD.get());
        boolean hasSacrilegious = attacker.getMainHandItem().is(ModItems.SACRILEGIOUS.get())
                || attacker.getOffhandItem().is(ModItems.SACRILEGIOUS.get());
        boolean isMkopiSlamAttack = isMkopiSlamAttack(attacker);
        boolean isBayonetGunshotAttack = isBayonetGunshotAttack(attacker);
        boolean isDuskThirdAttack = isDuskThirdAttack(attacker);
        boolean isAirmaceAttack = isAirmaceAttack(attacker);
        if (isAirmaceAttack || isHoldingAirmace(attacker)) {
            recordAirmaceSmash(attacker);
        }
        if (!hasVaporwaveSword && !hasSacrilegious && !isMkopiSlamAttack && !isBayonetGunshotAttack && !isDuskThirdAttack) {
            return;
        }
        if (hasVaporwaveSword) {
            debugLog("[fweapons] AttackEntityEvent applying vaporified: attacker={} target={}", attacker.getName().getString(), target.getName().getString());
            target.addEffect(new MobEffectInstance(ModEffects.VAPORIFIED, VAPORIFIED_DURATION_TICKS, 0), attacker);
        }
        if (hasSacrilegious) {
            debugLog("[fweapons] AttackEntityEvent applying sacrilegious effects: attacker={} target={}", attacker.getName().getString(), target.getName().getString());
            applySacrilegiousHitEffects(target, attacker);
        }
        if (isMkopiSlamAttack) {
            debugLog("[fweapons] AttackEntityEvent applying mkopi slam effects: attacker={} target={}", attacker.getName().getString(), target.getName().getString());
            applyMkopiSlamEffects(target, attacker);
        }
        if (isBayonetGunshotAttack) {
            debugLog("[fweapons] AttackEntityEvent applying bayonet gunshot particles: attacker={} target={}", attacker.getName().getString(), target.getName().getString());
            applyBayonetGunshotParticles(target);
        }
        if (isDuskThirdAttack) {
            debugLog("[fweapons] AttackEntityEvent applying dusk finisher: attacker={} target={}", attacker.getName().getString(), target.getName().getString());
            applyDuskSunsetFinisher(target, attacker);
        }
    }

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }
        if (arrow.level().isClientSide()) {
            return;
        }
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty() || !weapon.is(ModItems.THE_FOOL.get())) {
            return;
        }
        applyTheFoolSpectralBonus(arrow);
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHitResult)) {
            return;
        }
        Entity targetEntity = entityHitResult.getEntity();
        Entity owner = arrow.getOwner();
        if (!(owner instanceof LivingEntity attacker) || !(targetEntity instanceof LivingEntity target)) {
            return;
        }
        if (!attacker.isAlive() || !target.isAlive() || attacker == target) {
            return;
        }
        swapPositions(attacker, target);
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0F) {
            return;
        }

        applyVaporifiedArmorBypass(event);
        applySunsetArmorBypass(event);
        applyHonorStrike(event);
        applyAirmaceFallBonus(event);
        applyFromSource(event.getEntity(), event.getSource());
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        if (!event.getSource().is(VAPORIFIED_DAMAGE)) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        DamageContainer container = event.getContainer();
        float adjusted = event.getNewDamage();
        DamageSource source = event.getSource();

        boolean bypassArmor = source.is(DamageTypeTags.BYPASSES_ARMOR);
        boolean bypassEnchants = source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS);

        if (bypassArmor && container.getReduction(DamageContainer.Reduction.ARMOR) <= 0.0F) {
            int armorValue = event.getEntity().getArmorValue();
            if (armorValue > 0) {
                float armorAfter = CombatRules.getDamageAfterAbsorb(
                        event.getEntity(),
                        adjusted,
                        source,
                        (float) armorValue,
                        (float) event.getEntity().getAttributeValue(Attributes.ARMOR_TOUGHNESS)
                );
                float armorReduction = adjusted - armorAfter;
                if (armorReduction > 0.0F) {
                    adjusted -= armorReduction * 0.5F;
                }
            }
        }

        if (bypassEnchants && container.getReduction(DamageContainer.Reduction.ENCHANTMENTS) <= 0.0F) {
            if (event.getEntity().level() instanceof ServerLevel serverLevel) {
                float enchantProtection = EnchantmentHelper.getDamageProtection(serverLevel, event.getEntity(), source);
                if (enchantProtection > 0.0F) {
                    float enchantAfter = CombatRules.getDamageAfterMagicAbsorb(adjusted, enchantProtection);
                    float enchantReduction = adjusted - enchantAfter;
                    if (enchantReduction > 0.0F) {
                        adjusted -= enchantReduction * 0.5F;
                    }
                }
            }
        }

        if (adjusted != event.getNewDamage()) {
            event.setNewDamage(adjusted);
        }
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
        applyTheFoolSpectralEffect(event.getEntity(), event.getSource());
        applyTheFoolPotionEffects(event.getEntity(), event.getSource());
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        DamageSource source = event.getSource();
        ItemStack weapon = source.getWeaponItem();
        boolean isNatureAxe = weapon != null && !weapon.isEmpty() && weapon.is(ModItems.NATUREAXE.get());
        Player attackerPlayer = null;
        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof Player player) {
            attackerPlayer = player;
        }
        if (!isNatureAxe && sourceEntity instanceof LivingEntity attacker) {
            isNatureAxe = attacker.getMainHandItem().is(ModItems.NATUREAXE.get())
                    || attacker.getOffhandItem().is(ModItems.NATUREAXE.get());
        }
        if (!isNatureAxe) {
            return;
        }

        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos feetPos = event.getEntity().blockPosition();
        ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);
        if (BoneMealItem.applyBonemeal(boneMeal, serverLevel, feetPos, attackerPlayer)) {
            if (attackerPlayer != null) {
                attackerPlayer.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            }
            serverLevel.levelEvent(1505, feetPos, 15);
            return;
        }
        BlockPos below = feetPos.below();
        if (BoneMealItem.applyBonemeal(boneMeal, serverLevel, below, attackerPlayer)) {
            if (attackerPlayer != null) {
                attackerPlayer.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            }
            serverLevel.levelEvent(1505, below, 15);
        }
    }

    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof SpectralArrow arrow)) {
            return;
        }
        if (!(arrow.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty() || !weapon.is(ModItems.THE_FOOL.get())) {
            return;
        }
        PotionContents contents = arrow.getPickupItemStackOrigin()
                .getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.equals(PotionContents.EMPTY)) {
            return;
        }
        int color = contents.getColor();
        if (color == -1) {
            return;
        }
        int count;
        if (isArrowInGround(arrow)) {
            int inGroundTime = getArrowInGroundTime(arrow);
            if (inGroundTime % 5 != 0) {
                return;
            }
            count = 1;
        } else {
            count = 2;
        }
        for (int i = 0; i < count; i++) {
            serverLevel.sendParticles(
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color),
                    arrow.getRandomX(0.5),
                    arrow.getRandomY(),
                    arrow.getRandomZ(0.5),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        updateLScytheDashPenalty(player);
        if (player.isSpectator()) {
            return;
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            LScythe.tickDashTrail(serverLevel, player);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            pruneExpiredAttackFlags(serverPlayer);
        }

        var data = EntityDataUtil.getPersistentData(player);
        boolean hasShroud = player.hasEffect(ModEffects.CERULEAN_SHROUD);
        boolean markedInvisible = EntityDataUtil.getBoolean(data, CeruleanShroudEffect.INVISIBLE_TAG);

        if (hasShroud) {
            if (!player.isInvisible()) {
                player.setInvisible(true);
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                CeruleanShroudEffect.spawnFootsteps(serverLevel, player);
                if (!markedInvisible) {
                    data.putBoolean(CeruleanShroudEffect.INVISIBLE_TAG, true);
                    clearNearbyMobTargets(serverLevel, player);
                    if (player instanceof ServerPlayer serverPlayer) {
                        syncCeruleanEquipment(serverLevel, serverPlayer, true);
                    }
                }
            } else if (!markedInvisible) {
                data.putBoolean(CeruleanShroudEffect.INVISIBLE_TAG, true);
            }
            return;
        }

        if (markedInvisible) {
            if (!player.hasEffect(MobEffects.INVISIBILITY)) {
                player.setInvisible(false);
            }
            if (player.level() instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                syncCeruleanEquipment(serverLevel, serverPlayer, false);
            }
            data.remove(CeruleanShroudEffect.INVISIBLE_TAG);
            data.remove(CeruleanShroudEffect.LAST_X_TAG);
            data.remove(CeruleanShroudEffect.LAST_Y_TAG);
            data.remove(CeruleanShroudEffect.LAST_Z_TAG);
            data.remove(CeruleanShroudEffect.STEP_PROGRESS_TAG);
        }
    }

    private static void updateLScytheDashPenalty(Player player) {
        var data = EntityDataUtil.getPersistentData(player);
        if (!data.contains(LScythe.DASH_ARMOR_EXPIRES_TAG)) {
            LScythe.clearDashArmorPenalty(player);
            return;
        }
        long expiresAt = EntityDataUtil.getLong(data, LScythe.DASH_ARMOR_EXPIRES_TAG);
        if (expiresAt <= 0L) {
            data.remove(LScythe.DASH_ARMOR_EXPIRES_TAG);
            data.remove(LScythe.DASH_ARMOR_STACKS_TAG);
            LScythe.clearDashArmorPenalty(player);
            return;
        }
        long now = player.level().getGameTime();
        if (expiresAt < now) {
            data.remove(LScythe.DASH_ARMOR_EXPIRES_TAG);
            data.remove(LScythe.DASH_ARMOR_STACKS_TAG);
            LScythe.clearDashArmorPenalty(player);
            return;
        }
        int stacks = EntityDataUtil.getInt(data, LScythe.DASH_ARMOR_STACKS_TAG);
        if (stacks <= 0) {
            stacks = 1;
            data.putInt(LScythe.DASH_ARMOR_STACKS_TAG, stacks);
        }
        LScythe.ensureDashArmorPenalty(player, stacks);
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
            if (!enchantment.value().canEnchant(left) && !isBreachDensityEnchantment(enchantment)) {
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
        event.setXpCost((int) totalCost);
        event.setMaterialCost(1);
    }

    private static void swapPositions(LivingEntity first, LivingEntity second) {
        Vec3 firstPos = first.position();
        Vec3 secondPos = second.position();
        teleportEntity(first, secondPos);
        teleportEntity(second, firstPos);
    }

    private static void applyTheFoolSpectralBonus(AbstractArrow arrow) {
        if (EntityDataUtil.getBoolean(arrow.getPersistentData(), THE_FOOL_SPECTRAL_BONUS_TAG)) {
            return;
        }
        ItemStack pickup = arrow.getPickupItemStackOrigin();
        if (!pickup.isEmpty() && pickup.is(Items.SPECTRAL_ARROW)) {
            arrow.setBaseDamage(getArrowBaseDamage(arrow) + 1.0D);
        }
        arrow.getPersistentData().putBoolean(THE_FOOL_SPECTRAL_BONUS_TAG, true);
    }

    private static void applyTheFoolSpectralEffect(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide()) {
            return;
        }
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof AbstractArrow arrow)) {
            return;
        }
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty() || !weapon.is(ModItems.THE_FOOL.get())) {
            return;
        }
        Entity effectSource = arrow.getEffectSource();
        target.addEffect(
                new MobEffectInstance(MobEffects.GLOWING, THE_FOOL_SPECTRAL_DURATION_TICKS, 0),
                effectSource
        );
        LivingEntity sourceLiving = null;
        if (effectSource instanceof LivingEntity living) {
            sourceLiving = living;
        } else if (arrow.getOwner() instanceof LivingEntity ownerLiving) {
            sourceLiving = ownerLiving;
        }
        if (sourceLiving != null) {
            sourceLiving.addEffect(
                    new MobEffectInstance(MobEffects.GLOWING, THE_FOOL_SPECTRAL_DURATION_TICKS, 0),
                    effectSource
            );
        }
    }

    private static void applyTheFoolPotionEffects(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide()) {
            return;
        }
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof AbstractArrow arrow)) {
            return;
        }
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty() || !weapon.is(ModItems.THE_FOOL.get())) {
            return;
        }
        PotionContents contents = arrow.getPickupItemStackOrigin()
                .getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.equals(PotionContents.EMPTY)) {
            return;
        }
        Entity effectSource = arrow.getEffectSource();
        if (contents.potion().isPresent()) {
            for (MobEffectInstance effectInstance : contents.potion().get().value().getEffects()) {
                target.addEffect(
                        new MobEffectInstance(
                                effectInstance.getEffect(),
                                Math.max(effectInstance.mapDuration(duration -> duration / 8), 1),
                                effectInstance.getAmplifier(),
                                effectInstance.isAmbient(),
                                effectInstance.isVisible()
                        ),
                        effectSource
                );
            }
        }
        for (MobEffectInstance effectInstance : contents.customEffects()) {
            target.addEffect(effectInstance, effectSource);
        }
    }

    private static Field resolveArrowField(String name) {
        try {
            Field field = AbstractArrow.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method resolveArrowMethod(String name) {
        try {
            return AbstractArrow.class.getMethod(name);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean isArrowInGround(AbstractArrow arrow) {
        if (ARROW_IN_GROUND_FIELD == null) {
            return false;
        }
        try {
            return ARROW_IN_GROUND_FIELD.getBoolean(arrow);
        } catch (IllegalAccessException ignored) {
            return false;
        }
    }

    private static int getArrowInGroundTime(AbstractArrow arrow) {
        if (ARROW_IN_GROUND_TIME_FIELD == null) {
            return 0;
        }
        try {
            return ARROW_IN_GROUND_TIME_FIELD.getInt(arrow);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static double getArrowBaseDamage(AbstractArrow arrow) {
        if (ARROW_GET_BASE_DAMAGE_METHOD != null) {
            try {
                Object value = ARROW_GET_BASE_DAMAGE_METHOD.invoke(arrow);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (ARROW_BASE_DAMAGE_FIELD == null) {
            return 0.0D;
        }
        try {
            return ARROW_BASE_DAMAGE_FIELD.getDouble(arrow);
        } catch (IllegalAccessException ignored) {
            return 0.0D;
        }
    }

    private static void teleportEntity(LivingEntity entity, Vec3 position) {
        if (entity instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = serverPlayer.level();
            serverPlayer.teleportTo(
                    serverLevel,
                    position.x,
                    position.y,
                    position.z,
                    Set.of(),
                    entity.getYRot(),
                    entity.getXRot(),
                    false
            );
        } else {
            entity.teleportTo(position.x, position.y, position.z);
        }
    }

    private static void applyFromSource(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide()) {
            return;
        }

        ItemStack weaponFromSource = source.getWeaponItem();
        boolean isVaporwaveSword = weaponFromSource != null && !weaponFromSource.isEmpty() && weaponFromSource.is(ModItems.VAPORWAVE_SWORD.get());
        boolean isDawn = weaponFromSource != null && !weaponFromSource.isEmpty() && weaponFromSource.is(ModItems.DAWN.get());
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
        if (!isDawn && attacker != null) {
            isDawn = attacker.getMainHandItem().is(ModItems.DAWN.get())
                    || attacker.getOffhandItem().is(ModItems.DAWN.get());
        }
        if (!isSacrilegious && attacker != null) {
            isSacrilegious = attacker.getMainHandItem().is(ModItems.SACRILEGIOUS.get())
                    || attacker.getOffhandItem().is(ModItems.SACRILEGIOUS.get());
        }

        if (!isVaporwaveSword && !isDawn && !isSacrilegious) {
            if (attacker != null && attacker.getType().toString().contains("player")) {
                debugLog(
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
            debugLog("[fweapons] Damage hook applying vaporified: msgId={} attacker={} target={}",
                    source.getMsgId(),
                    attacker == null ? "<none>" : attacker.getName().getString(),
                    target.getName().getString());
            target.addEffect(new MobEffectInstance(ModEffects.VAPORIFIED, VAPORIFIED_DURATION_TICKS, 0), attacker);
        }
        if (isDawn) {
            debugLog("[fweapons] Damage hook applying sunset: msgId={} attacker={} target={}",
                    source.getMsgId(),
                    attacker == null ? "<none>" : attacker.getName().getString(),
                    target.getName().getString());
            applySunsetHitEffects(target, attacker);
        }
        if (isSacrilegious) {
            debugLog("[fweapons] Damage hook applying sacrilegious effects: msgId={} attacker={} target={}",
                    source.getMsgId(),
                    attacker == null ? "<none>" : attacker.getName().getString(),
                    target.getName().getString());
            applySacrilegiousHitEffects(target, attacker);
        }
    }

    public static void onBayonetComboAttack(ServerPlayer player) {
        if (player.level().isClientSide()) {
            return;
        }
        if (!isHoldingBayonet(player)) {
            return;
        }
        spawnPerfectDapEffect(player);
        playBetterCombatAttackAnimation(player);
    }

    private static void playBetterCombatAttackAnimation(ServerPlayer player) {
        try {
            Class<?> attackAnimClass = Class.forName("net.bettercombat.network.Packets$AttackAnimation");
            Class<?> animatedHandClass = Class.forName("net.bettercombat.logic.AnimatedHand");
            Class<?> swingParticlesClass = Class.forName("net.bettercombat.network.Packets$SwingParticles");
            Class<?> serverNetworkClass = Class.forName("net.bettercombat.network.ServerNetwork");

            Object hand = Enum.valueOf((Class<Enum>) animatedHandClass, "MAIN_HAND");
            Object particles = swingParticlesClass.getField("EMPTY").get(null);
            float length = 1.625f;
            float upswing = 1.2f;
            int upswingTicks = Math.round(upswing * 20.0f);
            Object payload = attackAnimClass
                    .getConstructor(
                            int.class,
                            animatedHandClass,
                            String.class,
                            float.class,
                            float.class,
                            float.class,
                            int.class,
                            swingParticlesClass
                    )
                    .newInstance(
                            player.getId(),
                            hand,
                            "fweapons:perfect_hit",
                            length,
                            upswing,
                            0.0f,
                            upswingTicks,
                            particles
                    );

            serverNetworkClass
                    .getMethod(
                            "handleAttackAnimation",
                            attackAnimClass,
                            net.minecraft.server.MinecraftServer.class,
                            net.minecraft.server.level.ServerPlayer.class
                    )
                    .invoke(null, payload, player.level().getServer(), player);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void spawnPerfectDapEffect(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 look = player.getLookAngle().normalize();
        double x = player.getX() + look.x * 0.7D;
        double y = player.getEyeY() - 0.2D + look.y * 0.1D;
        double z = player.getZ() + look.z * 0.7D;

        serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 5, 0.2, 0.2, 0.2, 0.0);
        serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 40, 0.4, 0.4, 0.4, 0.12);
        serverLevel.sendParticles(ParticleTypes.FIREWORK, x, y, z, 50, 0.5, 0.5, 0.5, 0.15);

        serverLevel.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5f, 1.0f);
        serverLevel.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
        serverLevel.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.2f, 1.0f);
    }

    private static void applyHonorStrike(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
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
        var data = EntityDataUtil.getPersistentData(attacker);
        data.putDouble(CeruleanShroudEffect.STEP_PROGRESS_TAG, 0.0D);
        data.remove(CeruleanShroudEffect.LAST_X_TAG);
        data.remove(CeruleanShroudEffect.LAST_Y_TAG);
        data.remove(CeruleanShroudEffect.LAST_Z_TAG);
    }

    private static void applyVaporifiedArmorBypass(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(VAPORIFIED_DAMAGE)) {
            return;
        }

        // Reduce armor and enchantment reductions by 50% (effective half protection).
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * 0.5F);
        event.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (container, reduction) -> reduction * 0.5F);
    }

    private static void applySunsetArmorBypass(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(SUNSET_DAMAGE)) {
            return;
        }

        // Ignore 60% of armor reduction.
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * 0.4F);
    }

    private static void applyAirmaceFallBonus(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
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

        double fallDistance = attacker.fallDistance;
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
            double fallDistance,
            ItemStack airmaceStack
    ) {
        if (!canAirmaceSmash(attacker, fallDistance)) {
            return 0.0F;
        }

        double baseBonus;
        if (fallDistance <= 3.0D) {
            baseBonus = 4.0D * fallDistance;
        } else if (fallDistance <= 8.0D) {
            baseBonus = 12.0D + 2.0D * (fallDistance - 3.0D);
        } else {
            baseBonus = 22.0D + fallDistance - 8.0D;
        }

        if (attacker.level() instanceof ServerLevel serverLevel) {
            ItemStack weaponStack = airmaceStack == null || airmaceStack.isEmpty()
                    ? attacker.getWeaponItem()
                    : airmaceStack;
            float enchantBonus = net.minecraft.world.item.enchantment.EnchantmentHelper
                    .modifyFallBasedDamage(serverLevel, weaponStack, target, source, 0.0F)
                    * (float) fallDistance;
            return (float) baseBonus + enchantBonus;
        }

        return (float) baseBonus;
    }

    private static void clearNearbyMobTargets(ServerLevel serverLevel, Player player) {
        AABB area = player.getBoundingBox().inflate(32.0D);
        for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, area, mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
            mob.setLastHurtByPlayer((Player) null, 0);
            mob.setLastHurtByMob(null);
        }
    }

    private static void syncCeruleanEquipment(ServerLevel serverLevel, ServerPlayer shrouded, boolean hidden) {
        if (serverLevel.players().size() <= 1) {
            return;
        }

        List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>(CERULEAN_EQUIPMENT_SLOTS.length);
        for (EquipmentSlot slot : CERULEAN_EQUIPMENT_SLOTS) {
            ItemStack stack = hidden ? ItemStack.EMPTY : shrouded.getItemBySlot(slot).copy();
            equipment.add(Pair.of(slot, stack));
        }

        ClientboundSetEquipmentPacket packet = new ClientboundSetEquipmentPacket(shrouded.getId(), equipment);
        for (ServerPlayer viewer : serverLevel.players()) {
            if (viewer == shrouded) {
                continue;
            }
            viewer.connection.send(packet);
        }
    }

    private static void applySacrilegiousHitEffects(LivingEntity target, LivingEntity attacker) {
        var slowness = target.level().registryAccess().lookupOrThrow(Registries.MOB_EFFECT).getOrThrow(SLOWNESS_EFFECT);
        target.addEffect(new MobEffectInstance(slowness, SACRILEGIOUS_SLOWNESS_DURATION_TICKS, 0), attacker);

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

    private static void applySunsetHitEffects(LivingEntity target, LivingEntity attacker) {
        MobEffectInstance existing = target.getEffect(ModEffects.SUNSET);
        int amplifier = existing == null ? 0 : existing.getAmplifier() + 1;
        target.addEffect(new MobEffectInstance(ModEffects.SUNSET, SUNSET_DURATION_TICKS, amplifier), attacker);
    }

    private static void applyDuskSunsetFinisher(LivingEntity target, LivingEntity attacker) {
        MobEffectInstance existing = target.getEffect(ModEffects.SUNSET);
        if (existing == null) {
            return;
        }

        int stacks = existing.getAmplifier() + 1;
        target.removeEffect(ModEffects.SUNSET);

        float bonusDamage = 2.0F * stacks;
        DamageSource source;
        if (attacker instanceof Player player) {
            source = player.damageSources().playerAttack(player);
        } else if (attacker != null) {
            source = attacker.damageSources().mobAttack(attacker);
        } else {
            source = target.damageSources().generic();
        }
        target.hurt(source, bonusDamage);

        if (target.level() instanceof ServerLevel serverLevel) {
            spawnDuskSunsetBurst(serverLevel, target, stacks);
            serverLevel.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    ModSounds.DUSK.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }

    private static void spawnDuskSunsetBurst(ServerLevel serverLevel, LivingEntity target, int stacks) {
        int count = Math.min(480, 22 * stacks);
        double x = target.getX();
        double y = target.getY(0.6);
        double z = target.getZ();
        double spread = Math.min(3.0D, 0.55D + 0.12D * stacks);
        serverLevel.sendParticles(
                DUSK_SUNSET_DUST,
                x,
                y,
                z,
                count,
                spread,
                spread * 0.8D,
                spread,
                0.12D
        );
        int lightCount = Math.max(10, (int) (count * 0.7F));
        serverLevel.sendParticles(
                DUSK_SUNSET_LIGHT_DUST,
                x,
                y,
                z,
                lightCount,
                spread * 1.1D,
                spread * 0.9D,
                spread * 1.1D,
                0.16D
        );
    }

    public static void recordClientAttackFlag(ServerPlayer player, ClientAttackFlag flag) {
        if (player.level().isClientSide()) {
            return;
        }
        if (!isClientAttackFlagValid(player, flag)) {
            return;
        }

        long expiresAt = player.level().getGameTime() + CLIENT_ATTACK_FLAG_WINDOW_TICKS;
        CLIENT_ATTACK_FLAGS
                .computeIfAbsent(player.getUUID(), id -> new EnumMap<>(ClientAttackFlag.class))
                .put(flag, expiresAt);
    }

    private static boolean consumeClientAttackFlag(LivingEntity attacker, ClientAttackFlag flag) {
        if (!(attacker instanceof ServerPlayer player)) {
            return false;
        }

        EnumMap<ClientAttackFlag, Long> flags = CLIENT_ATTACK_FLAGS.get(player.getUUID());
        if (flags == null) {
            return false;
        }

        Long expiresAt = flags.get(flag);
        if (expiresAt == null) {
            return false;
        }

        long now = player.level().getGameTime();
        flags.remove(flag);
        if (flags.isEmpty()) {
            CLIENT_ATTACK_FLAGS.remove(player.getUUID());
        }
        if (expiresAt < now) {
            return false;
        }

        return isClientAttackFlagValid(player, flag);
    }

    private static void pruneExpiredAttackFlags(ServerPlayer player) {
        EnumMap<ClientAttackFlag, Long> flags = CLIENT_ATTACK_FLAGS.get(player.getUUID());
        if (flags == null) {
            return;
        }
        long now = player.level().getGameTime();
        for (var iterator = flags.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            if (entry.getValue() < now) {
                iterator.remove();
            }
        }
        if (flags.isEmpty()) {
            CLIENT_ATTACK_FLAGS.remove(player.getUUID());
        }
    }

    private static boolean isClientAttackFlagValid(LivingEntity attacker, ClientAttackFlag flag) {
        return switch (flag) {
            case BAYONET_GUNSHOT -> isHoldingBayonet(attacker);
            case MKOPI_SLAM -> isHoldingMkopi(attacker);
            case DUSK_THIRD -> isHoldingDusk(attacker);
        };
    }

    private static boolean isMkopiSlamAttack(LivingEntity attacker) {
        boolean result = false;
        try {
            Object currentAttack = attacker.getClass().getMethod("getCurrentAttack").invoke(attacker);
            if (currentAttack != null) {
                Object attackItem = currentAttack.getClass().getMethod("itemStack").invoke(currentAttack);
                if (attackItem instanceof ItemStack attackStack && attackStack.is(ModItems.MKOPI.get())) {
                    Object attack = currentAttack.getClass().getMethod("attack").invoke(currentAttack);
                    if (attack != null) {
                        Object hitbox = attack.getClass().getMethod("hitbox").invoke(attack);
                        Object animation = attack.getClass().getMethod("animation").invoke(attack);
                        result = hitbox != null
                                && "VERTICAL_PLANE".equals(hitbox.toString())
                                && MKOPI_SLAM_ANIMATION.equals(animation);
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            result = false;
        }

        if (result) {
            return true;
        }

        return consumeClientAttackFlag(attacker, ClientAttackFlag.MKOPI_SLAM);
    }

    private static boolean isBayonetGunshotAttack(LivingEntity attacker) {
        boolean result = false;
        try {
            Object currentAttack = attacker.getClass().getMethod("getCurrentAttack").invoke(attacker);
            if (currentAttack != null) {
                Object attackItem = currentAttack.getClass().getMethod("itemStack").invoke(currentAttack);
                if (attackItem instanceof ItemStack attackStack && attackStack.is(ModItems.BAYONET.get())) {
                    Object attack = currentAttack.getClass().getMethod("attack").invoke(currentAttack);
                    if (attack != null) {
                        Object hitbox = attack.getClass().getMethod("hitbox").invoke(attack);
                        Object animation = attack.getClass().getMethod("animation").invoke(attack);
                        result = hitbox != null
                                && BAYONET_GUNSHOT_HITBOX.equals(hitbox.toString())
                                && isBayonetGunshotAnimation(animation);
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            result = false;
        }

        if (result) {
            return true;
        }

        return consumeClientAttackFlag(attacker, ClientAttackFlag.BAYONET_GUNSHOT);
    }

    private static boolean isBayonetGunshotAnimation(Object animation) {
        if (animation == null) {
            return false;
        }
        String value = animation.toString();
        return BAYONET_GUNSHOT_ANIMATION.equals(value) || BAYONET_IMPACT_ANIMATION.equals(value);
    }

    private static boolean isDuskThirdAttack(LivingEntity attacker) {
        boolean result = false;
        try {
            Object currentAttack = attacker.getClass().getMethod("getCurrentAttack").invoke(attacker);
            if (currentAttack != null) {
                Object attackItem = currentAttack.getClass().getMethod("itemStack").invoke(currentAttack);
                if (attackItem instanceof ItemStack attackStack && attackStack.is(ModItems.DUSK.get())) {
                    Object attack = currentAttack.getClass().getMethod("attack").invoke(currentAttack);
                    if (attack != null) {
                        Object hitbox = attack.getClass().getMethod("hitbox").invoke(attack);
                        Object animation = attack.getClass().getMethod("animation").invoke(attack);
                        result = hitbox != null
                                && DUSK_THIRD_HITBOX.equals(hitbox.toString())
                                && isDuskThirdAnimation(animation);
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            result = false;
        }

        if (result) {
            return true;
        }

        return consumeClientAttackFlag(attacker, ClientAttackFlag.DUSK_THIRD);
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

    private static boolean isHoldingMkopi(LivingEntity attacker) {
        return attacker.getMainHandItem().is(ModItems.MKOPI.get())
                || attacker.getOffhandItem().is(ModItems.MKOPI.get());
    }

    private static boolean isHoldingDusk(LivingEntity attacker) {
        return attacker.getMainHandItem().is(ModItems.DUSK.get())
                || attacker.getOffhandItem().is(ModItems.DUSK.get());
    }

    private static boolean isDuskThirdAnimation(Object animation) {
        if (animation == null) {
            return false;
        }
        String value = animation.toString();
        return DUSK_THIRD_ANIMATION_PRIMARY.equals(value) || DUSK_THIRD_ANIMATION_FALLBACK.equals(value);
    }

    private static boolean isBreachDensityPair(net.minecraft.core.Holder<Enchantment> first, net.minecraft.core.Holder<Enchantment> second) {
        return (first.is(Enchantments.BREACH) && second.is(Enchantments.DENSITY))
                || (first.is(Enchantments.DENSITY) && second.is(Enchantments.BREACH));
    }

    private static boolean isBreachDensityEnchantment(net.minecraft.core.Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.BREACH) || enchantment.is(Enchantments.DENSITY);
    }

    private static void debugLog(String message, Object... args) {
        if (DEBUG_COMBAT_LOGS) {
            LOGGER.info(message, args);
        }
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

    public static void spawnBayonetGunshotMuzzleParticles(ServerPlayer attacker) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        spawnBayonetGunshotMuzzleParticles(serverLevel, attacker);
    }

    private static void spawnBayonetGunshotMuzzleParticles(ServerLevel serverLevel, LivingEntity attacker) {
        Vec3 look = attacker.getLookAngle().normalize();
        double x = attacker.getX() + look.x * 1.2D;
        double y = attacker.getEyeY() - 0.2D + look.y * 0.2D;
        double z = attacker.getZ() + look.z * 1.2D;

        sendParticlesToOthers(serverLevel, attacker, x, y, z, BAYONET_MUZZLE_SPECS);
    }

    private static void sendParticlesToOthers(
            ServerLevel serverLevel,
            LivingEntity attacker,
            double x,
            double y,
            double z,
            ParticleSpec[] specs
    ) {
        ServerPlayer attackerPlayer = attacker instanceof ServerPlayer player ? player : null;
        for (ServerPlayer player : serverLevel.players()) {
            if (attackerPlayer != null && player == attackerPlayer) {
                continue;
            }
            for (ParticleSpec spec : specs) {
                serverLevel.sendParticles(
                        player,
                        spec.particle,
                        false,
                        false,
                        x,
                        y,
                        z,
                        spec.count,
                        spec.xOffset,
                        spec.yOffset,
                        spec.zOffset,
                        spec.speed
                );
            }
        }
    }

    private static void spawnAirmaceSmashParticles(ServerLevel serverLevel, LivingEntity target, double fallDistance) {
        int totalCount = Math.min(140, 14 + (int) Math.round(fallDistance * 9.0D));
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
        var data = EntityDataUtil.getPersistentData(attacker);
        data.putDouble(AIRMACE_FALL_DISTANCE_TAG, attacker.fallDistance);
        data.putInt(AIRMACE_FALL_TICK_TAG, attacker.tickCount);
    }

    private static boolean hasRecentAirmaceSmash(LivingEntity attacker) {
        int recordedTick = EntityDataUtil.getInt(EntityDataUtil.getPersistentData(attacker), AIRMACE_FALL_TICK_TAG);
        return recordedTick != 0 && attacker.tickCount - recordedTick <= AIRMACE_FALL_TICK_WINDOW;
    }

    private static double getStoredAirmaceFallDistance(LivingEntity attacker) {
        return EntityDataUtil.getDouble(EntityDataUtil.getPersistentData(attacker), AIRMACE_FALL_DISTANCE_TAG);
    }

    private static void clearAirmaceSmash(LivingEntity attacker) {
        var data = EntityDataUtil.getPersistentData(attacker);
        data.remove(AIRMACE_FALL_DISTANCE_TAG);
        data.remove(AIRMACE_FALL_TICK_TAG);
    }

    private static boolean canAirmaceSmash(LivingEntity attacker, double fallDistance) {
        return fallDistance > 1.5D && !attacker.isFallFlying();
    }

}
