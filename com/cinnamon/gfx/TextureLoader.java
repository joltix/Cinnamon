package com.cinnamon.gfx;

import com.cinnamon.utils.ResourceLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

/**
 * <p>
 *     TextureLoader loads images from a jar's resources and prepares it for OpenGL use.
 * </p>
 */
public class TextureLoader extends ResourceLoader<Texture>
{
    // Texture storage
    private ShaderFactory.TextureMap mTextureMap;

    /**
     * <p>Constructor for a TextureLoader.</p>
     *
     * @param path directory of images to load.
     */
    public TextureLoader(String path)
    {
        super(path);
    }

    @Override
    protected Texture assemble(String name, InputStream link)
    {
        // Read bytes
        final ByteBuffer img = readBytes(link);
        try {
            link.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Read pixels and img data
        final IntBuffer w = BufferUtils.createIntBuffer(1);
        final IntBuffer h = BufferUtils.createIntBuffer(1);
        final IntBuffer comps = BufferUtils.createIntBuffer(1);
        final ByteBuffer pxData = pullPixels(img, w, h, comps);

        // Pull dimensions and color component count
        final int width = w.get(0);
        final int height = h.get(0);
        final int components = comps.get(0);

        // Get a Texture id and bind for configuration
        final int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);

        // Configure Texture
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        // Push Texture data
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0,GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, pxData);

        // Create mipmaps and unbind
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        return new Texture(pxData, width, height, components, id);
    }

    /**
     * <p>Reads the bytes from an {@link InputStream} into a {@link ByteBuffer} for later extracting the pixel data.</p>
     *
     * @param stream InputStream to resource.
     * @return resource's bytes.
     */
    protected ByteBuffer readBytes(InputStream stream)
    {
        byte[] bytes = null;
        try {
            final ByteArrayOutputStream buffStream;
            buffStream = new ByteArrayOutputStream();

            // Keep reading and writing bytes from resource
            int data;
            while ((data = stream.read()) != -1) {
                buffStream.write(data);
            }

            // Get bytes in arr and cleanup stream
            bytes = buffStream.toByteArray();
            buffStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Put img's bytes into buffer for STB Image
        final ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes);

        // Adjust buffer for reading
        buffer.flip();
        return buffer;
    }

    /**
     * <p>Attempts to decode a given {@link ByteBuffer} as an image. The returned ByteBuffer is ready for OpenGL
     * rendering.</p>
     *
     * @param bytes bytes as image.
     * @param w width container.
     * @param h height container.
     * @param channels color channels container.
     * @return decoded bytes.
     * @throws IllegalArgumentException if there was a problem while decoding.
     */
    private ByteBuffer pullPixels(ByteBuffer bytes, IntBuffer w, IntBuffer
            h, IntBuffer channels)
    {
        // Pull pixels from img
        final ByteBuffer pixels = STBImage.stbi_load_from_memory(bytes, w, h,
                channels, 4);

        // Validate img
        if (pixels == null) {
            throw new IllegalArgumentException("Bytes could not be decoded: "
                    + STBImage.stbi_failure_reason());
        }

        return pixels;
    }

    /**
     * <p>Loads all {@link Texture}s in the directory whose filename is on a given list. If a file on the list is not
     * found in the directory, this method continues on to attempt to load the next filename.</p>
     *
     * @param filenames image filenames to load.
     * @param textureMap container for loaded Textures.
     */
    public void loadDirectory(List<String> filenames, ShaderFactory.TextureMap
            textureMap)
    {
        try {
            mTextureMap = textureMap;

            super.loadDirectory(filenames);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDirectoryLoad(String name, Texture texture)
    {
        mTextureMap.put(name, texture);
    }
}
