package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.item.Item;

public class Dusk extends AnimatedGradientSwordItem {
    private static final int TWILIGHT_PURPLE = 0x5B3C88;
    private static final int DEEP_BLUE = 0x1B1F6B;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public Dusk(Item.Properties properties) {
        super(properties, TWILIGHT_PURPLE, DEEP_BLUE, COLOR_SHIFT_SPEED_MS);
    }
}
