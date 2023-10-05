package io.github.sleepy_evelyn.more_mod_tabs.mixin;

import io.github.sleepy_evelyn.more_mod_tabs.MMT;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UnsizedItemStackSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;

@Mixin(ItemGroup.class)
public class ItemGroupMixin {
	@Shadow private Collection<ItemStack> cachedTabStacks = UnsizedItemStackSet.get();

	@Inject(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemGroup;reloadSearch()V"))
	private void updateEntriesMixin(ItemGroup.DisplayParameters parameters, CallbackInfo ci) {
		MMT.onItemGroupDisplayReload((ItemGroup) (Object) this, cachedTabStacks);
	}
}
