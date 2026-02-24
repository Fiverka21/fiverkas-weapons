package com.fiv.fiverkas_weapons.effect;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Vaporified debuff for NeoForge 1.21.1
 */
public class VaporifiedEffect extends MobEffect {
    private static final int PINK = 0xFFFF69B4;
    private static final int CYAN = 0xFF00FFFF;
    // Tuned 70% weaker than the original slow-fall reduction.
    private static final double VAPORIFIED_MAX_FALL_SPEED = -0.0584D;
    private static final double VAPORIFIED_FALL_DAMPING = 0.73D;

    public VaporifiedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF69B4); // pink color
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Apply on both sides so local player motion feedback is immediate.
        applyVaporifiedSlowFall(entity);

        if (!entity.level().isClientSide) {
            if (entity.tickCount % 20 == 0) {
                if (entity.level() instanceof ServerLevel serverLevel) {
                    double x = entity.getX();
                    double y = entity.getY() + entity.getBbHeight() * 0.5D;
                    double z = entity.getZ();
                    double xzSpread = Math.max(0.1D, entity.getBbWidth() * 0.35D);
                    double ySpread = Math.max(0.1D, entity.getBbHeight() * 0.35D);

                    serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, PINK), x, y, z, 8, xzSpread, ySpread, xzSpread, 0.01D);
                    serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, CYAN), x, y, z, 8, xzSpread, ySpread, xzSpread, 0.01D);
                }
                float damage = 4.0f; // 4 damage per second
                entity.hurt(entity.damageSources().generic(), damage);
            }
        }
        // Returning false removes the effect instance in 1.21.1; keep it active.
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // run every tick for internal slow-fall behavior
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance effect) {
        // Base status particle color; dual-color visuals are emitted explicitly in applyEffectTick.
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, PINK);
    }

    private static void applyVaporifiedSlowFall(LivingEntity entity) {
        if (entity.onGround() || entity.isInWater() || entity.isInLava()) {
            return;
        }

        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.y < 0.0D) {
            double slowedY = velocity.y * VAPORIFIED_FALL_DAMPING;
            if (slowedY < VAPORIFIED_MAX_FALL_SPEED) {
                slowedY = VAPORIFIED_MAX_FALL_SPEED;
            }
            entity.setDeltaMovement(velocity.x, slowedY, velocity.z);
        }

        entity.resetFallDistance();
    }
}
