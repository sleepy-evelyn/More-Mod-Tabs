package io.github.sleepy_evelyn.more_mod_tabs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.sleepy_evelyn.more_mod_tabs.item_group.ModItemGroupRegistry;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceIoSupplier;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientLifecycleEvents;
import org.quiltmc.qsl.resource.loader.api.client.ClientResourceLoaderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MMT implements ClientModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger(MMT.MOD_ID);
	private static final String MOD_ID = "more_mod_tabs";

	private static final Map<String, ModTabEntry> MOD_TAB_ENTRIES = new HashMap<>();
	private static final Identifier LAST_VANILLA_ID = new Identifier("minecraft", "spawn_eggs");

	private boolean clientReady;

	@Override
	public void onInitializeClient(ModContainer mod) {
		ClientResourceLoaderEvents.START_RESOURCE_PACK_RELOAD.register(this::onClientResourcePackReload);
		ClientLifecycleEvents.READY.register(client -> clientReady = true);
	}

	private void onClientResourcePackReload(ClientResourceLoaderEvents.StartResourcePackReload.Context context) {
		// Make sure we don't register anything after the registries have frozen
		if (!clientReady) {
			// Search for resourcepacks containing more_mod_tabs entries
			context.resourceManager().streamResourcePacks().forEach(resourcePack
				-> resourcePack.listResources(ResourceType.CLIENT_RESOURCES,
				"more_mod_tabs",
				"tabs",
				this::handleMoreModTabsResources)
			);
		}
	}

	private void handleMoreModTabsResources(Identifier resourcePackId, ResourceIoSupplier<InputStream> resourceIoSupplier) {
		// Get the mod id from the file name
		final String modId = resourcePackId.getPath().replace("tabs/", "").replace(".json", "");
		final boolean alreadyRegistered = MOD_TAB_ENTRIES.containsKey(modId); // Check if it's already been registered

		if (QuiltLoader.isModLoaded(modId) && !alreadyRegistered) {
			try (var inputStream = resourceIoSupplier.get()) {
				var tabJsonObject = (JsonObject) JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
				var modTabEntryOptional = ModTabEntry.CODEC.parse(JsonOps.INSTANCE, tabJsonObject).get().left();

				modTabEntryOptional.ifPresentOrElse(modTabEntry -> {
					if (ModItemGroupRegistry.tryRegister(modId, modTabEntry))
						MOD_TAB_ENTRIES.put(modId, modTabEntry);
				}, () -> LOGGER.error("[More Mod Tabs] Failed to load additional creative mod tab entry for mod id: " + modId));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void onItemGroupDisplayReload(ItemGroup itemGroup, Collection<ItemStack> cachedTabStacks) {
		ModItemGroupRegistry.queueStacks(MOD_TAB_ENTRIES.keySet(), cachedTabStacks);
		Identifier groupId = Registries.ITEM_GROUP.getId(itemGroup);

		if (groupId != null && groupId.equals(LAST_VANILLA_ID))
			ModItemGroupRegistry.finishStackRegistration();
	}
}
