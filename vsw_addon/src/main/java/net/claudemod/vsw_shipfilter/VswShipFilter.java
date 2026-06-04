package net.claudemod.vsw_shipfilter;

import net.claudemod.vsw_shipfilter.network.ModNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VswShipFilter.MOD_ID)
public class VswShipFilter {

    public static final String MOD_ID = "vsw_shipfilter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VswShipFilter() {
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener(this::commonSetup);
        LOGGER.info("[VswShipFilter] VSW Ship ID Filter initializing...");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        LOGGER.info("[VswShipFilter] Network channels registered.");
    }
}
