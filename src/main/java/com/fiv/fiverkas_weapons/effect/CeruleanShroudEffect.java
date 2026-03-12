package com.fiv.fiverkas_weapons.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.fiv.fiverkas_weapons.util.EntityDataUtil;
import com.fiv.fiverkas_weapons.util.CompatIds;

public class CeruleanShroudEffect extends MobEffect {
    private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(0x0000FF, 1.3F);
    public static final String STEP_PROGRESS_TAG = "fweapons_cerulean_step_progress";
    public static final String INVISIBLE_TAG = "fweapons_cerulean_invisible";
    public static final String LAST_X_TAG = "fweapons_cerulean_last_x";
    public static final String LAST_Y_TAG = "fweapons_cerulean_last_y";
    public static final String LAST_Z_TAG = "fweapons_cerulean_last_z";
    private static final double STEP_DISTANCE = 0.18D;
    private static final int STEP_PARTICLE_COUNT = 1;

    public CeruleanShroudEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x0000FF);
        CompatIds.addAttributeModifier(
                this,
                Attributes.MOVEMENT_SPEED,
                "fweapons",
                "cerulean_shroud_speed",
                0.4D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
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

        var data = EntityDataUtil.getPersistentData(entity);
        if (!data.contains(LAST_X_TAG)) {
            data.putDouble(LAST_X_TAG, entity.getX());
            data.putDouble(LAST_Y_TAG, entity.getY());
            data.putDouble(LAST_Z_TAG, entity.getZ());
            return;
        }

        double lastX = EntityDataUtil.getDouble(data, LAST_X_TAG);
        double lastY = EntityDataUtil.getDouble(data, LAST_Y_TAG);
        double lastZ = EntityDataUtil.getDouble(data, LAST_Z_TAG);
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

        double progress = EntityDataUtil.getDouble(data, STEP_PROGRESS_TAG);
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
                        false,
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
