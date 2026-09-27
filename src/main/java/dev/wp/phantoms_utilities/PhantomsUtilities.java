package dev.wp.phantoms_utilities;

import dev.wp.phantoms_utilities.config.ServerConfig;
import dev.wp.phantoms_utilities.network.InitNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(PhantomsUtilities.ID)
public class PhantomsUtilities {
    public static final String ID = "phantoms_utilities";
    public static final String NAME = "Phantoms Utilities";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public PhantomsUtilities(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC);

        bus.addListener(this::commonSetup);
        bus.addListener(InitNetwork::init);
        bus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey() == Registries.SOUND_EVENT) registerSounds();
        });

        PUItems.ITEMS.register(bus);
        PUItems.CREATIVE_MODE_TABS.register(bus);
        PUComponents.init(bus);
    }

    public static ResourceLocation id(String id) {
        return ResourceLocation.fromNamespaceAndPath(ID, id);
    }

    private void registerSounds() {
        PUSounds.register(BuiltInRegistries.SOUND_EVENT);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

}
