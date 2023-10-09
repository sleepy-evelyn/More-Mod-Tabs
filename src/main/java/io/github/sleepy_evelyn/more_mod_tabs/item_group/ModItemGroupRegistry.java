package io.github.sleepy_evelyn.more_mod_tabs.item_group;

import io.github.sleepy_evelyn.more_mod_tabs.MMT;
import io.github.sleepy_evelyn.more_mod_tabs.ModTabEntry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public class ModItemGroupRegistry {

	private static final Map<String, List<ItemStack>> BLOCK_ITEM_STACKS = new HashMap<>();
	private static final Map<String, List<ItemStack>> ITEM_STACKS = new HashMap<>();

	public static void queueStacks(Set<String> modIds, Collection<ItemStack> cachedTabStacks) {
		for (ItemStack cachedTabStack : cachedTabStacks) {
			var itemId = Registries.ITEM.getId(cachedTabStack.getItem());
			var modId = itemId.getNamespace();

			// Filter out all the vanilla entries and entries from other mods we don't need
			if (modIds.contains(modId)) {
				BLOCK_ITEM_STACKS.computeIfAbsent(modId, k -> new ArrayList<>()).add(cachedTabStack);
				ITEM_STACKS.computeIfAbsent(modId, k -> new ArrayList<>()).add(cachedTabStack);
			}
		}
	}

	public static void finishStackRegistration() {
		for (var modItemGroupEntry : BLOCK_ITEM_STACKS.entrySet())
			modifyEntries(modItemGroupEntry);
		for (var modItemGroupEntry : ITEM_STACKS.entrySet())
			modifyEntries(modItemGroupEntry);
	}

	private static void modifyEntries(Map.Entry<String, List<ItemStack>> itemGroupEntry) {
		RegistryKey<ItemGroup> modItemGroupKey = createCustomItemGroupKey(itemGroupEntry.getKey());
		ItemGroupEvents.modifyEntriesEvent(modItemGroupKey).register((entries
			-> itemGroupEntry.getValue().forEach(entries::addStack)));
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

	private static RegistryKey<ItemGroup> createCustomItemGroupKey(String modId) {
		return RegistryKey.of(RegistryKeys.ITEM_GROUP, new Identifier(modId, modId + "_group"));
	}

	// Define a method to check if an item is vanilla.
	private static boolean isVanillaItem(ItemStack stack) {
		return Registries.ITEM.getId(stack.getItem()).getNamespace().equals("minecraft");
	}
}
