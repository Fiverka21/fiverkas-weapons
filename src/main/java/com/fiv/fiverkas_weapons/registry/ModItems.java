package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.item.Bayonet;
import com.fiv.fiverkas_weapons.item.BlueKatana;
import com.fiv.fiverkas_weapons.item.Mkopi;
import com.fiv.fiverkas_weapons.item.Sacrilegious;
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

    public static final DeferredHolder<Item, Sacrilegious> SACRILEGIOUS =
            ITEMS.register("sacrilegious",
                    () -> new Sacrilegious(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 10, -2.4f))
                    ));

    public static final DeferredHolder<Item, Mkopi> MKOPI =
            ITEMS.register("mkopi",
                    () -> new Mkopi(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 12, -2.4f))
                    ));

    public static final DeferredHolder<Item, Bayonet> BAYONET =
            ITEMS.register("bayonet",
                    () -> new Bayonet(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.4f))
                    ));

    public static final DeferredHolder<Item, BlueKatana> BLUE_KATANA =
            ITEMS.register("blue_katana",
                    () -> new BlueKatana(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.4f))
                    ));
}
