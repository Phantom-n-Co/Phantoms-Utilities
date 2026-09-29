package dev.wp.phantoms_utilities.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public enum PUColor {
    WHITE(DyeColor.WHITE),
    LIGHT_GRAY(DyeColor.LIGHT_GRAY),
    GRAY(DyeColor.GRAY),
    BLACK(DyeColor.BLACK),
    BROWN(DyeColor.BROWN),
    RED(DyeColor.RED),
    ORANGE(DyeColor.ORANGE),
    YELLOW(DyeColor.YELLOW),
    LIME(DyeColor.LIME),
    GREEN(DyeColor.GREEN),
    CYAN(DyeColor.CYAN),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE),
    BLUE(DyeColor.BLUE),
    PURPLE(DyeColor.PURPLE),
    MAGENTA(DyeColor.MAGENTA),
    PINK(DyeColor.PINK),
    CLEAR(null);

    public static final String CLEAR_TRANSLATION_KEY = "color.phantoms_utilities.clear";
    public static final Codec<PUColor> CODEC = Codec.STRING.comapFlatMap(PUColor::byName, PUColor::getName);
    public static final StreamCodec<FriendlyByteBuf, PUColor> STREAM_CODEC = NeoForgeStreamCodecs
            .enumCodec(PUColor.class);
    public static final List<PUColor> VALID_COLORS = Arrays.asList(WHITE, LIGHT_GRAY, GRAY, BLACK, LIME, YELLOW,
            ORANGE, BROWN, RED, PINK, MAGENTA, PURPLE, BLUE, LIGHT_BLUE, CYAN, GREEN);
    public final @Nullable DyeColor dye;

    PUColor(@Nullable DyeColor dye) {
        this.dye = dye;
    }

    public static PUColor fromDye(DyeColor vanillaDye) {
        for (var value : values()) if (value.dye == vanillaDye) return value;
        throw new IllegalArgumentException("Unknown Vanilla dye: " + vanillaDye);
    }

    private static DataResult<PUColor> byName(String name) {
        for (var value : values()) if (value.getName().equals(name)) return DataResult.success(value);
        return DataResult.error(() -> "Unknown color: " + name);
    }

    // Vanilla dye name ("light_blue"), as used in block IDs.
    public String getName() {
        return dye == null ? "clear" : dye.getName();
    }

    public Component getDisplayName() {
        return Component.translatable(dye == null ? CLEAR_TRANSLATION_KEY : "color.minecraft." + dye.getName());
    }
}
