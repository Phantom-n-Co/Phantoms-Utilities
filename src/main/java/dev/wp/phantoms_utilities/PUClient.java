package dev.wp.phantoms_utilities;

import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.util.AEColor;
import aztech.modern_industrialization.pipes.impl.PipeBlock;
import aztech.modern_industrialization.pipes.impl.PipeBlockEntity;
import dev.wp.phantoms_utilities.client.gui.SprayCanColorScreen;
import dev.wp.phantoms_utilities.helpers.IMouseWheelItem;
import dev.wp.phantoms_utilities.items.SprayCan;
import dev.wp.phantoms_utilities.network.ServerBoundPacket;
import dev.wp.phantoms_utilities.network.server.MWPacket;
import dev.wp.phantoms_utilities.network.server.SprayCanColorSelectPacket;
import dev.wp.phantoms_utilities.util.PUColor;
import dev.wp.phantoms_utilities.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = PhantomsUtilities.ID, dist = Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = PhantomsUtilities.ID)
public class PUClient {

    public PUClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(PUClient::registerItemProperties);
    }

    // Selects the spray_can model override matching the stored color (ordinal; Clear when unset).
    private static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(PUItems.SPRAY_CAN.get(), PhantomsUtilities.id("color"),
                (stack, level, entity, seed) -> stack.getOrDefault(PUComponents.SELECTED_COLOR, PUColor.CLEAR).ordinal()));
    }

    @SubscribeEvent
    private static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS
                && Minecraft.getInstance().options.keyPickItem.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))
                && openPickerOnMiss()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private static void onKey(InputEvent.Key event) {
        final Minecraft mc = Minecraft.getInstance();
        if (event.getAction() == GLFW.GLFW_PRESS
                && mc.options.keyPickItem.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()))
                && openPickerOnMiss()) {
            while (mc.options.keyPickItem.consumeClick()) ;
        }
    }

    private static boolean openPickerOnMiss() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.getOverlay() != null) return false;
        if (mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS) return false;
        if (!(mc.player.getMainHandItem().getItem() instanceof SprayCan || mc.player.getOffhandItem().getItem() instanceof SprayCan)) return false;
        mc.setScreen(new SprayCanColorScreen());
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private static void onKeyInput(final InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isPickBlock()) return;

        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;
        if (player == null) return;

        if (player.getMainHandItem().getItem() instanceof SprayCan || player.getOffhandItem().getItem() instanceof SprayCan) {
            event.setCanceled(true);

            if (player.isShiftKeyDown()) {
                mc.setScreen(new SprayCanColorScreen());
                return;
            }

            HitResult target = mc.hitResult;
            if (target != null && target.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) target;
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = player.level().getBlockState(pos);

                if (SprayCan.isBlacklisted(state)) return;

                if (Utils.isAE2Loaded) {
                    BlockEntity be = player.level().getBlockEntity(pos);
                    if (be instanceof IColorableBlockEntity colorable) {
                        if (colorable.getColor().equals(AEColor.TRANSPARENT)) {
                            PacketDistributor.sendToServer(new SprayCanColorSelectPacket(PUColor.CLEAR));
                            return;
                        }

                        for (PUColor color : PUColor.VALID_COLORS) {
                            if (color.name().equals(colorable.getColor().name())) {
                                PacketDistributor.sendToServer(new SprayCanColorSelectPacket(color));
                                return;
                            }
                        }
                    }
                }
                if (Utils.isMILoaded) {
                    BlockEntity be = player.level().getBlockEntity(pos);
                    if (be instanceof PipeBlockEntity) {
                        var hitPart = PipeBlock.getHitPart(player.level(), pos, blockHit);
                        var path = hitPart.type.getIdentifier().getPath();
                        if (path.contains("_cable")) {
                          return;
                        }
                        if (path.equals("fluid_pipe") || path.equals("item_pipe")) {
                            PacketDistributor.sendToServer(new SprayCanColorSelectPacket(PUColor.CLEAR));
                            return;
                        }
                        for (PUColor color : PUColor.VALID_COLORS) {
                            if (hitPart.type.getIdentifier().getPath().startsWith(color.getName() + "_")) {
                                PacketDistributor.sendToServer(new SprayCanColorSelectPacket(color));
                            }
                        }
                    }
                }

                String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                for (PUColor color : PUColor.VALID_COLORS) {
                    if (path.contains(color.getName())) {
                        PacketDistributor.sendToServer(new SprayCanColorSelectPacket(color));
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private static void wheelEvent(final InputEvent.MouseScrollingEvent me) {
        if (me.getScrollDeltaY() == 0) return;

        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;

        if (player != null && player.isShiftKeyDown()) {
            if (player.getMainHandItem().getItem() instanceof IMouseWheelItem || player.getOffhandItem().getItem() instanceof IMouseWheelItem) {
                ServerBoundPacket msg = new MWPacket(me.getScrollDeltaY() > 0);
                PacketDistributor.sendToServer(msg);
                me.setCanceled(true);
            }
        }
    }
}
