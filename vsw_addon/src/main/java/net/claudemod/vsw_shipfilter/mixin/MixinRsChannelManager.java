package net.claudemod.vsw_shipfilter.mixin;

import net.claudemod.vsw_shipfilter.IShipFilterDuck;
import net.claudemod.vsw_shipfilter.VswShipFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shao.valkyrien_space_war.function.rs_channel.IRsChannelProvider;
import net.shao.valkyrien_space_war.function.rs_channel.IRsChannelReceiver;
import net.shao.valkyrien_space_war.function.rs_channel.RsChannelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ServerShip;

import java.util.Map;

/**
 * Injects into {@link RsChannelManager#sendRsChannel} to add a third filter:
 * the VS2 Ship ID.
 *
 * <h3>How the original system works</h3>
 * {@code sendRsChannel} is called by the {@code RsChannelSenderBe} (and the Control Seat).
 * It iterates over every registered {@link IRsChannelReceiver}, and for each one it
 * calls {@link IRsChannelReceiver#receiveRsChannel} if:
 * <ol>
 *   <li>The receiver's {@code channelName} matches the sender's channel name.</li>
 *   <li>The integer "filter" value matches (this is the second parameter of the pair).</li>
 * </ol>
 *
 * <h3>What we add</h3>
 * When a receiver has {@link IShipFilterDuck#vsw_filter_isShipIdFilterEnabled()} == true,
 * we also require that the sender and receiver are on the SAME VS2 ship
 * (i.e. {@code provider.getShipId() == receiver.getShipId()}).
 *
 * We determine the ship IDs from the {@code newShips} map that the original method
 * already receives — each block entity's {@link BlockPos} is tested against the map
 * using VS2's {@link ServerShip#getShipAABB()} bounding boxes (passed in {@code newShipAABBs}).
 */
@Mixin(value = RsChannelManager.class, remap = false)
public abstract class MixinRsChannelManager {

    /**
     * Shadow the static {@code ALL_RECEIVERS} set so we can iterate inside the inject.
     * (Not strictly needed here because we inject around the iteration, but useful
     *  for future reference.)
     */
    @Shadow
    private static java.util.HashSet<IRsChannelReceiver> ALL_RECEIVERS;

    /**
     * Injects at the START of {@code sendRsChannel} so we can attach ship-IDs to every
     * receiver that has the filter enabled, before the original loop runs.
     *
     * Then injects a REDIRECT on the call to {@code IRsChannelReceiver.receiveRsChannel}
     * inside that loop, so we can gate it on the ship-ID check.
     *
     * Strategy: use @Inject at HEAD to populate the cached ship-IDs on all
     * registered receivers, then the redirect below will use those cached values.
     */
    @Inject(
        method = "sendRsChannel",
        at = @At("HEAD"),
        remap = false
    )
    private static void vsw_filter$onSendRsChannelHead(
            BlockEntity senderBe,
            ServerLevel level,
            Map<Long, ServerShip> newShips,
            Map<Long, AABB> newShipAABBs,
            BlockPos senderPos,
            BlockState senderState,
            CompoundTag senderTag,
            CallbackInfo ci) {

        if (ALL_RECEIVERS == null) return;

        // ── 1. Find the ship ID of the sender ────────────────────────────────
        Long senderShipId = vsw_filter$findShipId(senderPos, level, newShips, newShipAABBs);

        // ── 2. Cache the ship ID on every receiver that has the filter ON ────
        for (IRsChannelReceiver receiver : ALL_RECEIVERS) {
            if (!(receiver instanceof IShipFilterDuck duck)) continue;
            if (!duck.vsw_filter_isShipIdFilterEnabled()) continue;

            // The receiver is a BlockEntity — get its position
            if (!(receiver instanceof BlockEntity receiverBe)) continue;

            BlockPos receiverPos = receiverBe.getBlockPos();
            Long receiverShipId = vsw_filter$findShipId(receiverPos, level, newShips, newShipAABBs);
            duck.vsw_filter_setCachedShipId(receiverShipId);

            // Store the sender ship id in a thread-local so the redirect below can see it
            // We can't easily pass it, so we use a package-private static helper field.
            // (see vsw_filter$currentSenderShipId below)
        }

        // Store sender ship ID for use in the redirect
        vsw_filter$currentSenderShipId = senderShipId;
    }

    /**
     * Thread-local (effectively: single-threaded server tick) holder for the
     * sender's ship ID, so the redirect injected into the loop body can access it.
     */
    @org.spongepowered.asm.mixin.Unique
    private static Long vsw_filter$currentSenderShipId = null;

    /**
     * Redirects the call to {@code receiver.receiveRsChannel(channelName)} inside
     * the {@code sendRsChannel} loop.
     *
     * If the receiver has the ship-ID filter ON, we only forward the call when
     * sender and receiver are on the same ship.
     */
    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "sendRsChannel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/shao/valkyrien_space_war/function/rs_channel/IRsChannelReceiver;receiveRsChannel(Ljava/lang/String;I)V"
        ),
        remap = false
    )
    private static void vsw_filter$redirectReceiveRsChannel(
            IRsChannelReceiver receiver,
            String channelName,
            int filterValue) {

        // If this receiver implements our duck interface and has the filter ON,
        // check that sender and receiver are on the same ship.
        if (receiver instanceof IShipFilterDuck duck && duck.vsw_filter_isShipIdFilterEnabled()) {
            Long receiverShipId = duck.vsw_filter_getCachedShipId();
            Long senderShipId   = vsw_filter$currentSenderShipId;

            boolean sameShip = (receiverShipId != null)
                    && (senderShipId != null)
                    && receiverShipId.equals(senderShipId);

            if (!sameShip) {
                // Different ships (or one of them is in the overworld) → block the signal
                return;
            }
        }

        // Either filter is OFF, or ship IDs match → pass through to original method
        receiver.receiveRsChannel(channelName, filterValue);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the VS2 Ship ID for a block at the given world position,
     * or {@code null} if the block is not on any ship.
     *
     * We use the {@code newShipAABBs} map (world-space AABB per ship)
     * that the original {@code sendRsChannel} already has, to avoid
     * importing VS2 game-utils mixins.
     */
    @org.spongepowered.asm.mixin.Unique
    private static Long vsw_filter$findShipId(
            BlockPos pos,
            ServerLevel level,
            Map<Long, ServerShip> newShips,
            Map<Long, AABB> newShipAABBs) {

        if (newShips == null || newShipAABBs == null) return null;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        for (Map.Entry<Long, AABB> entry : newShipAABBs.entrySet()) {
            if (entry.getValue().contains(x, y, z)) {
                return entry.getKey();
            }
        }
        return null; // in the overworld / unloaded ship
    }
}
