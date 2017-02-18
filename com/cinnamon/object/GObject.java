package com.cinnamon.object;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.Texture;
import com.cinnamon.system.ComponentFactory;
import com.cinnamon.system.MouseEvent;
import com.cinnamon.utils.Identifiable;
import com.cinnamon.utils.OnClickListener;

/**
 * <p>
 *     GObjects are the generalization of all game objects. A GObject is
 *     represented in-game as the combination of its set
 *     {@link ComponentFactory.Component}s like its visuals
 *     ({@link ImageComponent}) or collision model {@link BodyComponent}.
 * </p>
 *
 * <p>
 *     When handling specific GObject instances, it is recommended to refer
 *     to the instance by the combination of its id and version, retrieved by
 *     {@link #getId()} and {@link #getVersion()}, respectively. The id is
 *     distinct and may be used to identify an instance amongst many. However,
 *     the version is needed to differentiate between an instance referenced
 *     as a game object but later destroyed and reused as another. Since a
 *     GObject's id is retained across life cycles, in these cases where
 *     recycling occurs, the version refers to a GObject instance as it
 *     was within a period of time.
 * </p>
 *
 *
 */
public class GObject implements Identifiable, Positional
{
    // Callback for on click events
    private OnClickListener mOnClickListener;

    // Collision
    private BodyComponent mBodyComp;

    // Visual
    private ImageComponent mImgComp;

    // Offset from position
    private float mOffX = 0f;
    private float mOffY = 0f;
    private float mOffZ = 0f;

    // Instance id
    private int mId;
    private int mVersion;

    /**
     * <p>Constructs an empty game object.</p>
     */
    public GObject()
    {
    }

    /**
     * <p>Gets the unique id.</p>
     *
     * @return id.
     */
    @Override
    public final int getId()
    {
        return mId;
    }

    /**
     * <p>Sets the unique id.</p>
     *
     * @param id id.
     */
    final void setId(int id)
    {
        mId = id;
    }

    /**
     * <p>Gets the version number.</p>
     *
     * @return version.
     */
    public final int getVersion()
    {
        return mVersion;
    }

    /**
     * <p>Sets the version number.</p>
     * @param version version.
     */
    final void setVersion(int version)
    {
        mVersion = version;
    }

    /**
     * <p>Gets the x coordinate.</p>
     *
     * <p>The x is the same as the GObject's {@link BodyComponent}'s x
     * position. If there is no BodyComponent, the returned value will be x
     * of the {@link ImageComponent}. If neither are available, this method
     * returns 0.</p>
     *
     * @return x.
     */
    @Override
    public float getX()
    {
        if (mBodyComp != null) {
            return mBodyComp.getX();
        } else if (mImgComp != null) {
            return mImgComp.getX();
        }
        return 0;
    }

    /**
     * <p>Gets the y coordinate.</p>
     *
     * <p>The y is the same as the GObject's {@link BodyComponent}'s y
     * position. If there is no BodyComponent, the returned value will be y
     * of the {@link ImageComponent}. If neither are available, this method
     * returns 0.</p>
     *
     * @return y.
     */
    @Override
    public float getY()
    {
        if (mBodyComp != null) {
            return mBodyComp.getY();
        } else if (mImgComp != null) {
            return mImgComp.getY();
        }
        return 0;
    }

    /**
     * <p>Gets the z coordinate.</p>
     *
     * <p>The z is the same as the GObject's {@link BodyComponent}'s z
     * position. If there is no BodyComponent, the returned value will be z
     * of the {@link ImageComponent}. If neither are available, this method
     * returns 0.</p>
     *
     * @return z.
     */
    @Override
    public float getZ()
    {
        if (mBodyComp != null) {
            return mBodyComp.getZ();
        } else if (mImgComp != null) {
            return mImgComp.getZ();
        }
        return 0;
    }

    /**
     * <p>Moves the GObject's components to the given (x,y) position.</p>
     *
     * @param x x.
     * @param y y;
     */
    @Override
    public void moveTo(float x, float y)
    {
        moveTo(x, y, getZ());
    }

    /**
     * <p>Moves the GObject's components to the given (x,y,z) position.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    public final void moveTo(float x, float y, float z)
    {
        // Move BodyComponent to match pos
        if (mBodyComp != null) {
            mBodyComp.moveTo(x, y, z);
        }

        // Move ImageComponent to match pos
        if (mImgComp != null) {
            mImgComp.moveTo(x, y, z);
        }
    }

    @Override
    public void setOffset(float x, float y)
    {
        setOffset(x, y, mOffZ);
    }

    @Override
    public void setOffset(float x, float y, float z)
    {
        mOffX = x;
        mOffY = y;
        mOffZ = z;
    }

    @Override
    public float getOffsetX()
    {
        return mOffX;
    }

    @Override
    public float getOffsetY()
    {
        return mOffY;
    }

    @Override
    public float getOffsetZ()
    {
        return mOffZ;
    }

    /**
     * <p>Gets the width coordinate.</p>
     *
     * <p>The width is the same as the GObject's {@link BodyComponent}'s
     * width. If there is no BodyComponent, the returned value will be
     * the width of the {@link ImageComponent}. If neither are available,
     * this method returns 0.</p>
     *
     * @return width.
     */
    public float getWidth()
    {
        if (mBodyComp != null) {
            return mBodyComp.getWidth();
        } else if (mImgComp != null) {
            return mImgComp.getWidth();
        }
        return 0;
    }

    /**
     * <p>Gets the height coordinate.</p>
     *
     * <p>The height is the same as the GObject's {@link BodyComponent}'s
     * height. If there is no BodyComponent, the returned value will be
     * the height of the {@link ImageComponent}. If neither are available,
     * this method returns 0.</p>
     *
     * @return height.
     */
    public float getHeight()
    {
        if (mBodyComp != null) {
            return mBodyComp.getHeight();
        } else if (mImgComp != null) {
            return mImgComp.getHeight();
        }
        return 0;
    }

    /**
     * <p>Gets the {@link ImageComponent} to use when drawing.</p>
     *
     * @return ImageComponent.
     */
    public final ImageComponent getImageComponent()
    {
        return mImgComp;
    }

    /**
     * <p>Sets the {@link ImageComponent} to use when drawing.</p>
     *
     * <p>Any previously set ImageComponent will lose its GObject id and
     * version, resetting to {@link ComponentFactory.Component#NULL}. The newly
     * set component adopts the id and version.
     *
     * @param component ImageComponent.
     */
    public final void setImageComponent(ImageComponent component)
    {
        // Don't change anything since same component
        if (mImgComp == component) {
            return;
        }

        // Disown previous component
        if (mImgComp != null) {
            mImgComp.setGObject(null);
        }

        // Apply new component's ownership
        final ImageComponent oldImg = mImgComp;
        mImgComp = component;
        if (mImgComp != null) {

            mImgComp.setGObject(this);
            syncImageToBody();
        }
    }

    /**
     * <p>Gets the {@link BodyComponent} to use for collision operations.</p>
     *
     * @return BodyComponent.
     */
    public final BodyComponent getBodyComponent()
    {
        return mBodyComp;
    }

    /**
     * <p>Sets the {@link BodyComponent} to use for collision operations.</p>
     *
     * <p>Any previously set BodyComponent will lose its GObject id and
     * version, resetting to {@link ComponentFactory.Component#NULL}. The
     * newly set component adopts the id and version.
     *
     * @param component BodyComponent.
     */
    public final void setBodyComponent(BodyComponent component)
    {
        // Same component so no changes to do
        if (mBodyComp == component) {
            return;
        }

        // Disown previous component
        if (mBodyComp != null) {
            mBodyComp.setGObject(null);
        }

        mBodyComp = component;
        if (mBodyComp != null) {

            // Apply component ownership
            mBodyComp.setGObject(this);

            syncImageToBody();
        }
    }

    /**
     * <p>Moves the {@link ImageComponent} to match positions with the
     * {@link BodyComponent}.</p>
     *
     * <p>If either components have not been set, this method does nothing.</p>
     */
    private void syncImageToBody()
    {
        // Can't sync if no img
        if (mImgComp == null || mBodyComp == null) {
            return;
        }

        // Synchronize ImageComponent's position to body
        mImgComp.moveTo(mBodyComp.getX(), mBodyComp.getY());
    }

    /**
     * <p>Checks if a given point is contained within the {@link BodyComponent}.</p>
     *
     * <p>If there is no set BodyComponent, this method returns false.</p>
     *
     * @param x x.
     * @param y y.
     * @return true if the point is contained.
     */
    public boolean contains(float x, float y)
    {
        return (mBodyComp != null && mBodyComp.contains(x, y));
    }

    /**
     * <p>Notifies a listener attached via
     * {@link #setOnClickListener(OnClickListener)} that the GObject was
     * clicked on and returns whether or not the given {@link MouseEvent} was
     * consumed.</p>
     *
     * <p>This method returns false unless a listener is notified. In that
     * case, the returned value is deferred to the listener and its
     * implementation.</p>
     *
     * @param event MouseEvent.
     * @return true if the MouseEvent was used.
     */
    public boolean click(MouseEvent event)
    {
        return (mOnClickListener != null && mOnClickListener.onClick(event));
    }

    /**
     * <p>Sets the {@link OnClickListener} to be notified of
     * {@link MouseEvent}s used during calls to {@link #click(MouseEvent)}.</p>
     *
     * @param listener OnClickListener.
     */
    public final void setOnClickListener(OnClickListener listener)
    {
        mOnClickListener = listener;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Copy operations must go through" +
                " GObjectFactory");
    }

    @Override
    public String toString()
    {
        final int texture;
        if (mImgComp == null) {
            texture = Texture.NULL;
        } else {
            texture = mImgComp.getTexture();
        }

        final Positional anchor;
        anchor = (mImgComp == null) ? mBodyComp : mImgComp;

        String position = "(NULL)";
        if (anchor != null) {
            position = "(" + anchor.getX() + "," + anchor.getY() + "," +
                    anchor.getZ() + ")";
        }

        return "[id(" + mId + ")," + "locat" + position + ",tex(" +
            texture + ")," + ((mBodyComp == null) ? "no collision" :
                mBodyComp.getShape()) + "]";
    }
}
