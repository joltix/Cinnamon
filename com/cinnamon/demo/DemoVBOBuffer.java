package com.cinnamon.demo;

import com.cinnamon.gfx.Canvas;
import com.cinnamon.gfx.GLBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * <p>
 *     DemoVBOBuffer helps pull out buffering operations for OpenGL drawing
 *     from the {@link Canvas}.
 * </p>
 */
public final class DemoVBOBuffer extends GLBuffer
{
    /**
     * Vertex attribute indices
     */

    private static final int LOCATION_POSITION = 0;
    private static final int LOCATION_TEX_COORDS = 1;
    private static final int LOCATION_TEX_FLIP = 2;
    private static final int LOCATION_COLORS = 3;
    private static final int LOCATION_PROJECTION = 4;
    private static final int LOCATION_TRANSLATION = 8;
    private static final int LOCATION_SCALE = 12;
    private static final int LOCATION_ROTATION = 13;

    /**
     * Number of values per instance (an (x,y) coord counts as two vals)
     */

    private static final int SIZE_VERTEX = 3 * 4;
    private static final int SIZE_INDEX = 6;
    private static final int SIZE_TEX_COORDS = 2 * 4;
    private static final int SIZE_TEX_FLIP = 2;
    private static final int SIZE_COLOR = 4;
    private static final int SIZE_SCALE = 2;
    private static final int SIZE_ROTATION = 1;
    private static final int SIZE_MAT4 = 4 * 4;

    // 4x4 projection matrix for drawing
    private float[] mProjection;

    // Vertex indices
    private final int[] mConstantIndices;

    // Whether or not larger buffers should be used for batch drawing
    private boolean mInstancing;

    // Number of instances to draw
    private int mInstanceCount;

    // Element buffer object for vertex indices
    private int mEBOIndices;

    /**
     * Vertex buffer objects for each vertex attribute
     */

    private int mVBOPositions;
    private int mVBOTexCoords;
    private int mVBOTexFlip;
    private int mVBOColors;
    private int mVBOProjections;
    private int mVBOTranslations;
    private int mVBOScales;
    private int mVBORotation;

    /**
     * Single instance capacity buffers for individual drawing
     */

    private final FloatBuffer mVertices;
    private final IntBuffer mIndices;
    private final FloatBuffer mTexCoords;
    private final IntBuffer mTexFlip;
    private final FloatBuffer mColors;
    private final FloatBuffer mProjections;
    private final FloatBuffer mTranslations;
    private final FloatBuffer mScales;
    private final FloatBuffer mRotation;

    /**
     * Larger capacity buffers for instancing
     */

    private final FloatBuffer mBatchVertices;
    private final IntBuffer mBatchIndices;
    private final FloatBuffer mBatchTexCoords;
    private final IntBuffer mBatchTexFlip;
    private final FloatBuffer mBatchColors;
    private final FloatBuffer mBatchProjections;
    private final FloatBuffer mBatchTranslations;
    private final FloatBuffer mBatchScales;
    private final FloatBuffer mBatchRotation;

    /**
     * Currently selected data sources (to actually draw)
     */

    private FloatBuffer mSelVertices;
    private IntBuffer mSelIndices;
    private FloatBuffer mSelTexCoords;
    private IntBuffer mSelTexFlip;
    private FloatBuffer mSelColors;
    private FloatBuffer mSelProjections;
    private FloatBuffer mSelTranslations;
    private FloatBuffer mSelScales;
    private FloatBuffer mSelRotation;

    /**
     * <p>Constructor for a DemoVBOBuffer.</p>
     *
     * @param capacity max number of instances allowed at a time.
     * @param projection projection matrix for all instances.
     * @param vertices quad's vertices for each instance.
     * @param indices indices for all instances' triangles.
     */
    public DemoVBOBuffer(int capacity, float[] projection, float[] vertices, int[] indices, float[] textureCoords)
    {
        // Make sure capacity is for drawing > 1
        if (capacity <= 1) {
            throw new IllegalArgumentException("Batch capacity must be > 1");
        }

        // Defensive copy projection for each instance added
        mProjection = projection.clone();
        mConstantIndices = indices;

        // Generate and bind vertex array object
        final int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // Generate vertices VBOs
        mVBOPositions = GL15.glGenBuffers();
        mEBOIndices = GL15.glGenBuffers();

        // Generate vertex attrib VBOs
        mVBOTexCoords = GL15.glGenBuffers();
        mVBOTexFlip = GL15.glGenBuffers();
        mVBOColors = GL15.glGenBuffers();
        mVBOProjections = GL15.glGenBuffers();
        mVBOTranslations = GL15.glGenBuffers();
        mVBOScales = GL15.glGenBuffers();
        mVBORotation = GL15.glGenBuffers();

        // Init single draw sources
        mVertices = BufferUtils.createFloatBuffer(SIZE_VERTEX);
        mIndices = BufferUtils.createIntBuffer(SIZE_INDEX);
        mTexCoords = BufferUtils.createFloatBuffer(SIZE_TEX_COORDS);
        mTexFlip = BufferUtils.createIntBuffer(SIZE_TEX_FLIP);
        mColors = BufferUtils.createFloatBuffer(SIZE_COLOR);
        mProjections = BufferUtils.createFloatBuffer(SIZE_MAT4);
        mTranslations = BufferUtils.createFloatBuffer(SIZE_MAT4);
        mScales = BufferUtils.createFloatBuffer(SIZE_SCALE);
        mRotation = BufferUtils.createFloatBuffer(SIZE_ROTATION);

        // Init batch draw sources
        mBatchVertices = BufferUtils.createFloatBuffer(capacity * SIZE_VERTEX);
        mBatchIndices = BufferUtils.createIntBuffer(capacity * SIZE_INDEX);
        mBatchTexCoords = BufferUtils.createFloatBuffer(capacity * SIZE_TEX_COORDS);
        mBatchTexFlip = BufferUtils.createIntBuffer(capacity * SIZE_TEX_FLIP);
        mBatchColors = BufferUtils.createFloatBuffer(capacity * SIZE_COLOR);
        mBatchProjections = BufferUtils.createFloatBuffer(capacity * SIZE_MAT4);
        mBatchTranslations = BufferUtils.createFloatBuffer(capacity * SIZE_MAT4);
        mBatchScales = BufferUtils.createFloatBuffer(capacity * SIZE_SCALE);
        mBatchRotation = BufferUtils.createFloatBuffer(capacity * SIZE_ROTATION);

        // Set single draw sources as default
        mSelVertices = mVertices;
        mSelIndices = mIndices;
        mSelTexCoords = mTexCoords;
        mSelTexFlip = mTexFlip;
        mSelColors = mColors;
        mSelProjections = mProjections;
        mSelTranslations = mTranslations;
        mSelScales = mScales;
        mSelRotation = mRotation;

        // Allocate memory for high capacity buffers
        allocateMemory();

        // Insert base vertices used for all drawing
        mBatchVertices.put(vertices);
        mVertices.put(vertices);

        // Insert base texture coordinates used for all drawing
        mBatchTexCoords.put(textureCoords);
        mTexCoords.put(textureCoords);
    }

    /**
     * <p>Allocates memory ahead of time by buffering the larger capacity buffers.</p>
     */
    private void allocateMemory()
    {
        // Bind vertices and upload
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOPositions);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchVertices, GL15.GL_DYNAMIC_DRAW);

        // Bind element buffer object and allocate indices memory
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, mEBOIndices);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, mBatchIndices, GL15.GL_STATIC_DRAW);

        // Specify vertices (position) layout
        GL20.glVertexAttribPointer(LOCATION_POSITION, 3, GL11.GL_FLOAT, false, 12, 0);
        GL20.glEnableVertexAttribArray(LOCATION_POSITION);

        // Allocate memory for texture coords
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOTexCoords);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchTexCoords, GL15.GL_DYNAMIC_DRAW);

        // Specify texture coords layout
        GL20.glVertexAttribPointer(LOCATION_TEX_COORDS, 2, GL11.GL_FLOAT, false, 8, 0);
        GL20.glEnableVertexAttribArray(LOCATION_TEX_COORDS);

        // Allocate memory for texture flip toggles
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOTexFlip);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchTexFlip, GL15.GL_DYNAMIC_DRAW);

        // Specify texture flip layout
        GL30.glVertexAttribIPointer(LOCATION_TEX_FLIP, 2, GL11.GL_INT, SIZE_TEX_FLIP * 4, 0);
        GL20.glEnableVertexAttribArray(LOCATION_TEX_FLIP);

        // Bind colors
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOColors);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchColors, GL15.GL_DYNAMIC_DRAW);

        // Specify colors layout
        GL20.glVertexAttribPointer(LOCATION_COLORS, 4, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(LOCATION_COLORS);

        // Bind projection matrices
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOProjections);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchProjections, GL15.GL_DYNAMIC_DRAW);

        // Specify projection matrix layout
        this.attribPointerMat4(LOCATION_PROJECTION,SIZE_MAT4 * 4, 0);
        GL20.glEnableVertexAttribArray(LOCATION_PROJECTION);

        // Bind translation matrices
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOTranslations);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchTranslations,
                GL15.GL_DYNAMIC_DRAW);

        // Specify translation matrix layout
        this.attribPointerMat4(LOCATION_TRANSLATION,SIZE_MAT4 * 4, 0);
        GL20.glEnableVertexAttribArray(LOCATION_TRANSLATION);

        // Bind scaling values
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOScales);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchScales, GL15.GL_DYNAMIC_DRAW);

        // Specify scale values layout
        GL20.glVertexAttribPointer(LOCATION_SCALE, 2, GL11.GL_FLOAT, false, 8, 0);
        GL20.glEnableVertexAttribArray(LOCATION_SCALE);

        // Bind rotation angle
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBORotation);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchRotation, GL15.GL_DYNAMIC_DRAW);

        // Specify rotation angle layout
        GL20.glVertexAttribPointer(LOCATION_ROTATION, 1, GL11.GL_FLOAT, false, 4, 0);
        GL20.glEnableVertexAttribArray(LOCATION_ROTATION);

        // Unbind VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * <p>Adds drawing data for one instance to the buffer.</p>
     *
     * @param colors colors.
     * @param flipH flip horizontally.
     * @param flipV flip vertically.
     * @param translation translation matrix.
     * @param scale x and y scaling.
     * @param angle rotation in radians.
     */
    public void add(float[] colors, boolean flipH, boolean flipV, float[] translation, float[] scale, float angle)
    {
        // Use same vertex indices as previous
        mSelIndices.put(mConstantIndices);

        // Add image flip
        mSelTexFlip.put((flipH) ? 1 : 0);
        mSelTexFlip.put((flipV) ? 1 : 0);

        // Add colors
        mSelColors.put(colors);

        // Add projection and translation matrices
        mSelProjections.put(mProjection);
        mSelTranslations.put(translation);
        mSelScales.put(scale);
        mSelRotation.put(angle);

        // Count instance (for instanced drawing)
        mInstanceCount++;
    }

    @Override
    public void flush() {
        // Prep for reading
        flipBuffers();

        // Format vertices
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOPositions);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelVertices);
        GL20.glEnableVertexAttribArray(LOCATION_POSITION);
        GL33.glVertexAttribDivisor(LOCATION_POSITION, 0);

        // Upload indices
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, mEBOIndices);
        GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, mSelIndices);

        // Format texture coordinates
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOTexCoords);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelTexCoords);
        GL20.glEnableVertexAttribArray(LOCATION_TEX_COORDS);
        GL33.glVertexAttribDivisor(LOCATION_TEX_COORDS, 0);

        // Format texture flip toggles
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOTexFlip);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelTexFlip);
        GL20.glEnableVertexAttribArray(LOCATION_TEX_FLIP);
        GL33.glVertexAttribDivisor(LOCATION_TEX_FLIP, 1);

        // Format colors
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOColors);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelColors);
        GL20.glEnableVertexAttribArray(LOCATION_COLORS);
        GL33.glVertexAttribDivisor(LOCATION_COLORS, 1);

        // Format projections
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOProjections);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelProjections);
        this.enableVertexAttribArrayMat4(LOCATION_PROJECTION);
        this.vertexAttribDivisorMat4(LOCATION_PROJECTION, 1);

        // Format translations
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOTranslations);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelTranslations);
        this.enableVertexAttribArrayMat4(LOCATION_TRANSLATION);
        this.vertexAttribDivisorMat4(LOCATION_TRANSLATION, 1);

        // Format scales
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOScales);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelScales);
        GL20.glEnableVertexAttribArray(LOCATION_SCALE);
        GL33.glVertexAttribDivisor(LOCATION_SCALE, 1);

        // Format rotation angle
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBORotation);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelRotation);
        GL20.glEnableVertexAttribArray(LOCATION_ROTATION);
        GL33.glVertexAttribDivisor(LOCATION_ROTATION, 1);

        // Unbind VBO then VAO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * <p>Flips all the buffers in preparation for reading.</p>
     */
    private void flipBuffers()
    {
        mSelVertices.flip();
        mSelIndices.flip();
        mSelTexCoords.flip();
        mSelTexFlip.flip();
        mSelColors.flip();
        mSelProjections.flip();
        mSelTranslations.flip();
        mSelScales.flip();
        mSelRotation.flip();
    }

    /**
     * <p>Calls <i>glDrawElementsInstanced(...)</i></p>
     */
    public void draw()
    {
        GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0, mInstanceCount);
    }

    @Override
    public void clear() {
        // Reset instance count
        mInstanceCount = 0;

        // Treat buffers as empty for next drawing
        mSelIndices.clear();
        mSelTexFlip.clear();
        mSelColors.clear();
        mSelProjections.clear();
        mSelTranslations.clear();
        mSelScales.clear();
        mSelRotation.clear();
    }

    @Override
    public boolean isInstancing() {
        return mInstancing;
    }

    @Override
    public void setInstancing(boolean enable) {
        mInstancing = enable;

        // Use higher capacity batch buffers
        if (enable) {
            mSelVertices = mBatchVertices;
            mSelIndices = mBatchIndices;
            mSelTexCoords = mBatchTexCoords;
            mSelTexFlip = mBatchTexFlip;
            mSelColors = mBatchColors;
            mSelProjections = mBatchProjections;
            mSelTranslations = mBatchTranslations;
            mSelScales = mBatchScales;
            mSelRotation = mBatchRotation;

        } else {
            // Use single capacity buffers for single instance drawing
            mSelVertices = mVertices;
            mSelIndices = mIndices;
            mSelTexCoords = mTexCoords;
            mSelTexFlip = mTexFlip;
            mSelColors = mColors;
            mSelProjections = mProjections;
            mSelTranslations = mTranslations;
            mSelScales = mScales;
            mSelRotation = mRotation;
        }
    }

    @Override
    public void setProjection(float[] matrix)
    {
        mProjection = matrix.clone();
    }
}
