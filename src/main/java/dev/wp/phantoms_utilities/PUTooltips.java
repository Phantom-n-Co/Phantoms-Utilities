package dev.wp.phantoms_utilities;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.Arrays;
import java.util.List;

public final class PUTooltips {
    public static final Style DEFAULT_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xa9a9a9)).withItalic(false);
    public static final Style HIGHLIGHT_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xffde7d)).withItalic(false);

    public static final String SHIFT_REQUIRED = "tooltip.phantoms_utilities.shift_required";
    public static final String SPRAY_CAN_PAINT = "tooltip.phantoms_utilities.spray_can.paint";
    public static final String SPRAY_CAN_PAINT_CONNECTED = "tooltip.phantoms_utilities.spray_can.paint_connected";
    public static final String SPRAY_CAN_CYCLE = "tooltip.phantoms_utilities.spray_can.cycle";
    public static final String SPRAY_CAN_PICK = "tooltip.phantoms_utilities.spray_can.pick";
    public static final String SPRAY_CAN_PICKER = "tooltip.phantoms_utilities.spray_can.picker";
    public static final String SPRAY_CAN_OFFHAND = "tooltip.phantoms_utilities.spray_can.offhand";

    public static MutableComponent line(String key, Object... args) {
        return Component.translatable(key, args).withStyle(DEFAULT_STYLE);
    }

    public static MutableComponent key(String keyMapping) {
        return Component.keybind(keyMapping).withStyle(HIGHLIGHT_STYLE);
    }

    public static void addShiftInfo(List<Component> tooltip, Component... lines) {
        if (FMLEnvironment.dist.isClient() && Screen.hasShiftDown()) tooltip.addAll(Arrays.asList(lines));
        else tooltip.add(line(SHIFT_REQUIRED));
    }
}
