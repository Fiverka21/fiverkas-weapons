## Weapon name fonts

Gradient weapon names use the shared Minecraft font id `fweapons:weapon_names` by default.

For a per-weapon font, add one entry to `ITEM_FONTS` in `src/main/java/com/fiv/fiverkas_weapons/item/WeaponNameFonts.java`:

```java
item("blue_katana", "miyukatsu")
```

The first value is the item registry name. The second value is the font name.

To add a new font named `miyukatsu`, put the `.ttf` or `.tte` file here:

`src/main/resources/assets/fweapons/font/miyukatsu.ttf`

Then add this font definition:

`src/main/resources/assets/fweapons/font/weapon_name_fonts/miyukatsu.json`

```json
{
  "providers": [
    {
      "type": "ttf",
      "file": "fweapons:miyukatsu.ttf",
      "size": 11.0,
      "oversample": 2.0
    },
    {
      "type": "reference",
      "id": "minecraft:default"
    }
  ]
}
```
