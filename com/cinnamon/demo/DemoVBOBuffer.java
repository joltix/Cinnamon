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
 *
 *
 */
public final class DemoVBOBuffer extends GLBuffer
{
    // Number of floats per instance to be drawn
    private static final int SIZE_VERTEX = 3 * 4;
    private static final int SIZE_INDEX = 6;
    private static final int SIZE_TEX_COORDS = 2 * 4;
    private static final int SIZE_COLOR = 4;
    private static final int SIZE_MAT4 = 4 * 4;

    private float[] mProjection;
    private final int[] mConstantIndices;

    // Vertex array object
    private int mVAO;

    // Whether or not larger Buffers should be used
    private boolean mInstancing;

    // Number of instances added
    private int mInstanceCount;

    // VBO ids for vertices and indices
    private int mVBOPositions;
    private int mEBOIndices;

    // VBO ids for rest of vertex attributes
    private int mVBOTexCoords;
    private int mVBOColors;
    private int mVBOProjections;
    private int mVBOTranslations;

    // Data sources for individual drawing
    private final FloatBuffer mVertices;
    private final IntBuffer mIndices;
    private final FloatBuffer mTexCoords;
    private final FloatBuffer mColors;
    private final FloatBuffer mProjections;
    private final FloatBuffer mTranslations;

    // Data sources for instancing
    private final FloatBuffer mBatchVertices;
    private final IntBuffer mBatchIndices;
    private final FloatBuffer mBatchTexCoords;
    private final FloatBuffer mBatchColors;
    private final FloatBuffer mBatchProjections;
    private final FloatBuffer mBatchTranslations;

    // Currently selected data sources (to actually draw)
    private FloatBuffer mSelVertices;
    private IntBuffer mSelIndices;
    private FloatBuffer mSelTexCoords;
    private FloatBuffer mSelColors;
    private FloatBuffer mSelProjections;
    private FloatBuffer mSelTranslations;

    /**
     * <p>Constructor for a DemoVBOBuffer.</p>
     *
     * @param capacity max number of instances allowed at a time.
     * @param projection projection matrix for all instances.
     * @param indices indices for all instances' triangles.
     */
    public DemoVBOBuffer(int capacity, float[] projection, int[] indices)
    {
        // Make sure capacity is for drawing > 1
        if (capacity <= 1) {
            throw new IllegalArgumentException("PooledBatch capacity must be <= 1");
        }

        // Defensive copy projection for each instance added
        mProjection = projection.clone();
        mConstantIndices = indices;

        // Generate vertex array object
        mVAO = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(mVAO);

        // Generate vertices VBOs
        mVBOPositions = GL15.glGenBuffers();
        mEBOIndices = GL15.glGenBuffers();

        // Generate vertex attrib VBOs
        mVBOTexCoords = GL15.glGenBuffers();
        mVBOColors = GL15.glGenBuffers();
        mVBOProjections = GL15.glGenBuffers();
        mVBOTranslations = GL15.glGenBuffers();

        // Init single draw sources
        mVertices = BufferUtils.createFloatBuffer(SIZE_VERTEX);
        mIndices = BufferUtils.createIntBuffer(SIZE_INDEX);
        mTexCoords = BufferUtils.createFloatBuffer(SIZE_TEX_COORDS);
        mColors = BufferUtils.createFloatBuffer(SIZE_COLOR);
        mProjections = BufferUtils.createFloatBuffer(SIZE_MAT4);
        mTranslations = BufferUtils.createFloatBuffer(SIZE_MAT4);

        // Init batch draw sources
        mBatchVertices = BufferUtils.createFloatBuffer(capacity *
                SIZE_VERTEX);
        mBatchIndices = BufferUtils.createIntBuffer(capacity * SIZE_INDEX);
        mBatchTexCoords = BufferUtils.createFloatBuffer(capacity *
                SIZE_TEX_COORDS);
        mBatchColors = BufferUtils.createFloatBuffer(capacity * SIZE_COLOR);
        mBatchProjections = BufferUtils.createFloatBuffer(capacity * SIZE_MAT4);
        mBatchTranslations = BufferUtils.createFloatBuffer(capacity *
                SIZE_MAT4);

        // Set single draw sources as default
        mSelVertices = mVertices;
        mSelIndices = mIndices;
        mSelTexCoords = mTexCoords;
        mSelColors = mColors;
        mSelProjections = mProjections;
        mSelTranslations = mTranslations;

        // Allocate memory ahead of time
        allocMemory();
    }

    /**
     * <p>Allocates memory ahead of time by buffering the batch sized
     * buffers.</p>
     */
    private void allocMemory()
    {
        // Bind vertices and upload
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOPositions);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchVertices,
                GL15.GL_DYNAMIC_DRAW);

        // Bind element buffer object and allocate indices memory
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, mEBOIndices);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, mBatchIndices, GL15.GL_STATIC_DRAW);

        // Specify vertices (position) layout
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 4 * 3, 0);
        GL20.glEnableVertexAttribArray(0);

        // Allocate memory for texture coords
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOTexCoords);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchTexCoords, GL15.GL_DYNAMIC_DRAW);

        // Specify texture coords layout
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 2, 0);
        GL20.glEnableVertexAttribArray(1);

        // Bind colors
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOColors);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchColors,
                GL15.GL_DYNAMIC_DRAW);

        // Specify colors layout
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, 4 * 4, 0);
        GL20.glEnableVertexAttribArray(2);

        // Bind projection matrices
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOProjections);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchProjections, GL15.GL_DYNAMIC_DRAW);

        // Specify projection matrix layout
        this.attribPointerMat4(3,SIZE_MAT4 * 4, 0);
        GL20.glEnableVertexAttribArray(3);

        // Bind translation matrices
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOTranslations);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mBatchTranslations,
                GL15.GL_DYNAMIC_DRAW);

        // Specify translation matrix layout
        this.attribPointerMat4(7,SIZE_MAT4 * 4, 0);
        GL20.glEnableVertexAttribArray(7);

        // Unbind VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * <p>Adds drawing data for one instance to the buffer.</p>
     *
     * @param vertices vertices.
     * @param texCoords texture coordinates.
     * @param colors colors.
     * @param translations translation matrix.
     */
    public void add(float[] vertices, float[] texCoords, float[] colors,
                    float[] translations)
    {
        mSelVertices.put(vertices);
        mSelIndices.put(mConstantIndices);
        mSelTexCoords.put(texCoords);
        mSelColors.put(colors);
        mSelProjections.put(mProjection);
        mSelTranslations.put(translations);

        // Count instance (for instanced drawing)
        mInstanceCount++;
    }

    @Override
    public void flush() {
        // Prep for reading
        flipBuffers();

        // Upload vertices vertices
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOPositions);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelVertices);
        // Upload indices
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, mEBOIndices);
        GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, mSelIndices);

        final int locVertices = 0;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOPositions);
        GL20.glEnableVertexAttribArray(locVertices);
        GL33.glVertexAttribDivisor(locVertices, 0);


        // Format texture coordinates
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOTexCoords);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelTexCoords);
        final int locTex = 1;
        GL20.glEnableVertexAttribArray(locTex);
        GL33.glVertexAttribDivisor(locTex, 0);


        // Format colors
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOColors);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelColors);
        final int locColors = 2;
        GL20.glEnableVertexAttribArray(locColors);
        GL33.glVertexAttribDivisor(locColors, 1);


        // Format projections
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOProjections);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelProjections);
        final int locProj = 3;
        this.enableVertexAttribArrayMat4(locProj);
        this.vertexAttribDivisorMat4(locProj, 1);


        // Format translations
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mVBOTranslations);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, mSelTranslations);
        final int locTrans = 7;
        this.enableVertexAttribArrayMat4(locTrans);
        this.vertexAttribDivisorMat4(locTrans, 1);

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
        mSelColors.flip();
        mSelProjections.flip();
        mSelTranslations.flip();
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
        mInstanceCount = 0;
        mSelVertices.clear();
        mSelIndices.clear();
        mSelTexCoords.clear();
        mSelColors.clear();
        mSelProjections.clear();
        mSelTranslations.clear();
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
            mSelColors = mBatchColors;
            mSelProjections = mBatchProjections;
            mSelTranslations = mBatchTranslations;

        } else {
            // Use single capacity buffers for single instance drawing
            mSelVertices = mVertices;
            mSelIndices = mIndices;
            mSelTexCoords = mTexCoords;
            mSelColors = mColors;
            mSelProjections = mProjections;
            mSelTranslations = mTranslations;
        }
    }

    @Override
    public void setProjection(float[] matrix)
    {
        mProjection = matrix.clone();
    }
}
