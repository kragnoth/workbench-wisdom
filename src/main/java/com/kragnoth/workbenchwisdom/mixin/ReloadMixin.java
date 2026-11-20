package com.kragnoth.workbenchwisdom.mixin;

import com.kragnoth.workbenchwisdom.RecipeChanges;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Right after a world loads its datapacks (and on every /reload), once the item tags are bound so tag ingredients can
 * be read, and before any player is sent the recipes.
 */
@Mixin(ReloadableServerResources.class)
public abstract class ReloadMixin {
    @Inject(method = "updateRegistryTags()V", at = @At("TAIL"))
    private void workbenchwisdom$changeRecipes(CallbackInfo ci) {
        RecipeChanges.apply(((ReloadableServerResources) (Object) this).getRecipeManager());
    }
}
