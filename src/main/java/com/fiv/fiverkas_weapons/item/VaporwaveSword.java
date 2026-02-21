package com.fiv.fiverkas_weapons.item;

import com.fiv.fiverkas_weapons.registry.ModEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ItemStack;

public class VaporwaveSword extends SwordItem {

    public VaporwaveSword(Tier tier, Item.Properties properties) {
        super(tier, properties); // Uses default Diamond attack + speed
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply Vaporified for 4 seconds (80 ticks)
        target.addEffect(new MobEffectInstance(ModEffects.VAPORIFIED, 80, 0));

        // Spawn particles around target (client-side)
        if (!target.level().isClientSide) return super.hurtEnemy(stack, target, attacker);

        for (int i = 0; i < 10; i++) {
            double px = target.getX() + (Math.random() - 0.5);
            double py = target.getY() + 1.0;
            double pz = target.getZ() + (Math.random() - 0.5);

            target.level().addParticle(ParticleTypes.END_ROD, px, py, pz, 0.0, 0.1, 0.0);
        }

        return super.hurtEnemy(stack, target, attacker);
    }
}
