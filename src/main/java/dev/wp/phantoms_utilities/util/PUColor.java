package dev.wp.phantoms_utilities.util;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Arrays;
import java.util.List;

public enum PUColor implements StringRepresentable {
    WHITE("White", "gui.phantoms_utilities.White", "white", DyeColor.WHITE),
    LIGHT_GRAY("Light Gray", "gui.phantoms_utilities.LightGray", "light_gray", DyeColor.LIGHT_GRAY),
    GRAY("Gray", "gui.phantoms_utilities.Gray", "gray", DyeColor.GRAY),
    BLACK("Black", "gui.phantoms_utilities.Black", "black", DyeColor.BLACK),
    LIME("Lime", "gui.phantoms_utilities.Lime", "lime", DyeColor.LIME),
    YELLOW("Yellow", "gui.phantoms_utilities.Yellow", "yellow", DyeColor.YELLOW),
    ORANGE("Orange", "gui.phantoms_utilities.Orange", "orange", DyeColor.ORANGE),
    BROWN("Brown", "gui.phantoms_utilities.Brown", "brown", DyeColor.BROWN),
    RED("Red", "gui.phantoms_utilities.Red", "red", DyeColor.RED),
    PINK("Pink", "gui.phantoms_utilities.Pink", "pink", DyeColor.PINK),
    MAGENTA("Magenta", "gui.phantoms_utilities.Magenta", "magenta", DyeColor.MAGENTA),
    PURPLE("Purple", "gui.phantoms_utilities.Purple", "purple", DyeColor.PURPLE),
    BLUE("Blue", "gui.phantoms_utilities.Blue", "blue", DyeColor.BLUE),
    LIGHT_BLUE("Light Blue", "gui.phantoms_utilities.LightBlue", "light_blue", DyeColor.LIGHT_BLUE),
    CYAN("Cyan", "gui.phantoms_utilities.Cyan", "cyan", DyeColor.CYAN),
    GREEN("Green", "gui.phantoms_utilities.Green", "green", DyeColor.GREEN),
    CLEAR("Clear", "gui.phantoms_utilities.Clear", "clear", null);

    public static final Codec<PUColor> CODEC = StringRepresentable.fromEnum(PUColor::values);
    public static final StreamCodec<FriendlyByteBuf, PUColor> STREAM_CODEC = NeoForgeStreamCodecs
            .enumCodec(PUColor.class);
    public static final List<PUColor> VALID_COLORS = Arrays.asList(WHITE, LIGHT_GRAY, GRAY, BLACK, LIME, YELLOW,
            ORANGE, BROWN, RED, PINK, MAGENTA, PURPLE, BLUE, LIGHT_BLUE, CYAN, GREEN);
    public final String registryPrefix;
    public final String englishName;
    public final String translationKey;
    public final DyeColor dye;

    PUColor(String englishName, String translationKey, String registryPrefix, DyeColor dye) {
        this.englishName = englishName;
        this.translationKey = translationKey;
        this.registryPrefix = registryPrefix;
        this.dye = dye;
    }

    public static PUColor fromDye(DyeColor vanillaDye) {
        for (var value : values()) {
            if (value.dye == vanillaDye) return value;
        }
        throw new IllegalArgumentException("Unknown Vanilla dye: " + vanillaDye);
    }

    public String getEnglishName() {
        return englishName;
    }

    @Override
    public String toString() {
        return this.translationKey;
    }

    @Override
    public String getSerializedName() {
        return registryPrefix;
    }
}
