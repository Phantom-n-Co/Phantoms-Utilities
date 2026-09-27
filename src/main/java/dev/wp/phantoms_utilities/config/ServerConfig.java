package dev.wp.phantoms_utilities.config;

import dev.wp.phantoms_utilities.PhantomsUtilities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

@EventBusSubscriber(modid = PhantomsUtilities.ID)
public class ServerConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.Builder BUILDER;
    private static final ModConfigSpec.IntValue MAX_CABLE_DYE_COUNT;
    private static final ModConfigSpec.IntValue MAX_BLOCK_DYE_COUNT;
    private static final ModConfigSpec.IntValue MAX_TOTAL_CHECKS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_MODS;
    public static int maxCableDyeCount;
    public static int maxBlockDyeCount;
    public static int maxTotalChecks;
    public static List<? extends String> blacklistedMods;

    static {
        BUILDER = new ModConfigSpec.Builder();

        BUILDER.push("items").push("spray_can");
        MAX_CABLE_DYE_COUNT = BUILDER
                .comment("Maximum amount of ae2 cables that can be re-dyed using the spray can at once.")
                .translation(configKey("spray_can.max_cable_dye_count"))
                .defineInRange("maxCableDyeCount", 64, 0, Integer.MAX_VALUE);
        MAX_BLOCK_DYE_COUNT = BUILDER
                .comment("Maximum amount of blocks that can be dyed using the spray can at once.")
                .translation(configKey("spray_can.max_block_dye_count"))
                .defineInRange("maxBlockDyeCount", 1024, 0, Integer.MAX_VALUE);
        MAX_TOTAL_CHECKS = BUILDER
                .comment("Maximum amount of blocks/cables that can be checked for before it stops checking," +
                        "different from the maxBlockDyeCount and maxCableCount as this is the total amount of blocks/cables that can be checked for.")
                .translation(configKey("spray_can.max_total_checks"))
                .defineInRange("maxTotalChecks", 8192, 0, Integer.MAX_VALUE);
        BLACKLISTED_MODS = BUILDER
                .comment("Mods to blacklist from the spray can")
                .translation(configKey("spray_can.mod_blacklist"))
                .defineList("modBlacklist", List.of("spectrum", "pastel", "xycraft_world"), (o) -> true);

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) return;
        maxCableDyeCount = MAX_CABLE_DYE_COUNT.get();
        maxBlockDyeCount = MAX_BLOCK_DYE_COUNT.get();
        maxTotalChecks = MAX_TOTAL_CHECKS.get();
        blacklistedMods = BLACKLISTED_MODS.get();
    }

    private static String configKey(String key) {
        return "phantoms_utilities.configuration." + key;
    }
}
