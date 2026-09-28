package dev.wp.phantoms_utilities.mixin;

import aztech.modern_industrialization.pipes.api.PipeNetworkType;
import aztech.modern_industrialization.pipes.impl.PipeItem;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.wp.phantoms_utilities.PUSounds;
import dev.wp.phantoms_utilities.items.SprayCan;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Held pipe acts as the offhand can's color.
@Mixin(value = PipeItem.class, remap = false)
public abstract class PipeItemMixin {
    @Unique
    private static final ThreadLocal<PipeNetworkType> phantoms_utilities$override = new ThreadLocal<>();

    @Inject(method = "useOn", at = @At("HEAD"), require = 0)
    private void phantoms_utilities$beginUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        phantoms_utilities$override.set(SprayCan.getOffhandPipeType(context.getPlayer(), ((PipeItem) (Object) this).type));
    }

    @Inject(method = "useOn", at = @At("RETURN"), require = 0)
    private void phantoms_utilities$endUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        PipeNetworkType override = phantoms_utilities$override.get();
        phantoms_utilities$override.remove();
        if (override != null && !context.getLevel().isClientSide() && cir.getReturnValue().consumesAction()) {
            context.getLevel().playSound(null, context.getClickedPos(), PUSounds.SPRAY_CAN_SPRAY, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @ModifyExpressionValue(method = {"useOn", "tryAddPipeAt", "placeAt"}, require = 0,
            at = @At(value = "FIELD", target = "Laztech/modern_industrialization/pipes/impl/PipeItem;type:Laztech/modern_industrialization/pipes/api/PipeNetworkType;"))
    private PipeNetworkType phantoms_utilities$useOffhandColor(PipeNetworkType original) {
        PipeNetworkType override = phantoms_utilities$override.get();
        return override != null ? override : original;
    }
}
