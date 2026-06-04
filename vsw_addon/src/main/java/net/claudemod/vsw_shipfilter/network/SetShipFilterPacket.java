package net.claudemod.vsw_shipfilter.network;

import net.claudemod.vsw_shipfilter.IShipFilterDuck;
import net.claudemod.vsw_shipfilter.VswShipFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the client when the player clicks the "Ship ID Filter" toggle button
 * in the RsChannel Receiver GUI.
 *
 * Payload: BlockPos of the receiver + boolean (new enabled state).
 */
public class SetShipFilterPacket {

    private final BlockPos pos;
    private final boolean enabled;

    public SetShipFilterPacket(BlockPos pos, boolean enabled) {
        this.pos     = pos;
        this.enabled = enabled;
    }

    // ── codec ─────────────────────────────────────────────────────────────────

    public static SetShipFilterPacket decode(FriendlyByteBuf buf) {
        return new SetShipFilterPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(enabled);
    }

    // ── handler ───────────────────────────────────────────────────────────────

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be    = level.getBlockEntity(pos);

            if (!(be instanceof IShipFilterDuck duck)) {
                VswShipFilter.LOGGER.warn("[VswShipFilter] SetShipFilterPacket: BE at {} is not IShipFilterDuck", pos);
                return;
            }

            // Basic permission check: player must be within 8 blocks
            if (player.blockPosition().distSqr(pos) > 64.0) {
                VswShipFilter.LOGGER.warn("[VswShipFilter] Player {} is too far from receiver at {}", player.getName().getString(), pos);
                return;
            }

            duck.vsw_filter_setShipIdFilterEnabled(enabled);
            be.setChanged();

            VswShipFilter.LOGGER.debug("[VswShipFilter] Ship-ID filter {} at {} by {}",
                    enabled ? "ENABLED" : "DISABLED", pos, player.getName().getString());
        });
        ctx.setPacketHandled(true);
    }
}
