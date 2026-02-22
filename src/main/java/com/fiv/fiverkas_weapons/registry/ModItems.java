package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.item.VaporwaveSword;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, FiverkasWeapons.MODID);

    // Register swords
    public static final DeferredHolder<Item, VaporwaveSword> VAPORWAVE_SWORD =
            ITEMS.register("vaporwave_sword",
                    () -> new VaporwaveSword(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.4f))
                    ));

    public static final DeferredHolder<Item, SwordItem> SACRILEGIOUS =
            ITEMS.register("sacrilegious",
                    () -> new SwordItem(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 8, -2.4f))
                    ));

    public static final DeferredHolder<Item, SwordItem> MKOPI =
            ITEMS.register("mkopi",
                    () -> new SwordItem(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 10, -2.4f))
                    ));

    public static final DeferredHolder<Item, SwordItem> BAYONET =
            ITEMS.register("bayonet",
                    () -> new SwordItem(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4f))
                    ));
}
