package com.cinnamon.gfx;

import com.cinnamon.utils.Identifiable;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

/**
 * <p>
 *     Texture represents a 2D image to be used in OpenGL drawing operations.
 * </p>
 *
 *
 */
public class Texture implements Identifiable
{
    // Null id
    public static final int NULL = 0;

    // Number of color channels per type
    private static final int CHANNELS_RGBA = 4;
    private static final int CHANNELS_RGB = 3;

    // Identifier
    private int mTexId;

    // Pixels to draw
    private final ByteBuffer mPxData;

    // Img dimensions and color channels
    private final int mWidth;
    private final int mHeight;
    private final int mChannels;

    /**
     * <p>Constructor for a Texture.</p>
     *
     * @param image pixel data.
     * @param width width.
     * @param height height.
     * @param channels number of color channels.
     * @param id OpenGL texture id.
     */
    public Texture(ByteBuffer image, int width, int height, int
            channels, int id)
    {
        if (image == null) {
            throw new IllegalArgumentException("Image data must exist");
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Dimensions must be > 1: (" +
                    width + "," + height + ")");
        }
        if (channels != CHANNELS_RGB && channels != CHANNELS_RGBA) {
            throw new IllegalArgumentException("Only RGB (3) and RGBA (4) are" +
                    " " +
                    "supported");
        }

        mTexId = id;

        // Save img meta
        mPxData = image;
        mWidth = width;
        mHeight = height;
        mChannels = channels;
    }

    /**
     * <p>Gets the image's pixel data as bytes for use in OpenGL.</p>
     *
     * @return pixel data.
     */
    public final ByteBuffer getPixelData()
    {
        return mPxData;
    }

    /**
     * <p>Gets the image's width.</p>
     *
     * @return width.
     */
    public final int getWidth()
    {
        return mWidth;
    }

    /**
     * <p>Gets the image's height.</p>
     *
     * @return height.
     */
    public final int getHeight()
    {
        return mHeight;
    }

    /**
     * <p>Gets the number of color channels supported by the Texture.</p>
     *
     * @return number of channels such as RGBA (4).
     */
    public int getColorChannels()
    {
        return mChannels;
    }

    /**
     * <p>Checks whether or not the Texture's image data uses the fourth
     * channel (transparency).</p>
     *
     * @return true if supports transparency.
     */
    public boolean isRGBA()
    {
        return mChannels == CHANNELS_RGBA;
    }

    /**
     * <p>Gets Texture id assigned via {@link GL11#glGenTextures()}.</p>
     *
     * @return id.
     */
    public int getId()
    {
        return mTexId;
    }

    /**
     * Allows the Texture's id to be assigned to another. This Texture's id
     * becomes 0.
     */
    public void delete()
    {
        GL11.glDeleteTextures(mTexId);
        mTexId = 0;
    }

    @Override
    public String toString()
    {
        return "[Texture(" + mTexId + "): w(" + mWidth + ") h(" + mHeight +
                ") channels(" + mChannels + ")]";

    }
}
