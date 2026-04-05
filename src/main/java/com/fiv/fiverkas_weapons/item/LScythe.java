package com.fiv.fiverkas_weapons.item;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.util.CompatIds;
import com.fiv.fiverkas_weapons.util.EntityDataUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LScythe extends AnimatedGradientSwordItem {
    public static final String DASH_ARMOR_EXPIRES_TAG = "fweapons_lscythe_dash_armor_expires";
    public static final String DASH_ARMOR_STACKS_TAG = "fweapons_lscythe_dash_armor_stacks";
    public static final String DASH_TRAIL_EXPIRES_TAG = "fweapons_lscythe_dash_trail_expires";
    private static final String DASH_HIT_USED_TAG = "fweapons_lscythe_dash_hit_used";
    private static final String DASH_TRAIL_LAST_X_TAG = "fweapons_lscythe_dash_trail_last_x";
    private static final String DASH_TRAIL_LAST_Y_TAG = "fweapons_lscythe_dash_trail_last_y";
    private static final String DASH_TRAIL_LAST_Z_TAG = "fweapons_lscythe_dash_trail_last_z";
    private static final Object DASH_ARMOR_MODIFIER_ID = CompatIds.id(FiverkasWeapons.MODID, "lscythe_dash_armor");
    public static final double DASH_ARMOR_PENALTY = -2.0D;
    private static final int WHITE = 0xFFFFFF;
    private static final int DARK_BLUE = 0x0B1D4A;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;
    private static final double CHAIN_RADIUS = 2.0D;
    private static final double CHAIN_WEATHER_MULTIPLIER = 2.0D;
    private static final int MAX_CHAINS = 6;
    private static final float CHAIN_DAMAGE_MULTIPLIER = 0.5F;
    private static final DustParticleOptions LIGHT_BLUE_DUST = new DustParticleOptions(0x66CCFF, 1.6F);
    private static final DustParticleOptions DARK_BLUE_DUST = new DustParticleOptions(DARK_BLUE, 1.6F);
    private static final double DASH_SPEED = 4.0D;
    private static final double DASH_VERTICAL_DAMPING = 0.2D;
    private static final int DASH_ARMOR_PENALTY_DURATION_TICKS = 120;
    private static final int DASH_COOLDOWN_TICKS = 24;
    private static final int DASH_TRAIL_DURATION_TICKS = 8;
    private static final double DASH_HIT_DISTANCE = 4.0D;
    private static final double DASH_HIT_RADIUS = 1.6D;
    private static final double DASH_HIT_FORWARD_PADDING = 1.0D;
    private static final Constructor<?> ATTRIBUTE_MODIFIER_CTOR = resolveAttributeModifierConstructor();
    private static final Method ATTRIBUTE_MODIFIER_AMOUNT = resolveModifierAmountMethod();

    public LScythe(Item.Properties properties) {
        super(properties, WHITE, DARK_BLUE, COLOR_SHIFT_SPEED_MS);
    }

    @Override
    public @NotNull InteractionResult use(
            net.minecraft.world.level.Level level,
            Player player,
            @NotNull InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        player.getCooldowns().addCooldown(stack, DASH_COOLDOWN_TICKS);
        if (!level.isClientSide()) {
            dashForward(player);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (target.level().isClientSide()) {
            return;
        }
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float baseDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (baseDamage <= 0.0F) {
            return;
        }

        applyChainLightning(serverLevel, target, attacker, baseDamage * CHAIN_DAMAGE_MULTIPLIER);
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
        player.hurtMarked = true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.hurtMarked = true;
        }
        if (!player.level().isClientSide()) {
            long now = player.level().getGameTime();
            var data = EntityDataUtil.getPersistentData(player);
            long previousExpiresAt = EntityDataUtil.getLong(data, DASH_ARMOR_EXPIRES_TAG);
            int stacks = EntityDataUtil.getInt(data, DASH_ARMOR_STACKS_TAG);
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
        var data = EntityDataUtil.getPersistentData(player);
        long now = player.level().getGameTime();
        data.putLong(DASH_TRAIL_EXPIRES_TAG, now + DASH_TRAIL_DURATION_TICKS);
        data.remove(DASH_HIT_USED_TAG);
        data.putDouble(DASH_TRAIL_LAST_X_TAG, player.getX());
        data.putDouble(DASH_TRAIL_LAST_Y_TAG, player.getY());
        data.putDouble(DASH_TRAIL_LAST_Z_TAG, player.getZ());
    }

    public static void tickDashTrail(ServerLevel level, Player player) {
        var data = EntityDataUtil.getPersistentData(player);
        if (!data.contains(DASH_TRAIL_EXPIRES_TAG)) {
            return;
        }
        long expiresAt = EntityDataUtil.getLong(data, DASH_TRAIL_EXPIRES_TAG);
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
                EntityDataUtil.getDouble(data, DASH_TRAIL_LAST_X_TAG),
                EntityDataUtil.getDouble(data, DASH_TRAIL_LAST_Y_TAG),
                EntityDataUtil.getDouble(data, DASH_TRAIL_LAST_Z_TAG)
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
        var data = EntityDataUtil.getPersistentData(player);
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
        EntityDataUtil.getPersistentData(player).putBoolean(DASH_HIT_USED_TAG, true);
    }

    private static void tryDashCollisionHit(ServerLevel level, Player player) {
        var data = EntityDataUtil.getPersistentData(player);
        if (EntityDataUtil.getBoolean(data, DASH_HIT_USED_TAG)) {
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

        Object existing = getModifierById(armor, DASH_ARMOR_MODIFIER_ID);
        if (existing != null) {
            double existingAmount = readModifierAmount(existing);
            if (!Double.isNaN(existingAmount) && Math.abs(existingAmount - penalty) < 1.0E-6D) {
                return;
            }
            removeModifierById(armor, DASH_ARMOR_MODIFIER_ID);
        }

        addTransientModifier(armor, DASH_ARMOR_MODIFIER_ID, penalty);
    }

    public static void clearDashArmorPenalty(Player player) {
        var armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        removeModifierById(armor, DASH_ARMOR_MODIFIER_ID);
    }

    private static Constructor<?> resolveAttributeModifierConstructor() {
        for (Constructor<?> constructor : AttributeModifier.class.getConstructors()) {
            if (constructor.getParameterCount() != 3) {
                continue;
            }
            Class<?>[] params = constructor.getParameterTypes();
            if (!isDoubleLike(params[1])) {
                continue;
            }
            if (!AttributeModifier.Operation.class.isAssignableFrom(params[2])) {
                continue;
            }
            return constructor;
        }
        return null;
    }

    private static Method resolveModifierAmountMethod() {
        for (String name : new String[]{"amount", "getAmount"}) {
            try {
                Method method = AttributeModifier.class.getMethod(name);
                if (isDoubleLike(method.getReturnType())) {
                    return method;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static boolean isDoubleLike(Class<?> type) {
        return type == double.class || type == Double.class;
    }

    private static Object createModifier(Object id, double amount) {
        if (ATTRIBUTE_MODIFIER_CTOR == null) {
            return null;
        }
        try {
            return ATTRIBUTE_MODIFIER_CTOR.newInstance(id, amount, AttributeModifier.Operation.ADD_VALUE);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object getModifierById(Object attribute, Object id) {
        Method method = findCompatibleSingleArgMethod(attribute.getClass(), "getModifier", id.getClass());
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(attribute, id);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void removeModifierById(Object attribute, Object id) {
        Method method = findCompatibleSingleArgMethod(attribute.getClass(), "removeModifier", id.getClass());
        if (method == null) {
            return;
        }
        try {
            method.invoke(attribute, id);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void addTransientModifier(Object attribute, Object id, double amount) {
        Object modifier = createModifier(id, amount);
        if (modifier == null) {
            return;
        }
        Method method = findCompatibleSingleArgMethod(attribute.getClass(), "addTransientModifier", modifier.getClass());
        if (method == null) {
            return;
        }
        try {
            method.invoke(attribute, modifier);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static double readModifierAmount(Object modifier) {
        if (ATTRIBUTE_MODIFIER_AMOUNT == null) {
            return Double.NaN;
        }
        try {
            Object value = ATTRIBUTE_MODIFIER_AMOUNT.invoke(modifier);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return Double.NaN;
    }

    private static Method findCompatibleSingleArgMethod(Class<?> owner, String name, Class<?> argType) {
        for (Method method : owner.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameter = method.getParameterTypes()[0];
            if (parameter.isAssignableFrom(argType) || argType.isAssignableFrom(parameter)) {
                return method;
            }
        }
        return null;
    }
}
