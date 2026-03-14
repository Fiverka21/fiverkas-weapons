package com.fiv.fiverkas_weapons.effect;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class EternityEffect extends MobEffect {
    private static final int ETERNITY_COLOR = 0xFFFFB347;
    private static final double SLOW_PER_STACK = -0.1D;
    private static final int MAX_SLOW_STACKS = 8;
    private static final float DAMAGE_PER_SECOND_PER_STACK = 0.25F;
    private static final ResourceKey<DamageType> ETERNITY_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "eternity")
    );

    public EternityEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFB347);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "eternity_slow"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                level -> SLOW_PER_STACK * Math.min(level + 1, MAX_SLOW_STACKS)
        );
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            if (entity.tickCount % 20 == 0) {
                int stacks = amplifier + 1;
                float damage = DAMAGE_PER_SECOND_PER_STACK * stacks;
                DamageSource eternity = new DamageSource(
                        entity.level().registryAccess()
                                .registryOrThrow(Registries.DAMAGE_TYPE)
                                .getHolderOrThrow(ETERNITY_DAMAGE)
                );
                entity.hurt(eternity, damage);
            }
        }
        // Returning false removes the effect instance in 1.21.1; keep it active.
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance effect) {
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ETERNITY_COLOR);
    }

    
}
