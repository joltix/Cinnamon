package com.cinnamon.gfx;

import com.cinnamon.system.Window;

/**
 * <p>
 *     {@link Canvas} setup for 2D sprite drawing.
 * </p>
 */
public abstract class Canvas2D<E extends Canvas.SceneBuffer, T extends ShaderFactory> extends Canvas<E, T>
{
    // Quad vertices                                   x   y    z
    private final float[] QUAD_VERTICES = new float[] {0f, 0f, -1f,  // Bottom left
                                                       0f, 1f, -1f,  // Top left
                                                       1f, 1f, -1f,  // Top right
                                                       1f, 0f, -1f}; // Bottom right


    // Indices for specifying triangle vertices (2 of them) within a quad
    private static final int[] QUAD_INDICES = new int[]{0, 1, 2,    // First triangle
                                                        0, 2, 3};   // Second triangle

    // Each quad's texture coordinates                         s   t
    private static final float[] TEXTURE_COORDS = new float[] {0f, 1f,  // Top left
                                                               0f, 0f,  // Bottom left
                                                               1f, 0f,  // Bottom right
                                                               1f, 1f}; // Top right

    /**
     * <p>Constructs a Canvas2D.</p>
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
     * <p>Fills a given array with a translation matrix modified for the given x and y values.</p>
     *
     * @param x x.
     * @param y y.
     * @throws IllegalArgumentException if the given array is not of length 16 (the length of a flattened 4x4 matrix).
     */
    protected final void getTranslationMatrix(float[] container, float x, float y)
    {
        this.getTranslationMatrix(container, x, y, 0f);
    }

    /**
     * <p>Gets a 1D 8-float-long array of texture coordinates for a quad. Coordinates begin from top left corner (0,
     * 1) and rotate counter-clockwise.</p>
     *
     * @return texture coordinates.
     */
    protected final float[] getQuadTextureCoordinates()
    {
        return TEXTURE_COORDS;
    }

    /**
     * <p>Gets a 1D 12-float-long array of vertices forming a quad. Vertices begin from bottom left corner (0,0)
     * and rotate clockwise.</p>
     *
     * @return a quad's vertices.
     */
    protected final float[] getQuadVertices()
    {
        return QUAD_VERTICES.clone();
    }

    /**
     * <p>Gets a 1D 6-int-long array of indices designating the order of vertices for forming two triangles that make
     * up a quad.</p>
     *
     * @return a quad's vertex indices.
     */
    protected final int[] getQuadIndices()
    {
        return QUAD_INDICES.clone();
    }
}
