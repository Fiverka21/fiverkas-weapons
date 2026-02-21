package com.fiv.fiverkas_weapons.effect;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Vaporified debuff for NeoForge 1.21.1
 */
public class VaporifiedEffect extends MobEffect {
    private static final int PINK = 0xFFFF69B4;
    private static final int CYAN = 0xFF00FFFF;

    public VaporifiedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF69B4); // pink color
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
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
        // Returning false removes the effect instance in 1.21.1; keep it active.
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0; // every 20 ticks (1 second)
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance effect) {
        // Base status particle color; dual-color visuals are emitted explicitly in applyEffectTick.
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, PINK);
    }
}
