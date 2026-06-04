package net.claudemod.vsw_shipfilter.mixin;

import net.claudemod.vsw_shipfilter.IShipFilterDuck;
import net.minecraft.nbt.CompoundTag;
import net.shao.valkyrien_space_war.block.rschannel.RsChannelReceiverBe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into {@link RsChannelReceiverBe} to:
 * <ol>
 *   <li>Add a boolean NBT field {@code vsw_shipIdFilter} that persists the toggle.</li>
 *   <li>Implement {@link IShipFilterDuck} so the manager mixin can read the flag.</li>
 * </ol>
 *
 * The actual filtering logic lives in {@link MixinRsChannelManager}.
 */
@Mixin(value = RsChannelReceiverBe.class, remap = false)
public abstract class MixinRsChannelReceiverBe implements IShipFilterDuck {

    // ── injected fields ──────────────────────────────────────────────────────

    /** Whether the Ship-ID filter is active for this receiver. */
    @Unique
    private boolean vsw_filter$shipIdFilterEnabled = false;

    /**
     * The VS2 ship-id of the ship this receiver currently belongs to.
     * {@code null} means "not on a ship" (placed in the overworld).
     * Populated by the server-side tick logic in MixinRsChannelManager.
     */
    @Unique
    private Long vsw_filter$cachedShipId = null;

    // ── NBT persistence ───────────────────────────────────────────────────────

    /**
     * Hooks into {@code saveAdditional} (or {@code save}) to write our extra flag.
     * The original mod stores its data via {@code putRsChannelToTag}, which is called
     * from the vanilla {@code saveAdditional} path; we piggyback on the same tag.
     */
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void vsw_filter$onSave(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("vsw_shipIdFilter", vsw_filter$shipIdFilterEnabled);
    }

    /**
     * Hooks into {@code load} to read our extra flag back from NBT.
     */
    @Inject(method = "load", at = @At("TAIL"))
    private void vsw_filter$onLoad(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("vsw_shipIdFilter")) {
            vsw_filter$shipIdFilterEnabled = tag.getBoolean("vsw_shipIdFilter");
        }
    }

    // ── IShipFilterDuck ───────────────────────────────────────────────────────

    @Override
    public boolean vsw_filter_isShipIdFilterEnabled() {
        return vsw_filter$shipIdFilterEnabled;
    }

    @Override
    public void vsw_filter_setShipIdFilterEnabled(boolean enabled) {
        vsw_filter$shipIdFilterEnabled = enabled;
    }

    @Override
    public Long vsw_filter_getCachedShipId() {
        return vsw_filter$cachedShipId;
    }

    @Override
    public void vsw_filter_setCachedShipId(Long shipId) {
        vsw_filter$cachedShipId = shipId;
    }
}
