package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.Row;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {

    public static final CreativeModeTab FWEAPONS_TAB =
            Registry.register(
                    BuiltInRegistries.CREATIVE_MODE_TAB,
                    FiverkasWeapons.id("fweapons_tab"),
                    CreativeModeTab.builder(Row.TOP, 0)
                    .title(Component.translatable("itemGroup.fweapons_tab"))
                    .icon(() -> new ItemStack(ModItems.ICON.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.VAPORWAVE_SWORD.get());
                        output.accept(ModItems.SACRILEGIOUS.get());
                        output.accept(ModItems.ANTEM.get());
                        output.accept(ModItems.MKOPI.get());
                        output.accept(ModItems.BAYONET.get());
                        output.accept(ModItems.BLUE_KATANA.get());
                        output.accept(ModItems.AIRMACE.get());
                        output.accept(ModItems.NATUREAXE.get());
                        output.accept(ModItems.DAWN.get());
                        output.accept(ModItems.DUSK.get());
                        output.accept(ModItems.LSCYTHE.get());
                        output.accept(ModItems.HARVESTER.get());
                        output.accept(ModItems.THE_FOOL.get());
                        output.accept(ModItems.HCBOW.get());
                        output.accept(ModItems.GBLUEPRINT.get());
                        output.accept(ModItems.HCBOWPRINT.get());
                        output.accept(ModItems.DREAM_ESSENCE.get());
                        output.accept(ModItems.DSHIELD.get());
                    })
                    .build());

    public static void init() {
    }
}
