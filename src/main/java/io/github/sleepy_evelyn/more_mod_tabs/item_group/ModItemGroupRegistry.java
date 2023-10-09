package io.github.sleepy_evelyn.more_mod_tabs.item_group;

import io.github.sleepy_evelyn.more_mod_tabs.MMT;
import io.github.sleepy_evelyn.more_mod_tabs.ModTabEntry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public class ModItemGroupRegistry {

	public static void addStacksFromVanillaGroup(Collection<ItemStack> cachedTabStacks, Set<String> modIds) {
		Map<String, List<ItemStack>> blockItemStacks = new HashMap<>();
		Map<String, List<ItemStack>> itemStacks = new HashMap<>();

		for (ItemStack cachedTabStack : cachedTabStacks) {
			var itemId = Registries.ITEM.getId(cachedTabStack.getItem());
			var modId = itemId.getNamespace();

			// Filter out all the vanilla entries and entries from other mods we don't need
			if (modIds.contains(modId)) {
				blockItemStacks.computeIfAbsent(modId, k -> new ArrayList<>()).add(cachedTabStack);
				itemStacks.computeIfAbsent(modId, k -> new ArrayList<>()).add(cachedTabStack);
			}
		}
		for (var modItemGroupEntry : blockItemStacks.entrySet())
			modifyEntries(modItemGroupEntry);
		for (var modItemGroupEntry : itemStacks.entrySet())
			modifyEntries(modItemGroupEntry);
	}

	private static void modifyEntries(Map.Entry<String, List<ItemStack>> itemGroupEntry) {
		RegistryKey<ItemGroup> modItemGroupKey = createCustomItemGroupKey(itemGroupEntry.getKey());
		ItemGroupEvents.modifyEntriesEvent(modItemGroupKey)
			.register((entries -> itemGroupEntry.getValue().forEach(entries::addStack)));
	}

	public static boolean tryRegister(String modId, ModTabEntry modTabEntry) {
		boolean itemGroupExistsAlready = Registries.ITEM_GROUP.getEntries().stream().anyMatch(itemGroupEntry
			-> itemGroupEntry.getKey().getValue().getNamespace().equals(modId));
		if (itemGroupExistsAlready) return false;

		ItemGroup modItemGroup = FabricItemGroup.builder().icon(()
			-> modTabEntry.iconItem().getDefaultStack()).name(Text.literal(modTabEntry.displayName())).build();

		RegistryKey<ItemGroup> modItemGroupKey = createCustomItemGroupKey(modId);
		Registry.register(Registries.ITEM_GROUP, modItemGroupKey, modItemGroup);

		MMT.LOGGER.info("[More Mod Tabs] Registered Custom Mod Tab Entry with name: " + modItemGroup.getName().getString()
			+ " and icon: " + modItemGroup.getIcon().getItem().toString());
		return true;
	}

	public static RegistryKey<ItemGroup> createCustomItemGroupKey(String modId) {
		return RegistryKey.of(RegistryKeys.ITEM_GROUP, new Identifier(modId, modId));
	}
}
