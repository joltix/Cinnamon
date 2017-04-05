package com.cinnamon.demo;

import com.cinnamon.gfx.*;
import com.cinnamon.system.Window;
import org.lwjgl.opengl.GL11;

/**
 * <p>
 *     Demo {@link Canvas2D}.
 * </p>
 */
public final class DemoCanvas extends Canvas2D<ConcurrentSceneBuffer, ShaderFactory>
{
    // Max number of instances in a Batch to allocate memory for
    private static final int INSTANCING_CAPACITY = 500;

    // Array for holding translation matrix
    private static final float[] TRANSLATION = new float[16];

    // Array for holding x and y scaling values
    private static final float[] SCALE = new float[2];

    // Array for holding color tinting values (RGBA)
    private final float[] TINT = new float[4];

    // ShaderProgram for everything to draw
    private ShaderProgram mColorShader;
    private ShaderProgram mTextureShader;

    // GLBuffer to facilitate VBOs and binding
    private DemoVBOBuffer mBuffer;

    /**
     * <p>Constructs a DemoCanvas for drawing the demo.</p>
     *
     * @param window Window.
     * @param input SceneBuffer.
     * @param shaders ShaderFactory.
     */
    public DemoCanvas(Window window, ConcurrentSceneBuffer input, ShaderFactory shaders)
    {
        super(window, input, shaders);
    }

    @Override
    protected void onLoad()
    {
        // Load shaders
        ShaderFactory shaderFactory = getShaderFactory();
        shaderFactory.load();

        // Apply simple shader for all drawing
        mColorShader = shaderFactory.getShader("demo_color");
        mTextureShader = shaderFactory.getShader("demo_texture");
        mColorShader.use();

        // Get projection, vertices, vertex indices, and texture coordinates for all drawing
        final float[] projection = this.getProjectionMatrix();
        final float[] vertices = this.getQuadVertices();
        final int[] indices = this.getQuadIndices();
        final float[] textureCoords = this.getQuadTextureCoordinates();

        // Use a GLBuffer to obfuscate drawing details
        mBuffer = new DemoVBOBuffer(INSTANCING_CAPACITY, projection, vertices, indices, textureCoords);

        // Allow transparency
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    protected void onResize()
    {
        // Update GLBuffer's projection matrix since resolution changed
        mBuffer.setProjection(getProjectionMatrix());
    }

    @Override
    protected void draw(ConcurrentSceneBuffer input, ShaderFactory factory)
    {
        // Fill window background with black
        setBackgroundColor(0, 0, 0, 255);

        // Get next Scene to draw
        final Scene<Batch<Drawable>> scene = input.getReadScene();

        // Draw each Batch until Scene has none left
        while (!scene.isEmpty()) {

            // Pull Batch and its Drawables' texture
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

    /**
     * <p>Writes the {@link Drawable}'s drawing data to the {@link DemoVBOBuffer}.</p>
     *
     * @param drawable Drawable.
     */
    private void writeToBuffer(Drawable drawable)
    {
        // Get transformation info
        this.getTranslationMatrix(TRANSLATION, drawable.getX(), drawable.getY());
        SCALE[0] = drawable.getWidth();
        SCALE[1] = drawable.getHeight();
        final float rotation = (float) drawable.getRotation();

        // Pull color data
        TINT[0] = drawable.getRed();
        TINT[1] = drawable.getGreen();
        TINT[2] = drawable.getBlue();
        TINT[3] = drawable.getTransparency();

        // Get texture flip toggles
        final boolean flipH = drawable.isFlippedHorizontally();
        final boolean flipV = drawable.isFlippedVertically();

        // Put draw data in intermediate buffer
        mBuffer.add(TINT, flipH, flipV, TRANSLATION, SCALE, rotation);

        Canvas.checkForOpenGLErrors();
    }

    /**
     * <p>Applies either the textured or non textured shader depending on the given {@link Texture} id.</p>
     *
     * @param texture Texture id.
     */
    private void applyTexture(int texture)
    {
        // Use non-textured shader (color only)
        if (texture == Texture.NULL) {
            mColorShader.use();
        } else {

            // Use textured shader and bind image data
            mTextureShader.use();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }
}