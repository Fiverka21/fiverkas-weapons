package com.fiv.fiverkas_weapons.item;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.fabric.data.PersistentData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;

public class LScythe extends AnimatedGradientSwordItem {
    public static final String DASH_ARMOR_EXPIRES_TAG = "fweapons_lscythe_dash_armor_expires";
    public static final String DASH_ARMOR_STACKS_TAG = "fweapons_lscythe_dash_armor_stacks";
    public static final String DASH_TRAIL_EXPIRES_TAG = "fweapons_lscythe_dash_trail_expires";
    private static final String DASH_HIT_USED_TAG = "fweapons_lscythe_dash_hit_used";
    private static final String DASH_TRAIL_LAST_X_TAG = "fweapons_lscythe_dash_trail_last_x";
    private static final String DASH_TRAIL_LAST_Y_TAG = "fweapons_lscythe_dash_trail_last_y";
    private static final String DASH_TRAIL_LAST_Z_TAG = "fweapons_lscythe_dash_trail_last_z";
    public static final ResourceLocation DASH_ARMOR_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "lscythe_dash_armor");
    public static final double DASH_ARMOR_PENALTY = -2.0D;
    private static final int WHITE = 0xFFFFFF;
    private static final int DARK_BLUE = 0x0B1D4A;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;
    private static final double CHAIN_RADIUS = 2.0D;
    private static final double CHAIN_WEATHER_MULTIPLIER = 2.0D;
    private static final int MAX_CHAINS = 6;
    private static final float CHAIN_DAMAGE_MULTIPLIER = 0.5F;
    private static final Vector3f LIGHT_BLUE = Vec3.fromRGB24(0x66CCFF).toVector3f();
    private static final DustParticleOptions LIGHT_BLUE_DUST = new DustParticleOptions(LIGHT_BLUE, 1.6F);
    private static final Vector3f DARK_BLUE_PARTICLE = Vec3.fromRGB24(DARK_BLUE).toVector3f();
    private static final DustParticleOptions DARK_BLUE_DUST = new DustParticleOptions(DARK_BLUE_PARTICLE, 1.6F);
    private static final double DASH_SPEED = 4.0D;
    private static final double DASH_VERTICAL_DAMPING = 0.2D;
    private static final int DASH_ARMOR_PENALTY_DURATION_TICKS = 120;
    private static final int DASH_COOLDOWN_TICKS = 24;
    private static final int DASH_TRAIL_DURATION_TICKS = 8;
    private static final double DASH_HIT_DISTANCE = 4.0D;
    private static final double DASH_HIT_RADIUS = 1.6D;
    private static final double DASH_HIT_FORWARD_PADDING = 1.0D;

    public LScythe(Tier tier, Item.Properties properties) {
        super(tier, properties, WHITE, DARK_BLUE, COLOR_SHIFT_SPEED_MS);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            net.minecraft.world.level.Level level,
            Player player,
            @NotNull InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        Item item = stack.getItem();
        if (player.getCooldowns().isOnCooldown(item)) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(item, DASH_COOLDOWN_TICKS);
        if (!level.isClientSide) {
            dashForward(player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (target.level().isClientSide) {
            return result;
        }
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return result;
        }

        float baseDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (baseDamage <= 0.0F) {
            return result;
        }

        applyChainLightning(serverLevel, target, attacker, baseDamage * CHAIN_DAMAGE_MULTIPLIER);
        return result;
    }

    private static void applyChainLightning(
            ServerLevel level,
            LivingEntity initialTarget,
            LivingEntity attacker,
            float chainDamage
    ) {
        double chainRadius = getChainRadius(level);
        Set<UUID> hit = new HashSet<>();
        hit.add(initialTarget.getUUID());
        // Avoid chaining into mobs already hit by the initial attack.
        List<LivingEntity> initialHits = level.getEntitiesOfClass(
                LivingEntity.class,
                initialTarget.getBoundingBox().inflate(chainRadius),
                entity -> entity.isAlive()
                        && entity != attacker
                        && entity.getLastHurtByMob() == attacker
                        && entity.hurtTime > 0
        );
        for (LivingEntity entity : initialHits) {
            hit.add(entity.getUUID());
        }

        LivingEntity current = initialTarget;
        DamageSource source = createChainDamageSource(attacker, initialTarget);

        for (int chain = 0; chain < MAX_CHAINS; chain++) {
            LivingEntity next = findNextTarget(level, current, attacker, hit, chainRadius);
            if (next == null) {
                break;
            }
            hit.add(next.getUUID());
            next.hurt(source, chainDamage);
            spawnChainParticles(level, current, next);
            current = next;
        }
    }

    private static LivingEntity findNextTarget(
            ServerLevel level,
            LivingEntity current,
            LivingEntity attacker,
            Set<UUID> hit,
            double chainRadius
    ) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                current.getBoundingBox().inflate(chainRadius),
                entity -> entity.isAlive()
                        && entity != attacker
                        && !hit.contains(entity.getUUID())
        );
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(current)));
        return candidates.get(0);
    }

    private static double getChainRadius(ServerLevel level) {
        if (level.isRaining() || level.isThundering()) {
            return CHAIN_RADIUS * CHAIN_WEATHER_MULTIPLIER;
        }
        return CHAIN_RADIUS;
    }

    private static DamageSource createChainDamageSource(LivingEntity attacker, LivingEntity target) {
        if (attacker instanceof Player player) {
            return player.damageSources().playerAttack(player);
        }
        if (attacker != null) {
            return attacker.damageSources().mobAttack(attacker);
        }
        return target.damageSources().generic();
    }

    private static void spawnChainParticles(ServerLevel level, LivingEntity from, LivingEntity to) {
        Vec3 start = from.position().add(0.0D, from.getBbHeight() * 0.35D, 0.0D);
        Vec3 end = to.position().add(0.0D, to.getBbHeight() * 0.35D, 0.0D);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 1.0E-4) {
            return;
        }
        int steps = Math.max(10, (int) (length * 16.0D));
        Vec3 step = delta.scale(1.0D / steps);
        Vec3 pos = start;
        for (int i = 0; i <= steps; i++) {
            DustParticleOptions particle = (i & 1) == 0 ? LIGHT_BLUE_DUST : DARK_BLUE_DUST;
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (i % 3 == 0) {
                DustParticleOptions accent = (i & 1) == 0 ? DARK_BLUE_DUST : LIGHT_BLUE_DUST;
                level.sendParticles(accent, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            pos = pos.add(step);
        }
    }

    private static void dashForward(Player player) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() <= 1.0E-6) {
            return;
        }
        Vec3 direction = look.normalize();
        double horizontalLen = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        Vec3 horizontalDir = horizontalLen > 1.0E-6
                ? new Vec3(direction.x / horizontalLen, 0.0D, direction.z / horizontalLen)
                : Vec3.ZERO;
        Vec3 dashVelocity = new Vec3(
                horizontalDir.x * DASH_SPEED,
                direction.y * DASH_SPEED * DASH_VERTICAL_DAMPING,
                horizontalDir.z * DASH_SPEED
        );
        if (dashVelocity.lengthSqr() <= 1.0E-6) {
            return;
        }
        Vec3 dashDirection = dashVelocity.normalize();
        player.push(dashVelocity.x, dashVelocity.y, dashVelocity.z);
        player.hasImpulse = true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.hurtMarked = true;
        }
        if (!player.level().isClientSide) {
            long now = player.level().getGameTime();
            var data = PersistentData.get(player);
            long previousExpiresAt = data.getLong(DASH_ARMOR_EXPIRES_TAG);
            int stacks = data.getInt(DASH_ARMOR_STACKS_TAG);
            if (previousExpiresAt > now && stacks > 0) {
                stacks += 1;
            } else {
                stacks = 1;
            }
            long expiresAt = now + DASH_ARMOR_PENALTY_DURATION_TICKS;
            data.putLong(DASH_ARMOR_EXPIRES_TAG, expiresAt);
            data.putInt(DASH_ARMOR_STACKS_TAG, stacks);
            ensureDashArmorPenalty(player, stacks);
            if (player.level() instanceof ServerLevel serverLevel) {
                spawnDashParticles(serverLevel, player, dashDirection);
                startDashTrail(player);
                tryDashHit(serverLevel, player, dashDirection);
            }
        }
        player.resetFallDistance();
    }

    private static void spawnDashParticles(ServerLevel level, Player player, Vec3 direction) {
        Vec3 start = player.position().add(0.0D, player.getBbHeight() * 0.85D, 0.0D);
        Vec3 end = start.add(direction.scale(2.2D));
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 1.0E-4) {
            return;
        }
        int steps = Math.max(12, (int) (length * 20.0D));
        Vec3 step = delta.scale(1.0D / steps);
        Vec3 pos = start;
        for (int i = 0; i <= steps; i++) {
            DustParticleOptions particle = (i & 1) == 0 ? LIGHT_BLUE_DUST : DARK_BLUE_DUST;
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.004D, 0.004D, 0.004D, 0.0D);
            if ((i & 2) == 0) {
                DustParticleOptions accent = (i & 1) == 0 ? DARK_BLUE_DUST : LIGHT_BLUE_DUST;
                level.sendParticles(accent, pos.x, pos.y, pos.z, 1, 0.006D, 0.006D, 0.006D, 0.0D);
            }
            pos = pos.add(step);
        }
    }

    private static void startDashTrail(Player player) {
        var data = PersistentData.get(player);
        long now = player.level().getGameTime();
        data.putLong(DASH_TRAIL_EXPIRES_TAG, now + DASH_TRAIL_DURATION_TICKS);
        data.remove(DASH_HIT_USED_TAG);
        data.putDouble(DASH_TRAIL_LAST_X_TAG, player.getX());
        data.putDouble(DASH_TRAIL_LAST_Y_TAG, player.getY());
        data.putDouble(DASH_TRAIL_LAST_Z_TAG, player.getZ());
    }

    public static void tickDashTrail(ServerLevel level, Player player) {
        var data = PersistentData.get(player);
        if (!data.contains(DASH_TRAIL_EXPIRES_TAG)) {
            return;
        }
        long expiresAt = data.getLong(DASH_TRAIL_EXPIRES_TAG);
        if (expiresAt <= level.getGameTime()) {
            clearDashTrailData(player);
            return;
        }
        tryDashCollisionHit(level, player);
        if (!data.contains(DASH_TRAIL_LAST_X_TAG)) {
            data.putDouble(DASH_TRAIL_LAST_X_TAG, player.getX());
            data.putDouble(DASH_TRAIL_LAST_Y_TAG, player.getY());
            data.putDouble(DASH_TRAIL_LAST_Z_TAG, player.getZ());
            return;
        }

        Vec3 last = new Vec3(
                data.getDouble(DASH_TRAIL_LAST_X_TAG),
                data.getDouble(DASH_TRAIL_LAST_Y_TAG),
                data.getDouble(DASH_TRAIL_LAST_Z_TAG)
        );
        Vec3 current = player.position();
        data.putDouble(DASH_TRAIL_LAST_X_TAG, current.x);
        data.putDouble(DASH_TRAIL_LAST_Y_TAG, current.y);
        data.putDouble(DASH_TRAIL_LAST_Z_TAG, current.z);

        Vec3 start = last.add(0.0D, player.getBbHeight() * 0.85D, 0.0D);
        Vec3 end = current.add(0.0D, player.getBbHeight() * 0.85D, 0.0D);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 1.0E-4) {
            return;
        }
        int steps = Math.max(6, (int) (length * 18.0D));
        Vec3 step = delta.scale(1.0D / steps);
        Vec3 pos = start;
        for (int i = 0; i <= steps; i++) {
            DustParticleOptions particle = (i & 1) == 0 ? LIGHT_BLUE_DUST : DARK_BLUE_DUST;
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.004D, 0.004D, 0.004D, 0.0D);
            pos = pos.add(step);
        }
    }

    private static void clearDashTrailData(Player player) {
        var data = PersistentData.get(player);
        data.remove(DASH_TRAIL_EXPIRES_TAG);
        data.remove(DASH_HIT_USED_TAG);
        data.remove(DASH_TRAIL_LAST_X_TAG);
        data.remove(DASH_TRAIL_LAST_Y_TAG);
        data.remove(DASH_TRAIL_LAST_Z_TAG);
    }

    private static void tryDashHit(ServerLevel level, Player player, Vec3 direction) {
        Vec3 start = player.position();
        Vec3 end = start.add(direction.scale(DASH_HIT_DISTANCE));
        AABB searchBox = new AABB(start, end).inflate(DASH_HIT_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity.isAlive() && entity != player
        );
        if (candidates.isEmpty()) {
            return;
        }

        LivingEntity closest = null;
        double closestForward = Double.MAX_VALUE;
        for (LivingEntity entity : candidates) {
            Vec3 toEntity = entity.position().subtract(start);
            double forward = toEntity.dot(direction);
            if (forward < 0.0D || forward > DASH_HIT_DISTANCE + DASH_HIT_FORWARD_PADDING) {
                continue;
            }
            Vec3 perpendicular = toEntity.subtract(direction.scale(forward));
            if (perpendicular.lengthSqr() > DASH_HIT_RADIUS * DASH_HIT_RADIUS) {
                continue;
            }
            if (forward < closestForward) {
                closestForward = forward;
                closest = entity;
            }
        }
        if (closest == null) {
            return;
        }

        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (baseDamage <= 0.0F) {
            return;
        }
        DamageSource source = player.damageSources().playerAttack(player);
        closest.hurt(source, baseDamage);
        applyChainLightning(level, closest, player, baseDamage * CHAIN_DAMAGE_MULTIPLIER);
        PersistentData.get(player).putBoolean(DASH_HIT_USED_TAG, true);
    }

    private static void tryDashCollisionHit(ServerLevel level, Player player) {
        var data = PersistentData.get(player);
        if (data.getBoolean(DASH_HIT_USED_TAG)) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(0.2D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive() && entity != player
        );
        if (candidates.isEmpty()) {
            return;
        }
        LivingEntity target = candidates.get(0);
        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (baseDamage <= 0.0F) {
            return;
        }
        DamageSource source = player.damageSources().playerAttack(player);
        target.hurt(source, baseDamage);
        applyChainLightning(level, target, player, baseDamage * CHAIN_DAMAGE_MULTIPLIER);
        data.putBoolean(DASH_HIT_USED_TAG, true);
    }

    public static void ensureDashArmorPenalty(Player player, int stacks) {
        var armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        double baseArmor = armor.getBaseValue();
        double penalty = Math.max(DASH_ARMOR_PENALTY * Math.max(1, stacks), -baseArmor);
        var existing = armor.getModifier(DASH_ARMOR_MODIFIER_ID);
        if (existing != null) {
            if (existing.amount() == penalty) {
                return;
            }
            armor.removeModifier(DASH_ARMOR_MODIFIER_ID);
        }
        armor.addTransientModifier(new AttributeModifier(
                DASH_ARMOR_MODIFIER_ID,
                penalty,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    public static void clearDashArmorPenalty(Player player) {
        var armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        armor.removeModifier(DASH_ARMOR_MODIFIER_ID);
    }
}
