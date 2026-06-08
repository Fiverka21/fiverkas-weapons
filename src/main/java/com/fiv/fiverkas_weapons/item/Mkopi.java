package com.fiv.fiverkas_weapons.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class Mkopi extends AnimatedGradientSwordItem {
    private static final int GREY = 0x424040;
    private static final int RED = 0xFF0000;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;
    private static final DustParticleOptions HIT_RED_DUST =
            new DustParticleOptions(Vec3.fromRGB24(RED).toVector3f(), 1.8F);
    private static final int STRIKE_CHARGE_TICKS = 40;
    private static final int SELF_HIT_CHARGE_TICKS = STRIKE_CHARGE_TICKS + 80;
    private static final int USE_DURATION_TICKS = 72000;
    private static final int STRIKE_COOLDOWN_TICKS = 20;
    private static final String STRUCK_DURING_USE_TAG = "fweapons_mkopi_struck_during_use";
    private static final String DESTINATION_PARTICLES_AT_TAG = "fweapons_mkopi_destination_particles_at";
    private static final double SHAKE_STRENGTH = 0.045D;
    private static final double STRIKE_LUNGE_STRENGTH = 1.35D;
    private static final double STRIKE_DISTANCE = 5.5D;
    private static final double STRIKE_RADIUS = 0.95D;

    public Mkopi(Tier tier, Item.Properties properties) {
        super(tier, properties, RED, GREY, COLOR_SHIFT_SPEED_MS);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            player.getPersistentData().remove(STRUCK_DURING_USE_TAG);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int usedTicks = getUseDuration(stack, entity) - remainingUseTicks;
        if (usedTicks < STRIKE_CHARGE_TICKS) {
            shakePlayer(serverLevel, player, usedTicks);
            return;
        }

        var data = player.getPersistentData();
        if (!data.getBoolean(STRUCK_DURING_USE_TAG)) {
            data.putBoolean(STRUCK_DURING_USE_TAG, true);
            if (strike(serverLevel, player)) {
                data.remove(STRUCK_DURING_USE_TAG);
                player.getCooldowns().addCooldown(this, STRIKE_COOLDOWN_TICKS);
                player.stopUsingItem();
            } else {
                shakePlayer(serverLevel, player, usedTicks);
            }
            return;
        }

        if (usedTicks < SELF_HIT_CHARGE_TICKS) {
            shakePlayer(serverLevel, player, usedTicks);
            return;
        }

        data.remove(STRUCK_DURING_USE_TAG);
        if (player instanceof ServerPlayer serverPlayer) {
            selfStrike(serverLevel, serverPlayer);
            player.getCooldowns().addCooldown(this, STRIKE_COOLDOWN_TICKS);
        }
        player.stopUsingItem();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    private static void shakePlayer(ServerLevel level, Player player, int usedTicks) {
        double x = (level.random.nextDouble() - 0.5D) * SHAKE_STRENGTH;
        double z = (level.random.nextDouble() - 0.5D) * SHAKE_STRENGTH;
        player.push(x, 0.0D, z);
        player.hasImpulse = true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.hurtMarked = true;
        }

        if (usedTicks % 4 == 0) {
            level.sendParticles(
                    ParticleTypes.SQUID_INK,
                    player.getX(),
                    player.getY(0.65D),
                    player.getZ(),
                    3,
                    0.24D,
                    0.18D,
                    0.24D,
                    0.02D
            );
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.SCULK_CLICKING,
                    SoundSource.PLAYERS,
                    0.18F,
                    0.6F + level.random.nextFloat() * 0.35F
            );
        }
    }

    private static boolean strike(ServerLevel level, Player player) {
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() <= 1.0E-6D) {
            return false;
        }
        direction = direction.normalize();

        player.push(direction.x * STRIKE_LUNGE_STRENGTH, direction.y * 0.25D, direction.z * STRIKE_LUNGE_STRENGTH);
        player.hasImpulse = true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.hurtMarked = true;
        }
        player.swing(player.getUsedItemHand(), true);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.TRIDENT_THROW.value(),
                SoundSource.PLAYERS,
                0.85F,
                0.55F
        );

        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(direction.scale(STRIKE_DISTANCE));
        spawnStrikeParticles(level, start, end);

        ServerPlayer target = findHitPlayer(level, player, start, end);
        if (target != null) {
            spawnHitParticles(level, target);
            teleportToSpawn(target);
            return true;
        }
        return false;
    }

    private static ServerPlayer findHitPlayer(ServerLevel level, Player attacker, Vec3 start, Vec3 end) {
        AABB searchBox = new AABB(start, end).inflate(STRIKE_RADIUS);
        List<ServerPlayer> candidates = level.getEntitiesOfClass(
                ServerPlayer.class,
                searchBox,
                target -> target.isAlive()
                        && !target.isSpectator()
                        && target != attacker
                        && attacker.canHarmPlayer(target)
        );

        ServerPlayer closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ServerPlayer candidate : candidates) {
            Optional<Vec3> hit = candidate.getBoundingBox().inflate(STRIKE_RADIUS).clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    private static void selfStrike(ServerLevel level, ServerPlayer player) {
        player.swing(player.getUsedItemHand(), true);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.9F,
                0.55F
        );
        level.sendParticles(
                ParticleTypes.SQUID_INK,
                player.getX(),
                player.getY(0.6D),
                player.getZ(),
                36,
                0.45D,
                0.35D,
                0.45D,
                0.08D
        );
        spawnHitParticles(level, player);
        teleportToSpawn(player);
    }

    private static void teleportToSpawn(ServerPlayer target) {
        target.sendSystemMessage(Component.literal("...it feels as if waking up from a strange dream...").withStyle(style -> style.withColor(TextColor.fromRgb(RED))));

        ServerLevel oldLevel = target.serverLevel();
        oldLevel.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                0.72F
        );

        DimensionTransition transition = target.findRespawnPositionAndUseSpawnBlock(
                true,
                DimensionTransition.DO_NOTHING
        );
        Vec3 spawn = transition.pos();
        target.setDeltaMovement(Vec3.ZERO);
        target.teleportTo(transition.newLevel(), spawn.x, spawn.y, spawn.z, transition.yRot(), transition.xRot());
        scheduleDestinationParticles(target);
    }

    public static void tickDestinationParticles(ServerLevel level, Player player) {
        var data = player.getPersistentData();
        if (!data.contains(DESTINATION_PARTICLES_AT_TAG)) {
            return;
        }

        long particlesAt = data.getLong(DESTINATION_PARTICLES_AT_TAG);
        if (level.getGameTime() < particlesAt) {
            return;
        }

        data.remove(DESTINATION_PARTICLES_AT_TAG);
        spawnHitParticles(level, player);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                0.85F
        );
    }

    private static void scheduleDestinationParticles(ServerPlayer target) {
        target.getPersistentData().putLong(
                DESTINATION_PARTICLES_AT_TAG,
                target.serverLevel().getGameTime() + 2L
        );
    }

    private static void spawnHitParticles(ServerLevel level, Player target) {
        level.sendParticles(
                HIT_RED_DUST,
                target.getX(),
                target.getY(0.58D),
                target.getZ(),
                68,
                0.72D,
                0.48D,
                0.72D,
                0.0D
        );
    }

    private static void spawnStrikeParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = 12;
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = start.add(delta.scale((double) i / (double) steps));
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    pos.x,
                    pos.y,
                    pos.z,
                    2,
                    0.08D,
                    0.08D,
                    0.08D,
                    0.01D
            );
        }
    }
}
