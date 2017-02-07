package com.cinnamon.gfx;

/**
 * <p>
 *     {@link Canvas} setup for 2D sprite drawing.
 * </p>
 *
 *
 */
public abstract class Canvas2D<E extends Canvas.SceneBuffer,
        T extends ShaderFactory> extends Canvas<E, T>
{
    // QUAD value indices for resizing
    private static final int VERTEX_TOP_LEFT_Y = 4;
    private static final int VERTEX_TOP_RIGHT_X = 6;
    private static final int VERTEX_TOP_RIGHT_Y = 7;
    private static final int VERTEX_BOTTOM_RIGHT = 9;

    // Quad vertices                          x   y    z
    private final float[] QUAD = new float[] {0f, 0f, -1f,  // Bottom left
                                              0f, 0f, -1f,  // Top left
                                              0f, 0f, -1f,  // Top right
                                              0f, 0f, -1f}; // Bottom right

    // Texture coordinates for the quad
    private final float[] TEXTURE_COORDS = new float[] {0f, 1f,
                                                        0f, 0f,
                                                        1f, 0f,
                                                        1f, 1f};

    /**
     * <p>Constructor for a Canvas2D.</p>
     *
     * @param window host {@link Window}.
     * @param input {@link SceneBuffer} providing draw data.
     * @param shaders {@link ShaderFactory}.
     */
    public Canvas2D(Window window, E input, T shaders)
    {
        super(window, input, shaders);
    }

    /**
     * <p>Constructs an orthogonal projection matrix.</p>
     *
     * @return projection matrix.
     */
    @Override
    protected float[] createProjectionMatrix()
    {
        final float left = 0f;
        final float right = getWidth();
        final float bottom = 0f;
        final float top = getHeight();
        final float near = 0.001f;
        final float far = 1000f;

        return new float[] {(2f / (right - left)), 0, 0, 0,
                             0, (2f / (top - bottom)), 0, 0,
                             0, 0, (-2f / (far - near)), 0,
                (-(right + left) / (right - left)), (-(top + bottom) /
                (top - bottom)), (-(far + near) / (far - near)), (1f)};
    }

    /**
     * <p>Constructs a translation matrix for translating only along the x
     * and y axes.</p>
     *
     * @param x x translation.
     * @param y y translation.
     * @return a 4x4 matrix.
     */
    protected final float[] getTranslationMatrix(float x, float y)
    {
        return this.getTranslationMatrix(x, y, 0f);
    }

    /**
     * <p>Gets a 1D array of texture coordinates formatted as a 2x4 matrix
     * .</p>
     *
     * @return texture coordinates.
     */
    protected float[] getTextureCoordinates()
    {
        return TEXTURE_COORDS;
    }

    /**
     * <p>Returns a 1D array of vertices formatted as a 3x4 matrix.</p>
     *
     * @param width quad width.
     * @param height quad height.
     * @return quad vertices.
     */
    protected final float[] getQuad(float width, float height)
    {
        QUAD[VERTEX_TOP_LEFT_Y] = height;
        QUAD[VERTEX_TOP_RIGHT_X] = width;
        QUAD[VERTEX_TOP_RIGHT_Y] = height;
        QUAD[VERTEX_BOTTOM_RIGHT] = width;

        return QUAD;
    }
}
