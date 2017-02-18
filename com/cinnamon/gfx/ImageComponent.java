package com.cinnamon.gfx;


import com.cinnamon.object.Dimensional;
import com.cinnamon.object.GObject;
import com.cinnamon.object.Positional;
import com.cinnamon.system.ComponentFactory;
import com.cinnamon.utils.Point3F;

/**
 * <p>
 *     An ImageComponent represents the visual aspect of a {@link GObject}.
 *     In order to be drawn, each GObject holds a reference to a personal
 *     ImageComponent that holds drawing information such as color tinting,
 *     transparency value, and {@link Texture} choice.
 * </p>
 */
public abstract class ImageComponent extends ComponentFactory.Component
        implements Drawable, Positional, Dimensional
{
    // Tint value bounds
    private static final float COLOR_MIN = 0f;
    private static final float COLOR_MAX = 1f;

    // Visibility change listener for notifying ImageFactory of draw refresh
    private OnDrawVisibilityChangedListener mVisibilityListener;

    // Position padding
    private final Point3F mOffset = new Point3F(0, 0, 0);

    // Position on screen
    private Point3F mPosition = new Point3F(0, 0, 0);

    // Dimensions
    private float mWidth;
    private float mHeight;

    // Texture id
    private int mTexture;

    // Tinting color
    private float mRed = 1f;
    private float mGreen = 1f;
    private float mBlue = 1f;
    private float mAlpha = 1f;

    // Visibility toggle
    private boolean mVisible = true;


    @Override
    public final float getOffsetX()
    {
        return mOffset.getX();
    }

    @Override
    public final float getOffsetY()
    {
        return mOffset.getY();
    }

    @Override
    public final float getOffsetZ()
    {
        return mOffset.getZ();
    }

    @Override
    public final void setOffset(float x, float y)
    {
        mOffset.set(x, y);
    }

    @Override
    public final void setOffset(float x, float y, float z)
    {
        mOffset.set(x, y, z);
    }

    @Override
    public final float getX()
    {
        return mPosition.getX() + mOffset.getX();
    }

    @Override
    public final float getY()
    {
        return mPosition.getY() + mOffset.getY();
    }

    @Override
    public final float getZ()
    {
        return mPosition.getZ() + mOffset.getZ();
    }

    @Override
    public final void moveTo(float x, float y, float z)
    {
        mPosition.set(x, y, z);

        // Notify ImageFactory drawing data may need resorting
        if (z != mPosition.getZ() && mVisibilityListener != null) {
            mVisibilityListener.onChange(true);
        }
    }

    @Override
    public final void moveTo(float x, float y)
    {
        mPosition.set(x, y);
    }

    @Override
    public final float getWidth()
    {
        return mWidth;
    }

    @Override
    public final void setWidth(float width)
    {
        mWidth = width;
    }

    @Override
    public final float getHeight()
    {
        return mHeight;
    }

    @Override
    public final void setHeight(float height)
    {
        mHeight = height;
    }


    @Override
    public final int getTexture()
    {
        return mTexture;
    }

    /**
     * <p>Sets the {@link Texture} id to use when drawing the image.</p>
     *
     * @param texture Texture id.
     */
    public final void setTexture(int texture)
    {
        mTexture = texture;
    }

    @Override
    public final float getRed()
    {
        return mRed;
    }

    @Override
    public final float getGreen()
    {
        return mGreen;
    }

    @Override
    public final float getBlue()
    {
        return mBlue;
    }

    /**
     * <p>Sets the red, green, and blue color values for tinting the image.</p>
     *
     * @param red red.
     * @param green green.
     * @param blue blue.
     */
    public final void setTint(float red, float green, float blue)
    {
        if (red > COLOR_MAX) {
            red = COLOR_MAX;
        } else if (red < COLOR_MIN) {
            red = COLOR_MIN;
        }

        if (green > COLOR_MAX) {
            green = COLOR_MAX;
        } else if (green < COLOR_MIN) {
            green = COLOR_MIN;
        }

        if (blue > COLOR_MAX) {
            blue = COLOR_MAX;
        } else if (blue < COLOR_MIN) {
            blue = COLOR_MIN;
        }

        mRed = red;
        mGreen = green;
        mBlue = blue;
    }

    @Override
    public final float getTransparency()
    {
        return mAlpha;
    }

    /**
     * <p>Sets the transparency level.</p>
     *
     * @param alpha transparency.
     */
    public final void setTransparency(float alpha)
    {
        if (alpha > COLOR_MAX) {
            alpha = COLOR_MAX;
        } else if (alpha < COLOR_MIN) {
            alpha = COLOR_MIN;
        }

        // Figure if crossed boundary from vis to invis or invis to vis
        boolean visibilityChanged = false;
        if ((mAlpha <= 0 && alpha > 0)
                || (mAlpha > 0 && alpha <= 0)) {
            visibilityChanged = true;
        }

        // Update alpha
        mAlpha = alpha;

        // Notify ImageFactory of visibility change
        if (visibilityChanged && mVisibilityListener != null) {
            mVisibilityListener.onChange(mAlpha >= 0);
        }
    }

    /**
     * <p>Checks whether or not ImageComponent is allowed to be drawn.</p>
     *
     * @return true if the ImageComponent should be drawn.
     */

    public final boolean isVisible()
    {
        return mVisible;
    }

    /**
     * <p>Sets whether or not the ImageComponent should be drawn.</p>
     *
     * @param enable true to allow drawing.
     */
    public final void setVisible(boolean enable)
    {
        mVisible = enable;

        // Notify ImageFactory to update draw order
        if (enable != mVisible && mVisibilityListener != null) {
            mVisibilityListener.onChange(false);
        }
    }

    /**
     * <p>Sets an {@link OnDrawVisibilityChangedListener} to be called whenever
     * either the ImageComponent's transparency reaches 0, visibility is
     * changed with {@link #setVisible(boolean)}, or the depth (z position) is
     * changed.</p>
     *
     * @param listener OnDrawVisibilityChangedListener.
     */
    void setOnVisibilityChangeListener(OnDrawVisibilityChangedListener
                                                             listener)
    {
        mVisibilityListener = listener;
    }
}
