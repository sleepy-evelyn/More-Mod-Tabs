package io.github.sleepy_evelyn.more_mod_tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

public record ModTabEntry(String displayName, Item iconItem) {

	public static Codec<ModTabEntry> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codec.STRING.fieldOf("display_name").forGetter(ModTabEntry::displayName),
			Registries.ITEM.getCodec().fieldOf("icon_item").forGetter(ModTabEntry::iconItem)
		).apply(instance, ModTabEntry::new)
	);
}
