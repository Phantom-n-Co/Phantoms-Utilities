package dev.wp.phantoms_utilities.datagen.providers;

import dev.wp.phantoms_utilities.datagen.providers.client.PUItemModels;
import dev.wp.phantoms_utilities.datagen.providers.client.PULanguageProvider;
import net.minecraft.data.DataProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.function.Function;

public final class DatagenDelegatorClient {
    public static void configure(GatherDataEvent event) {
        add(event, PULanguageProvider::new);
        add(event, PUItemModels::new);
    }

    private static void add(GatherDataEvent event, Function<GatherDataEvent, DataProvider> providerCreator) {
        event.getGenerator().addProvider(event.includeClient(), providerCreator.apply(event));
    }
}
