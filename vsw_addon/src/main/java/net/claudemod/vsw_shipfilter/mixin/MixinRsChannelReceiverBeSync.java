package net.claudemod.vsw_shipfilter.mixin;

import net.claudemod.vsw_shipfilter.IShipFilterDuck;
import net.claudemod.vsw_shipfilter.network.ModNetwork;
import net.claudemod.vsw_shipfilter.network.SyncShipFilterPacket;
import net.minecraft.server.level.ServerPlayer;
import net.shao.valkyrien_space_war.block.rschannel.RsChannelReceiverBe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into {@code syncRsChannelNames} (the method called when a player opens
 * the receiver GUI) to also send a {@link SyncShipFilterPacket} with the current
 * ship-filter state, so the client button reflects the correct value immediately.
 */
@Mixin(value = RsChannelReceiverBe.class, remap = false)
public abstract class MixinRsChannelReceiverBeSync {

    /**
     * After the original sync packet is sent, also send our filter-state packet.
     * The method that triggers when a player opens the GUI is {@code syncRsChannelNames}.
     */
    @Inject(method = "syncRsChannelNames", at = @At("TAIL"))
    private void vsw_filter$onSync(ServerPlayer player, CallbackInfo ci) {
        if (!(this instanceof IShipFilterDuck duck)) return;
        if (!(this instanceof RsChannelReceiverBe be)) return;

        boolean enabled = duck.vsw_filter_isShipIdFilterEnabled();
        ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new SyncShipFilterPacket(be.getBlockPos(), enabled)
        );
    }
}
