package dev.wp.phantoms_utilities.datagen.providers.client;

import dev.wp.phantoms_utilities.PhantomsUtilities;
import dev.wp.phantoms_utilities.util.PUColor;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.minecraft.world.item.ItemDisplayContext;

public final class PUItemModels extends ItemModelProvider {
    public PUItemModels(GatherDataEvent event) {
        super(event.getGenerator().getPackOutput(), PhantomsUtilities.ID, event.getExistingFileHelper());
    }

    @Override
    protected void registerModels() {
        ModelFile parent = sprayCanBase();
        ItemModelBuilder sprayCan = getBuilder("spray_can").parent(parent)
                .texture("layer0", texture(PUColor.WHITE));
        for (PUColor color : PUColor.values()) {
            ItemModelBuilder variant = getBuilder("spray_can_" + color.getName()).parent(parent)
                    .texture("layer0", texture(color));
            sprayCan.override().predicate(PhantomsUtilities.id("color"), color.ordinal()).model(variant).end();
        }
    }

    private ModelFile sprayCanBase() {
        return getBuilder("spray_can_base").parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(0, -90, THIRD_PERSON_TILT).translation(0, 4, 0.5f).scale(0.85f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .rotation(0, 90, -THIRD_PERSON_TILT).translation(0, 4, 0.5f).scale(0.85f).end()
                .end();
    }

    private static final float THIRD_PERSON_TILT = 15;

    private ResourceLocation texture(PUColor color) {
        return modLoc("item/spray_can/spray_can_" + color.getName());
    }
}
