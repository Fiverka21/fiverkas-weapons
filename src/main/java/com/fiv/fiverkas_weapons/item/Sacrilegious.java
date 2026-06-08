package com.fiv.fiverkas_weapons.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import org.jetbrains.annotations.NotNull;

public class Sacrilegious extends AnimatedGradientSwordItem {
    private static final int BLUE = 0x332EBF;
    private static final int YELLOW = 0xBABF2E;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    // Artorias slam constants
    public static final String SLAM_AIRBORNE_TAG = "fweapons_artorias_airborne";
    public static final int SLAM_COOLDOWN_TICKS = 80;
    public static final int SLAM_TIMEOUT_TICKS = 80;
    public static final int SLAM_MIN_AIRBORNE_TICKS = 4;
    private static final double LAUNCH_HORIZONTAL_SPEED = 1.2D;
    private static final double LAUNCH_VERTICAL_SPEED = 0.75D;

    public Sacrilegious(Tier tier, Item.Properties properties) {
        super(tier, properties, BLUE, YELLOW, COLOR_SHIFT_SPEED_MS);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            Player player,
            @NotNull InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        Item item = stack.getItem();
        if (level.isClientSide) {
            // Send request to server to perform the slam and try to suppress client-side use animation
            try {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.fiv.fiverkas_weapons.network.SacrilegiousSlamRequestPayload());
            } catch (Throwable ignored) {
            }
            try {
                player.stopUsingItem();
            } catch (Throwable ignored) {
            }
            return InteractionResultHolder.fail(stack);
        }

        if (player.getCooldowns().isOnCooldown(item)) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(item, SLAM_COOLDOWN_TICKS);
        launchPlayer(player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void launchPlayer(Player player) {
        Vec3 look = player.getLookAngle();
        double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
        double x;
        double z;
        if (horizontalLength > 1.0E-6D) {
            x = look.x / horizontalLength * LAUNCH_HORIZONTAL_SPEED;
            z = look.z / horizontalLength * LAUNCH_HORIZONTAL_SPEED;
        } else {
            double yawRadians = Math.toRadians(player.getYRot());
            x = -Math.sin(yawRadians) * LAUNCH_HORIZONTAL_SPEED;
            z = Math.cos(yawRadians) * LAUNCH_HORIZONTAL_SPEED;
        }
        player.setDeltaMovement(x, LAUNCH_VERTICAL_SPEED, z);
        player.hasImpulse = true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.hurtMarked = true;
            System.out.println("[fweapons] Sacrilegious: Launching player, about to call sacrilegious slam animation");
            ModCombatEvents.playSacrilegiousSlamAnimation(serverPlayer);
            System.out.println("[fweapons] Sacrilegious: Finished calling sacrilegious slam animation");
            // Also swing the sword to provide fallback visual feedback
            serverPlayer.swing(InteractionHand.MAIN_HAND);
        }
        player.resetFallDistance();
        player.getPersistentData().putLong(SLAM_AIRBORNE_TAG, player.level().getGameTime());

        // Add visual feedback for the slam activation
        if (player.level() instanceof ServerLevel serverLevel) {
            // Add particle effects to make the slam more visible
            Vec3 pos = player.position();
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 8, 0.3D, 0.3D, 0.3D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            // Add sound effect for the slam activation
            serverLevel.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                1.0f,
                1.0f
            );
        }
    }
}
