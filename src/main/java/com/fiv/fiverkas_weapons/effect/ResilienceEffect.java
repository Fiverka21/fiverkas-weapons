package com.fiv.fiverkas_weapons.effect;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ResilienceEffect extends MobEffect {
    private static final int DARK_PURPLE = 0xFF2B004D;

    public ResilienceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x2B004D);
        addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "resilience_knockback_resistance"),
                1.0D,
                AttributeModifier.Operation.ADD_VALUE
        );
        addAttributeModifier(
                Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,
                ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "resilience_explosion_knockback_resistance"),
                1.0D,
                AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance effect) {
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, DARK_PURPLE);
    }
}
