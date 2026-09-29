package dev.wp.phantoms_utilities.datagen.providers.client;

import dev.wp.phantoms_utilities.PUItems;
import dev.wp.phantoms_utilities.PUTooltips;
import dev.wp.phantoms_utilities.PhantomsUtilities;
import dev.wp.phantoms_utilities.util.PUColor;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class PULanguageProvider extends LanguageProvider {
    public PULanguageProvider(GatherDataEvent event) {
        super(event.getGenerator().getPackOutput(), PhantomsUtilities.ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        for (PUColor color : PUColor.values()) {
            add(color.toString(), color.getEnglishName());
        }

        add(PUItems.SPRAY_CAN.get(), "Spray Can");
        add("phantoms_utilities.configuration.items", "Item configs");
        add("phantoms_utilities.configuration.spray_can", "Spray Can");
        add("phantoms_utilities.configuration.spray_can.max_cable_dye_count", "Max Cable Dye Count");
        add("phantoms_utilities.configuration.spray_can.max_block_dye_count", "Max Block Dye Count");
        add("phantoms_utilities.configuration.spray_can.max_total_checks", "Max Total Checks");
        add("phantoms_utilities.configuration.spray_can.mod_blacklist", "Blacklisted Mods");
        add("gui.phantoms_utilities.spray_can_color.title", "Select Color");

        add(PUTooltips.SHIFT_REQUIRED, "Press [Shift] for info");
        add(PUTooltips.SPRAY_CAN_PAINT, "- %s: paint a block, cable, pipe or sheep");
        add(PUTooltips.SPRAY_CAN_PAINT_CONNECTED, "- %s + %s: paint all connected");
        add(PUTooltips.SPRAY_CAN_CYCLE, "- %s + Scroll: cycle colors");
        add(PUTooltips.SPRAY_CAN_PICK, "- %s: copy color from a block");
        add(PUTooltips.SPRAY_CAN_PICKER, "- %s + %s: open color picker");
        add(PUTooltips.SPRAY_CAN_OFFHAND, "- In offhand: paints placed blocks, strips color from broken ones");
    }
}
