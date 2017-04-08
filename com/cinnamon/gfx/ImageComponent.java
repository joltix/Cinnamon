package com.cinnamon.gfx;


import com.cinnamon.object.GObject;
import com.cinnamon.system.ComponentFactory;
import com.cinnamon.utils.Point2F;
import com.cinnamon.utils.Point3F;

/**
 * <p>
 *     An ImageComponent represents the visual aspect of a {@link GObject}. In order to be drawn, each GObject holds
 *     a reference to a personal ImageComponent that holds drawing information such as color tinting, transparency
 *     value, and {@link Texture} choice.
 * </p>
 */
public final class ImageComponent extends ComponentFactory.Component implements Drawable
{
    // Tint value bounds
    private static final float COLOR_MIN = 0f;
    private static final float COLOR_MAX = 1f;

    // Listener for changes in number of drawable objects
    private OnDrawVisibilityChangedListener mVisibilityListener;

    // Listener for updates to drawing order
    private OnDrawOrderChangeListener mDrawOrderListener;

    // Position padding
    private final Point2F mOffset = new Point2F(0f, 0f);

    // Position on screen
    private Point3F mPosition = new Point3F(0f, 0f, 0f);

    // Texture id
    private int mTexture = Texture.NULL;

    // Toggles for flipping texture coordinates horizontally and/or vertically
    private boolean mFlipH = false;
    private boolean mFlipV = false;

    // Dimensions
    private float mWidth;
    private float mHeight;

    // Rotation angle in radians
    private double mAngle;

    // Tinting color
    private float mRed = 1f;
    private float mGreen = 1f;
    private float mBlue = 1f;
    private float mAlpha = 1f;

    // Visibility toggle
    private boolean mVisible = true;

    /**
     * <p>Gets the x offset.</p>
     *
     * @return x offset.
     */
    public float getOffsetX()
    {
        return mOffset.getX();
    }

    /**
     * <p>Gets the y offset.</p>
     *
     * @return y offset.
     */
    public float getOffsetY()
    {
        return mOffset.getY();
    }

    /**
     * <p>Sets offset values for the x and y coordinates.</p>
     *
     * <p>These values are added to {@link #getX()} and {@link #getY()}.</p>
     *
     * @param x x offset.
     * @param y y offset.
     */
    public void setOffsets(float x, float y)
    {
        mOffset.set(x, y);
    }

    @Override
    public Point3F getPosition()
    {
        return new Point3F(mPosition);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The value from {@link #getOffsetX()} is automatically added to the returning coordinate.</p>
     */
    @Override
    public float getX()
    {
        return mPosition.getX() + mOffset.getX();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The value from {@link #getOffsetY()} is automatically added to the returning coordinate.</p>
     */
    @Override
    public float getY()
    {
        return mPosition.getY() + mOffset.getY();
    }

    @Override
    public float getZ()
    {
        return mPosition.getZ();
    }

    @Override
    public void moveTo(float x, float y)
    {
        mPosition.set(x, y);
    }

    @Override
    public void moveTo(float x, float y, float z)
    {
        mPosition.set(x, y, z);

        // Notify ImageFactory drawing data may need resorting
        if (!Point2F.isEqual(z, mPosition.getZ()) && mDrawOrderListener != null) {
            mDrawOrderListener.onChange();
        }
    }

    @Override
    public void moveToCenter(float x, float y)
    {
        final float centerX = x - (mWidth / 2f);
        final float centerY = y - (mHeight / 2f);
        moveTo(centerX, centerY);
    }

    @Override
    public void moveBy(float x, float y)
    {
        mPosition.set(mPosition.getX() + x, mPosition.getY() + y);
    }

    @Override
    public void moveBy(float x, float y, float z)
    {
        mPosition.set(mPosition.getX() + x, mPosition.getY() + y, mPosition.getZ() + z);

        // Notify ImageFactory drawing data may need resorting
        if (!Point2F.isEqual(z, mPosition.getZ()) && mDrawOrderListener != null) {
            mDrawOrderListener.onChange();
        }
    }

    @Override
    public float getWidth()
    {
        return mWidth;
    }

    @Override
    public void setWidth(float width)
    {
        mWidth = width;
    }

    @Override
    public float getHeight()
    {
        return mHeight;
    }

    @Override
    public void setHeight(float height)
    {
        mHeight = height;
    }

    @Override
    public float getCenterX()
    {
        return getX() + (getWidth() / 2f);
    }

    @Override
    public float getCenterY()
    {
        return getY() + (getHeight() / 2f);
    }

    @Override
    public double getRotation()
    {
        return mAngle;
    }

    @Override
    public void rotateTo(double angle)
    {
        mAngle = angle;
    }

    @Override
    public void rotateBy(double angle)
    {
        mAngle += angle;
    }

    @Override
    public int getTexture()
    {
        return mTexture;
    }

    /**
     * <p>Sets the {@link Texture} id to use when drawing the image.</p>
     *
     * @param texture Texture id.
     */
    public void setTexture(int texture)
    {
        mTexture = texture;
    }

    /**
     * <p>Sets whether or not the texture should be flipped horizontally when drawn.</p>
     */
    public void setFlipHorizontally(boolean enable)
    {
        mFlipH = enable;
    }

    @Override
    public boolean isFlippedHorizontally()
    {
        return mFlipH;
    }

    /**
     * <p>Sets whether or not the texture should be flipped vertically when drawn.</p>
     */
    public void setFlipVertically(boolean enable)
    {
        mFlipV = enable;
    }

    @Override
    public boolean isFlippedVertically()
    {
        return mFlipV;
    }

    @Override
    public float getRed()
    {
        return mRed;
    }

    @Override
    public float getGreen()
    {
        return mGreen;
    }

    @Override
    public float getBlue()
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
    public void setTint(float red, float green, float blue)
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
    public float getTransparency()
    {
        return mAlpha;
    }

    /**
     * <p>Sets the transparency level.</p>
     *
     * @param alpha transparency.
     */
    public void setTransparency(float alpha)
    {
        // Clamp alpha to normalized range
        if (alpha > COLOR_MAX) {
            alpha = COLOR_MAX;
        } else if (alpha < COLOR_MIN) {
            alpha = COLOR_MIN;
        }

        // Figure if crossed boundary from vis to invis or invis to vis
        final boolean invisible = Point2F.isEqual(mAlpha, 0f);
        final boolean visibilityChanged = (invisible && alpha > 0f) || (!invisible && Point2F.isEqual(alpha, 0f));

        // Update alpha
        mAlpha = alpha;

        // Notify ImageFactory of visibility change
        if (visibilityChanged && mVisibilityListener != null) {
            mVisibilityListener.onChange(mAlpha > 0f || Point2F.isEqual(mAlpha, 0f));
        }
    }

    /**
     * <p>Checks whether or not ImageComponent is allowed to be drawn.</p>
     *
     * @return true if the ImageComponent should be drawn.
     */
    public boolean isVisible()
    {
        return mVisible;
    }

    /**
     * <p>Sets whether or not the ImageComponent should be drawn.</p>
     *
     * @param enable true to allow drawing.
     */
    public void setVisible(boolean enable)
    {
        final boolean previous = mVisible;
        mVisible = enable;

        // Notify ImageFactory to update draw order
        if (mVisible != previous && mVisibilityListener != null) {
            mVisibilityListener.onChange(mVisible);
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
    void setOnVisibilityChangeListener(OnDrawVisibilityChangedListener listener)
    {
        mVisibilityListener = listener;
    }


    /**
     * <p>Sets an {@link OnDrawOrderChangeListener} to be called whenever something occurs that would need the
     * the drawing order of all {@link ImageComponent}s to be updated.</p>
     *
     * @param listener OnDrawOrderChangeListener.
     */
    void setOnDrawOrderChangeListener(OnDrawOrderChangeListener listener)
    {
        mDrawOrderListener = listener;
    }
}
