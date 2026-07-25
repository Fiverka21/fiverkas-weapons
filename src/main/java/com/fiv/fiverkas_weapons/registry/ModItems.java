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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ModItems {

    private static Item.Properties properties(String name) {
        return new Item.Properties()
                .setId(ResourceKey.create(BuiltInRegistries.ITEM.key(), FiverkasWeapons.id(name)));
    }

    private static <T extends Item> Supplier<T> register(String name, T item) {
        T registered = Registry.register(
                BuiltInRegistries.ITEM,
                FiverkasWeapons.id(name),
                item
        );
        return () -> registered;
    }

    // Register swords
    public static final Supplier<VaporwaveSword> VAPORWAVE_SWORD =
            register("vaporwave_sword",
                    new VaporwaveSword(
                            ToolMaterial.DIAMOND,
                            properties("vaporwave_sword")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 4, -2.2f)
                    ));

    public static final Supplier<Sacrilegious> SACRILEGIOUS =
            register("sacrilegious",
                    new Sacrilegious(
                            ToolMaterial.DIAMOND,
                            properties("sacrilegious")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 8, -2.6f)
                    ));

    public static final Supplier<Antem> ANTEM =
            register("antem",
                    new Antem(
                            ToolMaterial.DIAMOND,
                            properties("antem")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 10, -2.6f)
                    ));

    public static final Supplier<Mkopi> MKOPI =
            register("mkopi",
                    new Mkopi(
                            ToolMaterial.DIAMOND,
                            properties("mkopi")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 12, -2.8f)
                    ));

    public static final Supplier<Bayonet> BAYONET =
            register("bayonet",
                    new Bayonet(
                            ToolMaterial.DIAMOND,
                            properties("bayonet")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 4, -2.4f)
                    ));

    public static final Supplier<BlueKatana> BLUE_KATANA =
            register("blue_katana",
                    new BlueKatana(
                            ToolMaterial.DIAMOND,
                            properties("blue_katana")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 8, -2.4f)
                    ));

    public static final Supplier<Airmace> AIRMACE =
            register("airmace",
                    new Airmace(
                            ToolMaterial.DIAMOND,
                            properties("airmace")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 4, -2.6f)
                    ));

    public static final Supplier<NatureAxe> NATUREAXE =
            register("natureaxe",
                    new NatureAxe(
                            ToolMaterial.NETHERITE,
                            properties("natureaxe")
                                    .stacksTo(1)
                                    .fireResistant()
                                    .axe(ToolMaterial.NETHERITE, 6, -2.6f)
                    ));

    public static final Supplier<Dawn> DAWN =
            register("dawn",
                    new Dawn(
                            ToolMaterial.DIAMOND,
                            properties("dawn")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, -1, -1.4f)
                    ));

    public static final Supplier<Dusk> DUSK =
            register("dusk",
                    new Dusk(
                            ToolMaterial.DIAMOND,
                            properties("dusk")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 0, -1.4f)
                    ));

    public static final Supplier<LScythe> LSCYTHE =
            register("lscythe",
                    new LScythe(
                            ToolMaterial.DIAMOND,
                            properties("lscythe")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 8, -2.8f)
                    ));

    public static final Supplier<Harvester> HARVESTER =
            register("harvester",
                    new Harvester(
                            ToolMaterial.DIAMOND,
                            properties("harvester")
                                    .stacksTo(1)
                                    .sword(ToolMaterial.DIAMOND, 2, -2.2f)
                    ));

    public static final Supplier<Item> THE_FOOL =
            register("thefool",
                    new TheFoolBow(
                            properties("thefool")
                                    .stacksTo(1)
                                    .durability(384)
                    ));

    public static final Supplier<Item> HCBOW =
            register("hcbow",
                    new HCBowItem(
                            properties("hcbow")
                                    .stacksTo(1)
                                    .durability(465)
                    ));

    public static final Supplier<Item> GBLUEPRINT =
            register("gblueprint",
                    // Match Airmace's gradient colors and animation speed.
                    new GBlueprintItem(
                            properties("gblueprint"),
                            0xF1CE6A,
                            0x92BFBA,
                            144L
                    ));

    public static final Supplier<Item> HCBOWPRINT =
            register("hcbowprint",
                    new AnimatedGradientItem(
                            properties("hcbowprint"),
                            HCBowItem.GRADIENT_START,
                            HCBowItem.GRADIENT_END,
                            HCBowItem.COLOR_SHIFT_SPEED_MS
                    ));

    public static final Supplier<Item> DREAM_ESSENCE =
            register("dream_essence",
                    new AnimatedGradientItem(
                            properties("dream_essence"),
                            0xFF0000,
                            0x424040,
                            144L
                    ));

    public static final Supplier<Item> ICON =
            register("icon",
                    new Item(properties("icon")));

    public static final Supplier<AnimatedGradientShieldItem> DSHIELD =
            register("dshield",
                    new AnimatedGradientShieldItem(
                            properties("dshield")
                                    .durability(672)
                                    .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                                    .repairable(ItemTags.WOODEN_TOOL_MATERIALS)
                                    .equippableUnswappable(EquipmentSlot.OFFHAND)
                                    .component(
                                            DataComponents.BLOCKS_ATTACKS,
                                            new BlocksAttacks(
                                                    0.25F,
                                                    1.0F,
                                                    List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                                                    new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                                                    Optional.of(DamageTypeTags.BYPASSES_SHIELD),
                                                    Optional.of(SoundEvents.SHIELD_BLOCK),
                                                    Optional.of(SoundEvents.SHIELD_BREAK)
                                            )
                                    )
                                    .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK),
                            0x90EE90,
                            0xA020F0,
                            144L
                    ));

    public static void init() {
    }
}
