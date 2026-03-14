package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TheFoolBow extends AnimatedGradientBowItem {
    private static final int GRADIENT_START = 0xFF6A00;
    private static final int GRADIENT_END = 0xFFE4B4;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public TheFoolBow(Item.Properties properties) {
        super(properties, GRADIENT_START, GRADIENT_END, COLOR_SHIFT_SPEED_MS);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        ItemStack pickup = ammo.copyWithCount(1);
        AbstractArrow arrow = new SpectralArrow(level, shooter, pickup, weapon);
        if (isCrit) {
            arrow.setCritArrow(true);
        }
        return customArrow(arrow, ammo, weapon);
    }
}
