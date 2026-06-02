package com.fiv.fiverkas_weapons.item;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * Font ids used by styled weapon names.
 *
 * To set a weapon font, add one entry to ITEM_FONTS:
 * item("blue_katana", "miyukatsu")
 * The font name should match assets/fweapons/font/weapon_name_fonts/<font_name>.json.
 */
public final class WeaponNameFonts {
    public static final ResourceLocation DEFAULT = font("weapon_names");
    private static final String DESCRIPTION_ID_PREFIX = "item." + FiverkasWeapons.MODID + ".";

    private static final Map<String, String> ITEM_FONTS = Map.ofEntries(
            item("blue_katana", "miyukatsu"),
            item("sacrilegious", "oldenglishfive"),
            item("antem", "oldenglishfive"),
            item("mkopi", "tfsadistic"),
            item("dream_essence", "tfsadistic")
    );

    private WeaponNameFonts() {
    }

    public static ResourceLocation forDescriptionId(String descriptionId, ResourceLocation fallback) {
        return itemNameFromDescriptionId(descriptionId)
                .flatMap(itemName -> Optional.ofNullable(ITEM_FONTS.get(itemName)))
                .map(WeaponNameFonts::fontForName)
                .orElse(fallback);
    }

    public static ResourceLocation font(String path) {
        return ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, path);
    }

    public static ResourceLocation weapon(String itemName) {
        return font("weapon_names/" + itemName);
    }

    private static Map.Entry<String, String> item(String itemName, String fontName) {
        return Map.entry(itemName, fontName);
    }

    private static ResourceLocation fontForName(String fontName) {
        return font("weapon_name_fonts/" + fontName);
    }

    private static Optional<String> itemNameFromDescriptionId(String descriptionId) {
        if (!descriptionId.startsWith(DESCRIPTION_ID_PREFIX)) {
            return Optional.empty();
        }

        return Optional.of(descriptionId.substring(DESCRIPTION_ID_PREFIX.length()));
    }
}
