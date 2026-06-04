package net.claudemod.vsw_shipfilter.network;

import net.claudemod.vsw_shipfilter.VswShipFilter;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VswShipFilter.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void register() {
        // Client → Server: toggle the ship-ID filter on a receiver
        CHANNEL.registerMessage(id++,
                SetShipFilterPacket.class,
                SetShipFilterPacket::encode,
                SetShipFilterPacket::decode,
                SetShipFilterPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        // Server → Client: sync filter state when opening the GUI
        CHANNEL.registerMessage(id++,
                SyncShipFilterPacket.class,
                SyncShipFilterPacket::encode,
                SyncShipFilterPacket::decode,
                SyncShipFilterPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }
}
