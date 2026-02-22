package com.fiv.fiverkas_weapons.effect;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class BleedEffect extends MobEffect {
    private static final int BLEED_COLOR = 0xFFB00000;

    public BleedEffect() {
        super(MobEffectCategory.HARMFUL, 0xB00000);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.hurt(entity.damageSources().generic(), 1.0F);
        }
        // Returning false removes the effect instance in 1.21.1; keep it active.
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0; // every 40 ticks (2 seconds)
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance effect) {
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, BLEED_COLOR);
    }
}
