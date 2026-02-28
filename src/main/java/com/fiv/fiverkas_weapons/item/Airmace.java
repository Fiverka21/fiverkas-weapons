package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

public class Airmace extends AnimatedGradientSwordItem {
    private static final int LIGHT_YELLOW = 0xF1CE6A;
    private static final int BLAND_CYAN = 0x92BFBA;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public Airmace(Tier tier, Item.Properties properties) {
        super(tier, properties, LIGHT_YELLOW, BLAND_CYAN, COLOR_SHIFT_SPEED_MS);
    }
}
