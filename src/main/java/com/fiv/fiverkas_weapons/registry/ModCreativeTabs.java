package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FiverkasWeapons.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FWEAPONS_TAB =
            CREATIVE_MODE_TABS.register("fweapons_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fweapons_tab"))
                    .icon(() -> new ItemStack(ModItems.VAPORWAVE_SWORD.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.VAPORWAVE_SWORD.get());
                        output.accept(ModItems.SACRILEGIOUS.get());
                    })
                    .build());
}
