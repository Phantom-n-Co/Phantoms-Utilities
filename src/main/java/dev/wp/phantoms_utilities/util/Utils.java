package dev.wp.phantoms_utilities.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Utils {
    public final static boolean isAE2Loaded = isModLoaded("ae2");
    public final static boolean isMILoaded = isModLoaded("modern_industrialization");

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean mayBreakBlock(Level level, BlockPos pos, BlockState state, Player player) {
        return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled();
    }

    public static ResourceLocation getRecoloredBlockID(ResourceLocation originalId, PUColor color) {
        if (color == PUColor.CLEAR) return getClearedBlockID(originalId);

        String namespace = originalId.getNamespace();
        String path = originalId.getPath();

        Set<String> validColors = PUColor.VALID_COLORS.stream().map(validColor -> validColor.registryPrefix).collect(Collectors.toSet());

        String colorPattern = validColors.stream().sorted((a, b) -> Integer.compare(b.length(), a.length())).collect(Collectors.joining("|"));

        Pattern pattern = Pattern.compile("(?<=^|_)(" + colorPattern + ")(?=_|$)");
        Matcher matcher = pattern.matcher(path);

        if (matcher.find()) {
            String updatedPath = matcher.replaceFirst(color.registryPrefix);

            return ResourceLocation.fromNamespaceAndPath(namespace, updatedPath);
        }

        return originalId;
    }

    public static ResourceLocation getClearedBlockID(ResourceLocation originalId) {
        String namespace = originalId.getNamespace();
        String path = originalId.getPath();

        Set<String> validColors = PUColor.VALID_COLORS.stream().map(validColor -> validColor.registryPrefix).collect(Collectors.toSet());
        String colorPattern = validColors.stream().sorted((a, b) -> Integer.compare(b.length(), a.length())).collect(Collectors.joining("|"));

        // Match color prefix or suffix with an optional underscore, and optional "stained"
        // Pattern matches:
        // 1. color_stained_
        // 2. color_
        // 3. _color
        String regex = "((" + colorPattern + ")_stained_)|((" + colorPattern + ")_)|(_(" + colorPattern + "))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(path);

        if (matcher.find()) {
            String updatedPath = matcher.replaceAll("");
            return ResourceLocation.fromNamespaceAndPath(namespace, updatedPath);
        }

        return originalId;
    }

    public static <T extends Comparable<T>> BlockState copyProperties(BlockState oldState, BlockState newState, Property<T> property) {
        if (newState.hasProperty(property)) return newState.setValue(property, oldState.getValue(property));
        return newState;
    }
}
