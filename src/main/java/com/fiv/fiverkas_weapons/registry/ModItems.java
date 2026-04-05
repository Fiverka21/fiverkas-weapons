package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.item.Airmace;
import com.fiv.fiverkas_weapons.item.AnimatedGradientItem;
import com.fiv.fiverkas_weapons.item.Bayonet;
import com.fiv.fiverkas_weapons.item.BlueKatana;
import com.fiv.fiverkas_weapons.item.Dawn;
import com.fiv.fiverkas_weapons.item.Dusk;
import com.fiv.fiverkas_weapons.item.GBlueprintItem;
import com.fiv.fiverkas_weapons.item.LScythe;
import com.fiv.fiverkas_weapons.item.Mkopi;
import com.fiv.fiverkas_weapons.item.NatureAxe;
import com.fiv.fiverkas_weapons.item.Sacrilegious;
import com.fiv.fiverkas_weapons.item.TheFoolBow;
import com.fiv.fiverkas_weapons.item.VaporwaveSword;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import com.fiv.fiverkas_weapons.util.CompatIds;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, FiverkasWeapons.MODID);

    // Register swords
    public static final DeferredHolder<Item, VaporwaveSword> VAPORWAVE_SWORD =
            ITEMS.register("vaporwave_sword",
                    () -> new VaporwaveSword(
                            itemProps("vaporwave_sword")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 4, -2.2f)
                    ));

    public static final DeferredHolder<Item, Sacrilegious> SACRILEGIOUS =
            ITEMS.register("sacrilegious",
                    () -> new Sacrilegious(
                            itemProps("sacrilegious")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 10, -2.6f)
                    ));

    public static final DeferredHolder<Item, Mkopi> MKOPI =
            ITEMS.register("mkopi",
                    () -> new Mkopi(
                            itemProps("mkopi")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 12, -2.8f)
                    ));

    public static final DeferredHolder<Item, Bayonet> BAYONET =
            ITEMS.register("bayonet",
                    () -> new Bayonet(
                            itemProps("bayonet")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 4, -2.4f)
                    ));

    public static final DeferredHolder<Item, BlueKatana> BLUE_KATANA =
            ITEMS.register("blue_katana",
                    () -> new BlueKatana(
                            itemProps("blue_katana")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 8, -2.4f)
                    ));

    public static final DeferredHolder<Item, Airmace> AIRMACE =
            ITEMS.register("airmace",
                    () -> new Airmace(
                            itemProps("airmace")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 4, -2.6f)
                    ));

    public static final DeferredHolder<Item, NatureAxe> NATUREAXE =
            ITEMS.register("natureaxe",
                    () -> new NatureAxe(
                            itemProps("natureaxe")
                                    .stacksTo(1)
                                    .fireResistant()
                                    .axe(ToolMaterial.NETHERITE, 6.0F, -2.6F)
                    ));

    public static final DeferredHolder<Item, Dawn> DAWN =
            ITEMS.register("dawn",
                    () -> new Dawn(
                            itemProps("dawn")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, -1, -1.4f)
                    ));

    public static final DeferredHolder<Item, Dusk> DUSK =
            ITEMS.register("dusk",
                    () -> new Dusk(
                            itemProps("dusk")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 0, -1.4f)
                    ));

    public static final DeferredHolder<Item, LScythe> LSCYTHE =
            ITEMS.register("lscythe",
                    () -> new LScythe(
                            itemProps("lscythe")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 8, -2.8f)
                    ));

    public static final DeferredHolder<Item, Item> THE_FOOL =
            ITEMS.register("thefool",
                    () -> new TheFoolBow(
                            itemProps("thefool")
                                    .stacksTo(1)
                                    .durability(384)
                    ));

    public static final DeferredHolder<Item, Item> GBLUEPRINT =
            ITEMS.register("gblueprint",
                    // Match Airmace's gradient colors and animation speed.
                    () -> new GBlueprintItem(
                            itemProps("gblueprint"),
                            0xF1CE6A,
                            0x92BFBA,
                            144L
                    ));

    public static final DeferredHolder<Item, Item> DREAM_ESSENCE =
            ITEMS.register("dream_essence",
                    () -> new AnimatedGradientItem(
                            itemProps("dream_essence"),
                            0xFF0000,
                            0x424040,
                            144L
                    ));

    public static final DeferredHolder<Item, Item> ICON =
            ITEMS.register("icon",
                    () -> new Item(itemProps("icon")));

    private static Item.Properties itemProps(String id) {
        return CompatIds.setItemId(new Item.Properties(), Registries.ITEM, FiverkasWeapons.MODID, id);
    }
}
