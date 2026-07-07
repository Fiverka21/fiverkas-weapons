package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

public class Harvester extends AnimatedGradientSwordItem {
    private static final int RED = 0xFF0000;
    private static final int DARK_BLOOD_RED = 0x3A0000;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public Harvester(Tier tier, Item.Properties properties) {
        super(tier, properties, RED, DARK_BLOOD_RED, COLOR_SHIFT_SPEED_MS);
    }
}
