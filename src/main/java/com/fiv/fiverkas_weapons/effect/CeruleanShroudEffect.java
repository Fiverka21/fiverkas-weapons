package com.fiv.fiverkas_weapons.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class CeruleanShroudEffect extends MobEffect {
    private static final Vector3f BLUE = Vec3.fromRGB24(0x0000FF).toVector3f();
    private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(BLUE, 1.3F);
    public static final String STEP_PROGRESS_TAG = "fweapons_cerulean_step_progress";
    public static final String INVISIBLE_TAG = "fweapons_cerulean_invisible";
    public static final String LAST_X_TAG = "fweapons_cerulean_last_x";
    public static final String LAST_Y_TAG = "fweapons_cerulean_last_y";
    public static final String LAST_Z_TAG = "fweapons_cerulean_last_z";
    private static final double STEP_DISTANCE = 0.18D;
    private static final int STEP_PARTICLE_COUNT = 1;

    public CeruleanShroudEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x0000FF);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath("fweapons", "cerulean_shroud_speed"),
                0.4D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Footstep particles are emitted from server player tick for consistent timing.
        // Returning false removes the effect instance in 1.21.1; keep it active.
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public DustParticleOptions createParticleOptions(MobEffectInstance effect) {
        return BLUE_DUST;
    }

    public static void spawnFootsteps(ServerLevel serverLevel, LivingEntity entity) {
        if (entity.isInWater() || entity.isInLava()) {
            return;
        }

        var data = entity.getPersistentData();
        if (!data.contains(LAST_X_TAG)) {
            data.putDouble(LAST_X_TAG, entity.getX());
            data.putDouble(LAST_Y_TAG, entity.getY());
            data.putDouble(LAST_Z_TAG, entity.getZ());
            return;
        }

        double lastX = data.getDouble(LAST_X_TAG);
        double lastY = data.getDouble(LAST_Y_TAG);
        double lastZ = data.getDouble(LAST_Z_TAG);
        double dx = entity.getX() - lastX;
        double dy = entity.getY() - lastY;
        double dz = entity.getZ() - lastZ;
        data.putDouble(LAST_X_TAG, entity.getX());
        data.putDouble(LAST_Y_TAG, entity.getY());
        data.putDouble(LAST_Z_TAG, entity.getZ());

        if (entity instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().flying) {
            return;
        }

        boolean nearGround = entity.onGround() || Math.abs(dy) < 0.03D;
        if (!nearGround) {
            return;
        }

        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist <= 0.0001D) {
            return;
        }

        double progress = data.getDouble(STEP_PROGRESS_TAG);
        progress += dist;
        while (progress >= STEP_DISTANCE) {
            progress -= STEP_DISTANCE;
            double x = entity.getX();
            double y = entity.getY() + 0.05D;
            double z = entity.getZ();
            double xzSpread = Math.max(0.1D, entity.getBbWidth() * 0.32D);
            for (ServerPlayer viewer : serverLevel.players()) {
                serverLevel.sendParticles(
                        viewer,
                        BLUE_DUST,
                        true,
                        x,
                        y,
                        z,
                        STEP_PARTICLE_COUNT,
                        xzSpread,
                        0.02D,
                        xzSpread,
                        0.0D
                );
            }
        }
        data.putDouble(STEP_PROGRESS_TAG, progress);
    }
}
