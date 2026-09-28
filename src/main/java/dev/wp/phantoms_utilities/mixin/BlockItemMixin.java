package dev.wp.phantoms_utilities.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wp.phantoms_utilities.items.SprayCan;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Recolor before placing, on both sides, so the client's predicted block doesn't flicker.
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @ModifyExpressionValue(method = "place", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/BlockItem;getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private @Nullable BlockState phantoms_utilities$paintPlacement(@Nullable BlockState state,
            @Local(argsOnly = true) BlockPlaceContext context) {
        return state == null ? null : SprayCan.getOffhandPlacementState(context, state);
    }
}
