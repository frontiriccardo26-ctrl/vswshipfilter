package net.claudemod.vsw_shipfilter;

/**
 * Duck interface injected into RsChannelReceiverBe via Mixin.
 *
 * Allows other code to read / write the "shipIdFilter" flag
 * and the cached "ownerShipId" on the block entity without
 * reflection or casting pain.
 */
public interface IShipFilterDuck {

    /**
     * Returns true when the receiver should only accept signals
     * from a Sender that sits on the same VS2 ship.
     */
    boolean vsw_filter_isShipIdFilterEnabled();

    /**
     * Enable or disable the Ship-ID filter.
     */
    void vsw_filter_setShipIdFilterEnabled(boolean enabled);

    /**
     * The VS2 ship-ID of the ship this receiver belongs to,
     * or {@code null} if the block is in the overworld / not on a ship.
     * Updated lazily on the server every tick.
     */
    Long vsw_filter_getCachedShipId();

    /**
     * Called by the manager mixin to update the cached ship ID.
     */
    void vsw_filter_setCachedShipId(Long shipId);
}
