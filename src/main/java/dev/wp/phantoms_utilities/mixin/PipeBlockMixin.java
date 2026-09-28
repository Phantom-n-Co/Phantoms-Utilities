package dev.wp.phantoms_utilities.mixin;

import aztech.modern_industrialization.pipes.impl.PipeBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wp.phantoms_utilities.items.SprayCan;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Wrench pickups skip BlockDropsEvent.
@Mixin(value = PipeBlock.class, remap = false)
public abstract class PipeBlockMixin {
    @WrapOperation(method = "useWrench", require = 0,
            at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private static ItemEntity phantoms_utilities$stripWrenchedPipe(Level level, double x, double y, double z, ItemStack stack,
            Operation<ItemEntity> original, @Local(argsOnly = true) Player player) {
        return original.call(level, x, y, z, SprayCan.stripForOffhand(player, stack));
    }
}
