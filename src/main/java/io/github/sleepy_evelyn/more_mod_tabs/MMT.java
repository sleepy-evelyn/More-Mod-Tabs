package io.github.sleepy_evelyn.more_mod_tabs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.sleepy_evelyn.more_mod_tabs.item_group.ModItemGroupRegistry;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.resource.loader.api.client.ClientResourceLoaderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class MMT implements ClientModInitializer {

	private static final String MOD_ID = "more_mod_tabs";
	public static final Logger LOGGER = LoggerFactory.getLogger(MMT.MOD_ID);
	private static final Map<String, ModTabEntry> MOD_TAB_ENTRIES = new HashMap<>();

	private static final Identifier LAST_VANILLA_ID = new Identifier("minecraft", "spawn_eggs");

	@Override
	public void onInitializeClient(ModContainer mod) {
		ClientResourceLoaderEvents.START_RESOURCE_PACK_RELOAD.register(this::onClientResourcePackReload);
	}

	private void onClientResourcePackReload(ClientResourceLoaderEvents.StartResourcePackReload.Context context) {
		final List<ModContainer> newModTabContainers = new ArrayList<>();

		context.resourceManager().streamResourcePacks().forEach(resourcePack ->
			resourcePack.listResources(ResourceType.CLIENT_RESOURCES, "more_mod_tabs", "tabs", (id, resourceIoSupplier) -> {
				// Get the mod id from the file name
				final String modId = id.getPath().replace("tabs/", "").replace(".json", "");

				if (QuiltLoader.isModLoaded(modId)) {
					try (var inputStream = resourceIoSupplier.get()) {
						var tabJsonObject = (JsonObject) JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
						var modTabEntryOptional = ModTabEntry.CODEC.parse(JsonOps.INSTANCE, tabJsonObject).get().left();

						// Exit if the mod has been registered already
						if (!MOD_TAB_ENTRIES.containsKey(modId)) {
							modTabEntryOptional.ifPresentOrElse(modTabEntry -> {
								if (ModItemGroupRegistry.tryRegister(modId, modTabEntry)) {
									MOD_TAB_ENTRIES.put(modId, modTabEntry);
									QuiltLoader.getModContainer(modId).ifPresent(newModTabContainers::add);
								}
							}, () -> LOGGER.error("Failed to load additional creative mod tab entry for mod id: " + modId));
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
		}));

		String modNames = newModTabContainers.stream()
			.map(modContainer -> modContainer.metadata().name())
			.collect(Collectors.joining(", "));

		if (!modNames.isEmpty())
			LOGGER.info("Successfully registered new creative tab entries for mods: " + modNames);
	}

	public static void onItemGroupDisplayReload(ItemGroup itemGroup, Collection<ItemStack> cachedTabStacks) {
		ModItemGroupRegistry.queueStacks(MOD_TAB_ENTRIES.keySet(), cachedTabStacks);
		Identifier groupId = Registries.ITEM_GROUP.getId(itemGroup);

		if (groupId != null && groupId.equals(LAST_VANILLA_ID))
			ModItemGroupRegistry.finishStackRegistration();
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
