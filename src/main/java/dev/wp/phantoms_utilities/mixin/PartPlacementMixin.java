package dev.wp.phantoms_utilities.mixin;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.parts.PartPlacement;
import dev.wp.phantoms_utilities.items.SprayCan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Adding to an existing cable bus fires no placement event.
@Pseudo
@Mixin(value = PartPlacement.class, remap = false)
public abstract class PartPlacementMixin {
    @Inject(method = "placePart", at = @At("RETURN"), require = 0)
    private static void phantoms_utilities$paintPlacedCable(@Nullable Player player, Level level, IPartItem<?> partItem,
            @Nullable DataComponentMap configData, BlockPos pos, Direction side, CallbackInfoReturnable<IPart> cir) {
        IPart part = cir.getReturnValue();
        if (part != null) SprayCan.onAE2PartPlaced(level, pos, part, player);
    }
}
