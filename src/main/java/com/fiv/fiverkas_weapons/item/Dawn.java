package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.item.Item;

public class Dawn extends AnimatedGradientSwordItem {
    private static final int GOLD = 0xFFD86B;
    private static final int SOFT_PINK = 0xFF8FB1;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public Dawn(Item.Properties properties) {
        super(properties, GOLD, SOFT_PINK, COLOR_SHIFT_SPEED_MS);
    }
}
