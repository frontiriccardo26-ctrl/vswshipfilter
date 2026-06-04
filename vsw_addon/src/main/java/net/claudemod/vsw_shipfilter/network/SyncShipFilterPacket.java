package net.claudemod.vsw_shipfilter.network;

import net.claudemod.vsw_shipfilter.IShipFilterDuck;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the server when the player opens the RsChannel Receiver GUI,
 * so the client knows the current state of the ship-ID filter toggle.
 */
public class SyncShipFilterPacket {

    private final BlockPos pos;
    private final boolean enabled;

    public SyncShipFilterPacket(BlockPos pos, boolean enabled) {
        this.pos     = pos;
        this.enabled = enabled;
    }

    public static SyncShipFilterPacket decode(FriendlyByteBuf buf) {
        return new SyncShipFilterPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(enabled);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(this::handleClient);
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be instanceof IShipFilterDuck duck) {
            duck.vsw_filter_setShipIdFilterEnabled(enabled);
        }
    }
}
