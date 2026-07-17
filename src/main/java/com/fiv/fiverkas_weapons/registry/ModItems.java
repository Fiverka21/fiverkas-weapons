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

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.function.Supplier;

public class ModItems {

    private static <T extends Item> Supplier<T> register(String name, T item) {
        T registered = Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.ITEM,
                FiverkasWeapons.id(name),
                item
        );
        return () -> registered;
    }

    // Register swords
    public static final Supplier<VaporwaveSword> VAPORWAVE_SWORD =
            register("vaporwave_sword",
                    new VaporwaveSword(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.2f))
                    ));

    public static final Supplier<Sacrilegious> SACRILEGIOUS =
            register("sacrilegious",
                    new Sacrilegious(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 8, -2.6f))
                    ));

    public static final Supplier<Antem> ANTEM =
            register("antem",
                    new Antem(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 10, -2.6f))
                    ));

    public static final Supplier<Mkopi> MKOPI =
            register("mkopi",
                    new Mkopi(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 12, -2.8f))
                    ));

    public static final Supplier<Bayonet> BAYONET =
            register("bayonet",
                    new Bayonet(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.4f))
                    ));

    public static final Supplier<BlueKatana> BLUE_KATANA =
            register("blue_katana",
                    new BlueKatana(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 8, -2.4f))
                    ));

    public static final Supplier<Airmace> AIRMACE =
            register("airmace",
                    new Airmace(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.6f))
                    ));

    public static final Supplier<NatureAxe> NATUREAXE =
            register("natureaxe",
                    new NatureAxe(
                            Tiers.NETHERITE,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .fireResistant()
                                    .attributes(AxeItem.createAttributes(Tiers.NETHERITE, 6, -2.6f))
                    ));

    public static final Supplier<Dawn> DAWN =
            register("dawn",
                    new Dawn(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, -1, -1.4f))
                    ));

    public static final Supplier<Dusk> DUSK =
            register("dusk",
                    new Dusk(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 0, -1.4f))
                    ));

    public static final Supplier<LScythe> LSCYTHE =
            register("lscythe",
                    new LScythe(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 8, -2.8f))
                    ));

    public static final Supplier<Harvester> HARVESTER =
            register("harvester",
                    new Harvester(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 2, -2.2f))
                    ));

    public static final Supplier<Item> THE_FOOL =
            register("thefool",
                    new TheFoolBow(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(384)
                    ));

    public static final Supplier<Item> HCBOW =
            register("hcbow",
                    new HCBowItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(465)
                    ));

    public static final Supplier<Item> GBLUEPRINT =
            register("gblueprint",
                    // Match Airmace's gradient colors and animation speed.
                    new GBlueprintItem(
                            new Item.Properties(),
                            0xF1CE6A,
                            0x92BFBA,
                            144L
                    ));

    public static final Supplier<Item> HCBOWPRINT =
            register("hcbowprint",
                    new AnimatedGradientItem(
                            new Item.Properties(),
                            HCBowItem.GRADIENT_START,
                            HCBowItem.GRADIENT_END,
                            HCBowItem.COLOR_SHIFT_SPEED_MS
                    ));

    public static final Supplier<Item> DREAM_ESSENCE =
            register("dream_essence",
                    new AnimatedGradientItem(
                            new Item.Properties(),
                            0xFF0000,
                            0x424040,
                            144L
                    ));

    public static final Supplier<Item> ICON =
            register("icon",
                    new Item(new Item.Properties()));

    public static final Supplier<AnimatedGradientShieldItem> DSHIELD =
            register("dshield",
                    new AnimatedGradientShieldItem(
                            new Item.Properties()
                                    .durability(672)
                                    .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY),
                            0x90EE90,
                            0xA020F0,
                            144L
                    ));

    public static void init() {
    }
}
