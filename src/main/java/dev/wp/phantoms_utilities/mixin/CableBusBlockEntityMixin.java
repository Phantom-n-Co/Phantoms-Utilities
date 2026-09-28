package dev.wp.phantoms_utilities.mixin;

import appeng.blockentity.networking.CableBusBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wp.phantoms_utilities.items.SprayCan;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

// Wrench pickups go straight to the inventory, skipping BlockDropsEvent.
@Pseudo
@Mixin(value = CableBusBlockEntity.class, remap = false)
public abstract class CableBusBlockEntityMixin {
    @WrapOperation(method = "disassembleWithWrench", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;)V"))
    private void phantoms_utilities$stripWrenchedCable(Inventory inventory, ItemStack stack, Operation<Void> original) {
        original.call(inventory, SprayCan.stripForOffhand(inventory.player, stack));
    }
}
