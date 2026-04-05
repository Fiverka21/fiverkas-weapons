package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.item.Item;

public class Bayonet extends AnimatedGradientSwordItem {
    private static final int WHITE = 0xFFFFFF;
    private static final int BLACK = 0X000000;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public Bayonet(Item.Properties properties) {
        super(properties, WHITE, BLACK, COLOR_SHIFT_SPEED_MS);
    }

}
