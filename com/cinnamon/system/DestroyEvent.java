package com.cinnamon.system;

import com.cinnamon.object.GObject;

/**
 * <p>
 *     DestroyEvents represent the destruction of specified {@link GObject}s.
 * </p>
 */
public class DestroyEvent extends Event
{
    // Target GObject id
    private int mTarId;

    // Target GObject version
    private int mTarVer;

    /**
     * <p>Constructs a DestroyEvent with the target {@link GObject}'s id and version.</p>
     *
     * @param id GObject id.
     * @param version GObject version.
     */
    public DestroyEvent(int id, int version)
    {
        mTarId = id;
        mTarVer = version;
    }

    /**
     * <p>Gets the id of the {@link GObject} to destroy.</p>
     *
     * @return GObject id.
     */
    public final int getTargetId()
    {
        return mTarId;
    }

    /**
     * <p>Gets the version of the {@link GObject} to destroy.</p>
     *
     * @return GObject version.
     */
    public final int getTargetVersion()
    {
        return mTarVer;
    }

    @Override
    protected void handle(EventDispatcher distributor)
    {
        distributor.process(this);
    }
}
