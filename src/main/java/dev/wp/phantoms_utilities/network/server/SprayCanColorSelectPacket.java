package dev.wp.phantoms_utilities.network.server;

import dev.wp.phantoms_utilities.items.SprayCan;
import dev.wp.phantoms_utilities.network.CustomPUPayload;
import dev.wp.phantoms_utilities.network.ServerBoundPacket;
import dev.wp.phantoms_utilities.util.PUColor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record SprayCanColorSelectPacket(@Nullable PUColor color) implements ServerBoundPacket {
    public static final StreamCodec<RegistryFriendlyByteBuf, SprayCanColorSelectPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    SprayCanColorSelectPacket::write,
                    SprayCanColorSelectPacket::decode);

    public static final Type<SprayCanColorSelectPacket> TYPE = CustomPUPayload
            .createType("spray_can_select_color");

    public static SprayCanColorSelectPacket decode(RegistryFriendlyByteBuf stream) {
        PUColor color = null;
        if (stream.readBoolean()) color = stream.readEnum(PUColor.class);
        return new SprayCanColorSelectPacket(color);
    }

    private static void switchColor(ItemStack stack, PUColor color) {
        if (!stack.isEmpty() && stack.getItem() instanceof SprayCan sprayCan) {
            sprayCan.setActiveColor(stack, color);
        }
    }

    @Override
    public Type<SprayCanColorSelectPacket> type() {
        return TYPE;
    }

    public void write(RegistryFriendlyByteBuf data) {
        if (color != null) {
            data.writeBoolean(true);
            data.writeEnum(color);
        } else {
            data.writeBoolean(false);
        }
    }

    @Override
    public void handleOnServer(ServerPlayer player) {
        switchColor(player.getMainHandItem(), color);
        switchColor(player.getOffhandItem(), color);
    }
}
