package net.claudemod.vsw_shipfilter.mixin;

import net.claudemod.vsw_shipfilter.IShipFilterDuck;
import net.claudemod.vsw_shipfilter.network.ModNetwork;
import net.claudemod.vsw_shipfilter.network.SetShipFilterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shao.valkyrien_space_war.block.rschannel.RsChannelReceiverBe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects a "Ship ID Filter" toggle button into the RsChannel Receiver screen.
 *
 * The button appears below the existing channel name / filter boxes.
 * When toggled ON:  label turns green  → "[Ship ID: ON ]"
 * When toggled OFF: label turns white  → "[Ship ID: OFF]"
 *
 * Clicking the button sends a {@link SetShipFilterPacket} to the server.
 */
@Mixin(targets = "net.shao.valkyrien_space_war.block.rschannel.RsChannelReceiverBe$Screen",
        remap = false)
public abstract class MixinRsChannelReceiverScreen
        extends AbstractContainerScreen<RsChannelReceiverBe.Menu> {

    // ── shadows ───────────────────────────────────────────────────────────────

    // The inner class holds a reference to its enclosing BlockEntity in a field
    // called "this$0" by the compiler; we shadow it here.
    @Shadow(aliases = {"this$0"})
    @Final
    private RsChannelReceiverBe vsw_filter$be;

    // ── injected state ────────────────────────────────────────────────────────

    @Unique
    private Button vsw_filter$shipFilterButton;

    // ── constructor redirect ───────────────────────────────────────────────────

    protected MixinRsChannelReceiverScreen(RsChannelReceiverBe.Menu menu,
                                           Inventory inv,
                                           Component title) {
        super(menu, inv, title);
    }

    // ── init inject ───────────────────────────────────────────────────────────

    /**
     * After the original {@code init()} has added its widgets, we append our button.
     * Position: just below the GUI background, centred horizontally.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void vsw_filter$onInit(CallbackInfo ci) {
        // Current filter state from the duck interface (populated by SyncShipFilterPacket)
        boolean currentState = (vsw_filter$be instanceof IShipFilterDuck duck)
                && duck.vsw_filter_isShipIdFilterEnabled();

        int btnX = leftPos + imageWidth / 2 - 60;
        int btnY = topPos + imageHeight - 20; // just below the GUI texture

        vsw_filter$shipFilterButton = addRenderableWidget(
                Button.builder(vsw_filter$makeLabel(currentState), btn -> vsw_filter$onToggle(btn))
                        .bounds(btnX, btnY, 120, 16)
                        .build()
        );
    }

    // ── render inject ─────────────────────────────────────────────────────────

    /**
     * Keep the button label in sync every frame (in case the packet arrives late).
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void vsw_filter$onRender(GuiGraphics g, int mx, int my, float pt, CallbackInfo ci) {
        if (vsw_filter$shipFilterButton == null) return;
        boolean on = (vsw_filter$be instanceof IShipFilterDuck duck)
                && duck.vsw_filter_isShipIdFilterEnabled();
        vsw_filter$shipFilterButton.setMessage(vsw_filter$makeLabel(on));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @Unique
    private void vsw_filter$onToggle(Button btn) {
        if (!(vsw_filter$be instanceof IShipFilterDuck duck)) return;

        boolean newState = !duck.vsw_filter_isShipIdFilterEnabled();
        duck.vsw_filter_setShipIdFilterEnabled(newState); // optimistic client update

        BlockPos pos = vsw_filter$be.getBlockPos();
        ModNetwork.CHANNEL.sendToServer(new SetShipFilterPacket(pos, newState));

        btn.setMessage(vsw_filter$makeLabel(newState));
    }

    @Unique
    private Component vsw_filter$makeLabel(boolean on) {
        if (on) {
            // §a = green
            return Component.literal("§a[Ship ID Filter: ON ]");
        } else {
            return Component.literal("§f[Ship ID Filter: OFF]");
        }
    }
}
