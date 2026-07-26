package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.item.Airmace;
import com.fiv.fiverkas_weapons.item.AnimatedGradientItem;
import com.fiv.fiverkas_weapons.item.Antem;
import com.fiv.fiverkas_weapons.item.Bayonet;
import com.fiv.fiverkas_weapons.item.BlueKatana;
import com.fiv.fiverkas_weapons.item.AnimatedGradientShieldItem;
import com.fiv.fiverkas_weapons.item.Dawn;
import com.fiv.fiverkas_weapons.item.Dusk;
import com.fiv.fiverkas_weapons.item.GBlueprintItem;
import com.fiv.fiverkas_weapons.item.HCBowItem;
import com.fiv.fiverkas_weapons.item.Harvester;
import com.fiv.fiverkas_weapons.item.LScythe;
import com.fiv.fiverkas_weapons.item.Mkopi;
import com.fiv.fiverkas_weapons.item.NatureAxe;
import com.fiv.fiverkas_weapons.item.Sacrilegious;
import com.fiv.fiverkas_weapons.item.TheFoolBow;
import com.fiv.fiverkas_weapons.item.VaporwaveSword;
import com.fiv.fiverkas_weapons.util.CompatIds;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, FiverkasWeapons.MODID);

    /**
     * Minecraft 1.21.10 requires an item's registry key before its constructor
     * derives the description ID.  DeferredRegister does not populate it for
     * custom factories, so supply it with the properties up front.
     */
    private static Item.Properties itemProperties(String name) {
        return CompatIds.setItemId(new Item.Properties(), Registries.ITEM, FiverkasWeapons.MODID, name);
    }

    // Register swords
    public static final DeferredHolder<Item, VaporwaveSword> VAPORWAVE_SWORD =
            ITEMS.register("vaporwave_sword",
                    () -> new VaporwaveSword(
                            ToolMaterial.DIAMOND,
                            itemProperties("vaporwave_sword")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 4, -2.2f)
                    ));

    public static final DeferredHolder<Item, Sacrilegious> SACRILEGIOUS =
            ITEMS.register("sacrilegious",
                    () -> new Sacrilegious(
                            ToolMaterial.DIAMOND,
                            itemProperties("sacrilegious")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 8, -2.6f)
                    ));

    public static final DeferredHolder<Item, Antem> ANTEM =
            ITEMS.register("antem",
                    () -> new Antem(
                            ToolMaterial.DIAMOND,
                            itemProperties("antem")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 10, -2.6f)
                    ));

    public static final DeferredHolder<Item, Mkopi> MKOPI =
            ITEMS.register("mkopi",
                    () -> new Mkopi(
                            ToolMaterial.DIAMOND,
                            itemProperties("mkopi")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 12, -2.8f)
                    ));

    public static final DeferredHolder<Item, Bayonet> BAYONET =
            ITEMS.register("bayonet",
                    () -> new Bayonet(
                            ToolMaterial.DIAMOND,
                            itemProperties("bayonet")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 4, -2.4f)
                    ));

    public static final DeferredHolder<Item, BlueKatana> BLUE_KATANA =
            ITEMS.register("blue_katana",
                    () -> new BlueKatana(
                            ToolMaterial.DIAMOND,
                            itemProperties("blue_katana")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 8, -2.4f)
                    ));

    public static final DeferredHolder<Item, Airmace> AIRMACE =
            ITEMS.register("airmace",
                    () -> new Airmace(
                            ToolMaterial.DIAMOND,
                            itemProperties("airmace")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 4, -2.6f)
                    ));

    public static final DeferredHolder<Item, NatureAxe> NATUREAXE =
            ITEMS.register("natureaxe",
                    () -> new NatureAxe(
                            ToolMaterial.NETHERITE,
                            itemProperties("natureaxe")
                                    .stacksTo(1)
                                    .fireResistant()
                                    .axe(ToolMaterial.NETHERITE, 6, -2.6f)
                    ));

    public static final DeferredHolder<Item, Dawn> DAWN =
            ITEMS.register("dawn",
                    () -> new Dawn(
                            ToolMaterial.DIAMOND,
                            itemProperties("dawn")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, -1, -1.4f)
                    ));

    public static final DeferredHolder<Item, Dusk> DUSK =
            ITEMS.register("dusk",
                    () -> new Dusk(
                            ToolMaterial.DIAMOND,
                            itemProperties("dusk")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 0, -1.4f)
                    ));

    public static final DeferredHolder<Item, LScythe> LSCYTHE =
            ITEMS.register("lscythe",
                    () -> new LScythe(
                            ToolMaterial.DIAMOND,
                            itemProperties("lscythe")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 8, -2.8f)
                    ));

    public static final DeferredHolder<Item, Harvester> HARVESTER =
            ITEMS.register("harvester",
                    () -> new Harvester(
                            ToolMaterial.DIAMOND,
                            itemProperties("harvester")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 2, -2.2f)
                    ));

    public static final DeferredHolder<Item, Item> THE_FOOL =
            ITEMS.register("thefool",
                    () -> new TheFoolBow(
                            itemProperties("thefool")
                                    .stacksTo(1)
                                    .durability(384)
                    ));

    public static final DeferredHolder<Item, Item> HCBOW =
            ITEMS.register("hcbow",
                    () -> new HCBowItem(
                            itemProperties("hcbow")
                                    .stacksTo(1)
                                    .durability(465)
                    ));

    public static final DeferredHolder<Item, Item> GBLUEPRINT =
            ITEMS.register("gblueprint",
                    // Match Airmace's gradient colors and animation speed.
                    () -> new GBlueprintItem(
                            itemProperties("gblueprint"),
                            0xF1CE6A,
                            0x92BFBA,
                            144L
                    ));

    public static final DeferredHolder<Item, Item> HCBOWPRINT =
            ITEMS.register("hcbowprint",
                    () -> new AnimatedGradientItem(
                            itemProperties("hcbowprint"),
                            HCBowItem.GRADIENT_START,
                            HCBowItem.GRADIENT_END,
                            HCBowItem.COLOR_SHIFT_SPEED_MS
                    ));

    public static final DeferredHolder<Item, Item> DREAM_ESSENCE =
            ITEMS.register("dream_essence",
                    () -> new AnimatedGradientItem(
                            itemProperties("dream_essence"),
                            0xFF0000,
                            0x424040,
                            144L
                    ));

    public static final DeferredHolder<Item, Item> ICON =
            ITEMS.register("icon",
                    () -> new Item(itemProperties("icon")));

    public static final DeferredHolder<Item, AnimatedGradientShieldItem> DSHIELD =
            ITEMS.register("dshield",
                    () -> new AnimatedGradientShieldItem(
                            itemProperties("dshield")
                                    .durability(672)
                                    .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY),
                            0x90EE90,
                            0xA020F0,
                            144L
                    ));
}
