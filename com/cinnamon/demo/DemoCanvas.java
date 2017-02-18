package com.cinnamon.demo;

import com.cinnamon.gfx.*;
import com.cinnamon.system.Window;
import org.lwjgl.opengl.GL11;

/**
 * <p>
 *     Demo {@link Canvas2D}.
 * </p>
 *
 *
 */
public final class DemoCanvas extends Canvas2D<ConcurrentSceneBuffer,
        ShaderFactory>
{
    // Indices for specifying triangle vertices (2) within a quad
    private static final int[] QUAD_INDICES = new int[]{
            0, 1, 2, // First triangle
            0, 2, 3 // Second triangle
    };

    // Max number of instances in a Batch to allocate memory for
    private static final int INSTANCING_CAPACITY = 2000;

    // Thread for running Canvas
    private Thread mThread;

    // ShaderProgram for everything to draw
    private ShaderProgram mColorShader;
    private ShaderProgram mTextureShader;

    // GLBuffer to facilitate VBOs and binding
    private DemoVBOBuffer mBuffer;


    private final float[] TINT = new float[4];

    public DemoCanvas(Window window, ConcurrentSceneBuffer input,
                      ShaderFactory shaders)
    {
        super(window, input, shaders);
    }

    @Override
    public void start()
    {
        // Launch drawing on another Thread
        mThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                loop();
            }
        });

        mThread.start();
    }

    @Override
    protected void onLoad()
    {
        // Block until shaders have been loaded
        ShaderFactory shaderFactory = getShaderFactory();
        shaderFactory.load();

        // Apply simple shader for all drawing
        mColorShader = shaderFactory.getShader("demo_color");
        mTextureShader = shaderFactory.getShader("demo_texture");
        mColorShader.use();

        final float[] proj = this.getProjection();
        mBuffer = new DemoVBOBuffer(INSTANCING_CAPACITY, proj, QUAD_INDICES);

        // Allow transparency
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    protected void draw(ConcurrentSceneBuffer input, ShaderFactory factory)
    {
        // Fill window background with color
        setBackgroundColor(0, 0, 0, 255);

        final Scene<Batch<Drawable>> scene = input.getReadScene();

        // Draw each Batch until Scene has none left
        while (!scene.isEmpty()) {

            final Batch<Drawable> batch = scene.poll();
            final int texId = batch.getTexture();

            // Toggle whether or not to use instancing when drawing
            mBuffer.setInstancing(batch.size() > 1);

            // Write all Drawables in a Batch to the GLBuffer
            while (!batch.isEmpty()) {
                writeToBuffer(batch.poll());
            }

            // Send all drawing data to the gpu
            mBuffer.flush();

            // Process whether or not to bind a Texture
            applyTexture(texId);

            // OpenGL draw function specific to GLBuffer implementation
            mBuffer.draw();

            // Clear current drawing data for next Scene
            mBuffer.clear();
        }
    }

    private void writeToBuffer(Drawable obj)
    {
        // Setup instance data
        final float[] quad = this.getQuad(obj.getWidth(), obj.getHeight());
        final float[] texCoords = this.getTextureCoordinates();

        // Alter coordinates based on world origin or screen origin
        float y = obj.getY();
        final float[] translation = this.getTranslationMatrix(obj.getX(), y);

        // Pull color data
        TINT[0] = obj.getRed();
        TINT[1] = obj.getGreen();
        TINT[2] = obj.getBlue();
        TINT[3] = obj.getTransparency();

        // Put draw data in intermediate buffer
        mBuffer.add(quad, texCoords, TINT, translation);
    }

    private void applyTexture(int texture)
    {
        if (texture == Texture.NULL) {
            mColorShader.use();
        } else {
            mTextureShader.use();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }
}