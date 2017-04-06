package com.cinnamon.object;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.Texture;
import com.cinnamon.system.ComponentFactory;
import com.cinnamon.system.IndexedFactory;
import com.cinnamon.system.MouseEvent;
import com.cinnamon.utils.OnClickListener;
import com.cinnamon.utils.Point3F;
import com.cinnamon.utils.Rotatable;

/**
 * <p>
 *     GObjects are the generalization of all game objects. A GObject is represented in-game as the combination of
 *     its set {@link ComponentFactory.Component}s like its visuals ({@link ImageComponent}) or collision model
 *     {@link BodyComponent}.
 * </p>
 *
 * <p>
 *     When handling specific GObject instances, it is recommended to refer to the instance by the combination of its
 *     id and version, retrieved by {@link #getId()} and {@link #getVersion()}, respectively. The id is distinct and
 *     may be used to identify an instance amongst many. However, the version is needed to differentiate between an
 *     instance referenced as a game object but later destroyed and reused as another. Since a GObject's id is
 *     retained across life cycles, in these cases where recycling occurs, the version refers to a GObject instance
 *     as it was within its relevant period of time.
 * </p>
 */
public class GObject extends IndexedFactory.Identifiable implements Positional, Rotatable
{
    // Callback for on click events
    private OnClickListener mOnClickListener;

    // Collision
    private BodyComponent mBodyComp;

    // Visual
    private ImageComponent mImgComp;

    /**
     * <p>Constructs an empty game object.</p>
     */
    public GObject()
    {
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned position belongs to either the {@link BodyComponent} or the {@link ImageComponent} with
     * preference given to the BodyComponent. If neither are set, this method returns null.</p>
     *
     * @return position.
     */
    @Override
    public final Point3F getPosition()
    {
        if (mBodyComp != null) {
            return mBodyComp.getPosition();
        } else if (mImgComp != null) {
            return mImgComp.getPosition();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned value belongs to either the {@link BodyComponent} or the {@link ImageComponent} with
     * preference given to the BodyComponent. If neither are set, this method returns 0.</p>
     *
     * @return x.
     */
    @Override
    public final float getX()
    {
        if (mBodyComp != null) {
            return mBodyComp.getX();
        } else if (mImgComp != null) {
            return mImgComp.getX();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned value belongs to either the {@link BodyComponent} or the {@link ImageComponent} with
     * preference given to the BodyComponent. If neither are set, this method returns 0.</p>
     *
     * @return y.
     */
    @Override
    public final float getY()
    {
        if (mBodyComp != null) {
            return mBodyComp.getY();
        } else if (mImgComp != null) {
            return mImgComp.getY();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned value belongs to either the {@link BodyComponent} or the {@link ImageComponent} with
     * preference given to the BodyComponent. If neither are set, this method returns 0.</p>
     *
     * @return z.
     */
    @Override
    public final float getZ()
    {
        if (mBodyComp != null) {
            return mBodyComp.getZ();
        } else if (mImgComp != null) {
            return mImgComp.getZ();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This move requires either a {@link BodyComponent} or {@link ImageComponent} set. If neither are set, this
     * method does nothing.</p>
     *
     * @param x x.
     * @param y y;
     */
    @Override
    public final void moveTo(float x, float y)
    {
        // Move BodyComponent to match pos
        if (mBodyComp != null) {
            // Bail out if not allowed to move
            if (mBodyComp.isStatic()) {
                return;
            }

            mBodyComp.moveTo(x, y);
        }

        // Move ImageComponent to match pos
        if (mImgComp != null) {
            mImgComp.moveTo(x, y);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This move requires either a {@link BodyComponent} or {@link ImageComponent} set. If neither are set, this
     * method does nothing.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    @Override
    public final void moveTo(float x, float y, float z)
    {
        // Move BodyComponent to match pos
        if (mBodyComp != null) {
            // Bail out if not allowed to move
            if (mBodyComp.isStatic()) {
                return;
            }

            mBodyComp.moveTo(x, y, z);
        }

        // Move ImageComponent to match pos
        if (mImgComp != null) {
            mImgComp.moveTo(x, y, z);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This move requires either a {@link BodyComponent} or {@link ImageComponent} set. If neither are set, this
     * method does nothing.</p>
     *
     * @param x amount along x.
     * @param y amount along y.
     */
    @Override
    public final void moveBy(float x, float y)
    {
        moveBy(x, y, 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This move requires either a {@link BodyComponent} or {@link ImageComponent} set. If neither are set, this
     * method does nothing.</p>
     *
     * @param x amount along x.
     * @param y amount along y.
     * @param z amount along z.
     */
    @Override
    public final void moveBy(float x, float y, float z)
    {
        // Move BodyComponent to match pos
        if (mBodyComp != null) {
            // Bail out if not allowed to move
            if (mBodyComp.isStatic()) {
                return;
            }

            mBodyComp.moveBy(x, y, z);
        }

        // Move ImageComponent to match pos
        if (mImgComp != null) {
            mImgComp.moveBy(x, y, z);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This move requires either a {@link BodyComponent} or {@link ImageComponent} set. If neither are set, this
     * method does nothing.</p>
     * *
     * @param x x.
     * @param y y.
     */
    @Override
    public final void moveToCenter(float x, float y)
    {
        // Get representative positional; either body or image
        final Positional pos = (mBodyComp != null) ? mBodyComp : mImgComp;

        // Compute bottom left position and move
        moveTo(x - (pos.getWidth() / 2f), y - (pos.getHeight() / 2f));
    }

    /**
     * {@inheritDoc}
     *
     * <p>The width is that of the {@link BodyComponent} or the {@link ImageComponent} with preference given
     * to the BodyComponent. If neither are set, this method returns 0.</p>
     *
     * @return width.
     */
    @Override
    public final float getWidth()
    {
        if (mBodyComp != null) {
            return mBodyComp.getWidth();
        } else if (mImgComp != null) {
            return mImgComp.getWidth();
        }
        return 0f;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The width is applied to both the {@link BodyComponent} and {@link ImageComponent}.</p>
     *
     * @param width width.
     */
    @Override
    public final void setWidth(float width)
    {
        // Resize collision to width
        if (mBodyComp != null) {
            mBodyComp.setWidth(width);
        }

        // Resize visual to width
        if (mImgComp != null) {
            mImgComp.setWidth(width);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The height is that of the {@link BodyComponent} or the {@link ImageComponent} with preference given
     * to the BodyComponent. If neither are set, this method returns 0.</p>
     *
     * @return height.
     */
    @Override
    public final float getHeight()
    {
        if (mBodyComp != null) {
            return mBodyComp.getHeight();
        } else if (mImgComp != null) {
            return mImgComp.getHeight();
        }
        return 0f;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The height is applied to both the {@link BodyComponent} and {@link ImageComponent}.</p>
     *
     * @param height height.
     */
    @Override
    public final void setHeight(float height)
    {
        // Resize collision to height
        if (mBodyComp != null) {
            mBodyComp.setHeight(height);
        }

        // Resize visual to height
        if (mImgComp != null) {
            mImgComp.setHeight(height);
        }
    }

    @Override
    public final float getCenterX()
    {
        if (mBodyComp != null) {
            return mBodyComp.getCenterX();
        } else if (mImgComp != null) {
            return mImgComp.getCenterX();
        } else {
            return 0f;
        }
    }

    @Override
    public final float getCenterY()
    {
        if (mBodyComp != null) {
            return mBodyComp.getCenterY();
        } else if (mImgComp != null) {
            return mImgComp.getCenterY();
        } else {
            return 0f;
        }
    }

    @Override
    public final double getRotation()
    {
        if (mBodyComp != null) {
            return mBodyComp.getRotation();
        } else if (mImgComp != null) {
            return mImgComp.getRotation();
        } else {
            return 0d;
        }
    }

    @Override
    public final void rotateTo(double angle)
    {
        // Rotate body if available
        final boolean hasBody = mBodyComp != null;
        if (hasBody) {
            mBodyComp.rotateTo(angle);
        }

        // Rotate image if available
        if (mImgComp != null) {
            mImgComp.rotateTo(angle);

            // Center image onto body
            if (hasBody) {
                final float bodyCenterX = mBodyComp.getX() + (mBodyComp.getWidth() / 2f);
                final float bodyCenterY = mBodyComp.getY() + (mBodyComp.getHeight() / 2f);

                mImgComp.moveToCenter(bodyCenterX, bodyCenterY);
            }
        }
    }

    @Override
    public final void rotateBy(double angle)
    {
        // Rotate body if available
        final boolean hasBody = mBodyComp != null;
        if (hasBody) {
            mBodyComp.rotateBy(angle);
        }

        // Rotate image if available
        if (mImgComp != null) {
            mImgComp.rotateBy(angle);

            // Center image onto body
            if (hasBody) {
                final float bodyCenterX = mBodyComp.getX() + (mBodyComp.getWidth() / 2f);
                final float bodyCenterY = mBodyComp.getY() + (mBodyComp.getHeight() / 2f);

                mImgComp.moveToCenter(bodyCenterX, bodyCenterY);
            }
        }
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
     * <p>Any previously set ImageComponent will lose its GObject id and version, resetting to
     * {@link ComponentFactory.Component#NULL}. The newly set component adopts the id and version.
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
     * <p>Any previously set BodyComponent will lose its GObject id and version, resetting to
     * {@link ComponentFactory.Component#NULL}. The newly set component adopts the id and version.
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
     * <p>Moves the {@link ImageComponent} to match positions with the {@link BodyComponent}.</p>
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
    public final boolean contains(float x, float y)
    {
        return (mBodyComp != null && mBodyComp.contains(x, y));
    }

    /**
     * <p>Notifies a listener attached via {@link #setOnClickListener(OnClickListener)} that the GObject was clicked
     * on and returns whether or not the given {@link MouseEvent} was consumed.</p>
     *
     * <p>This method returns false unless a listener is notified. In that case, the returned value is deferred to
     * the listener and its implementation.</p>
     *
     * @param event MouseEvent.
     * @return true if the MouseEvent was used.
     */
    public final boolean click(MouseEvent event)
    {
        return (mOnClickListener != null && mOnClickListener.onClick(event));
    }

    /**
     * <p>Sets the {@link OnClickListener} to be notified of {@link MouseEvent}s used during calls to
     * {@link #click(MouseEvent)}.</p>
     *
     * @param listener OnClickListener.
     */
    public final void setOnClickListener(OnClickListener listener)
    {
        mOnClickListener = listener;
    }

    @Override
    protected final Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Copy operations must go through GObjectFactory");
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
        anchor = (mBodyComp != null) ? mBodyComp : mImgComp;

        String position = "(NULL)";
        if (anchor != null) {
            position = "(" + anchor.getX() + "," + anchor.getY() + "," +
                    anchor.getZ() + ")";
        }

        return "[id(" + getId() + ")," + "locat" + position + ",tex(" + texture + ")," + ((mBodyComp == null) ? "no collision" : mBodyComp.getShape()) + "]";
    }
}
