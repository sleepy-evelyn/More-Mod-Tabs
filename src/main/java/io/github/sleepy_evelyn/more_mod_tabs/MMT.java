package io.github.sleepy_evelyn.more_mod_tabs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.sleepy_evelyn.more_mod_tabs.item_group.ModItemGroupRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceIoSupplier;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.WorldEvents;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientLifecycleEvents;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientWorldTickEvents;
import org.quiltmc.qsl.networking.api.client.ClientLoginConnectionEvents;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;
import org.quiltmc.qsl.resource.loader.api.client.ClientResourceLoaderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MMT implements ClientModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger(MMT.MOD_ID);
	private static final String MOD_ID = "more_mod_tabs";

	private static final Map<String, ModTabEntry> MOD_TAB_ENTRIES = new HashMap<>();
	private final AtomicBoolean STARTED_TICKING = new AtomicBoolean(false);

	private boolean clientReady;

	@Override
	public void onInitializeClient(ModContainer mod) {
		ClientResourceLoaderEvents.START_RESOURCE_PACK_RELOAD.register(this::onClientResourcePackReload);
		ClientLifecycleEvents.READY.register(client -> clientReady = true);
		ClientWorldTickEvents.START.register(this::onClientTick);
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

	private void onClientTick(MinecraftClient client, ClientWorld clientWorld) {
		// Only fire once when the client world gets loaded
		if (STARTED_TICKING.compareAndSet(false, true)) {
			if (clientWorld != null && client.getNetworkHandler() != null) {
				/* 	Reload item group entries early before the creative screen is opened. Done to form an early cache of the
					tab stacks before the creative screen is populated so we don't get empty tabs */
				ItemGroups.tryReloadEntries(client.getNetworkHandler().getEnabledFlags(),
					client.options.getOperatorItemsTab().get(),
					clientWorld.getRegistryManager()
				);
			}
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
				LOGGER.error("[More Mod Tabs] An exception occurred loading a more mod tabs .json resourcepack file. " +
					"Check the file name and contents are correct");
				e.printStackTrace();
			}
		}
	}

	public static void onItemGroupDisplayReload(ItemGroup itemGroup, Collection<ItemStack> cachedTabStacks) {
		Identifier groupId = Registries.ITEM_GROUP.getId(itemGroup);
		if (groupId != null) {
			boolean isVanillaGroup = groupId.getNamespace().equals("minecraft");
			if (isVanillaGroup)
				ModItemGroupRegistry.addStacksFromVanillaGroup(cachedTabStacks, MOD_TAB_ENTRIES.keySet());
		}
	}
}
