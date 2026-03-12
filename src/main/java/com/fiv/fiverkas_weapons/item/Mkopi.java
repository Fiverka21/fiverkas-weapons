package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.item.Item;

public class Mkopi extends AnimatedGradientSwordItem {
    private static final int GREY = 0x424040;
    private static final int RED = 0xFF0000;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public Mkopi(Item.Properties properties) {
        super(properties, RED, GREY, COLOR_SHIFT_SPEED_MS);
    }
}
