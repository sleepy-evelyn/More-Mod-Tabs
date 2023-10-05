# More Mod Tabs

Allows players and modpack makers to automatically add new custom tabs for mods that don't include their own.

![quilt-badge](https://raw.githubusercontent.com/intergrav/devins-badges/1aec26abb75544baec37249f42008b2fcc0e731f/assets/cozy/supported/quilt_vector.svg)

## Usage

Create a resourcepack and include the `more_mod_tabs` directory. Then include any tabs you want to include in the `tabs` folder naming each using the `mod id` of the mod you want to include.

For example if I wanted to add [Basic Weapons](https://modrinth.com/mod/basic-weapons) and [BLAST](https://www.curseforge.com/minecraft/mc-mods/blast) I could use the following layout:

```
- assets
  - more_mod_tabs
    - tabs
      basicweapons.json
      blast.json
```

Each `.json` file takes the format:
```json
{
  "display_name": "Tab Name",
  "icon_item": "minecraft:dirt"
}
```
Where the `display_name` takes the name you want to use for the new creative tab and the `icon_item` is the id of the item you want to be represented in the tab. For example for **Basic Weapons** you could use:
```json
{
  "display_name": "Basic Weapons",
  "icon_item": "basicweapons:iron_hammer"
}
```






