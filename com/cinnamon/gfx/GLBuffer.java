package com.cinnamon.gfx;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;

/**
 * <p>
 *     A GLBuffer helps organize VBOs when drawing in a {@link Canvas}. The
 *     primary purpose is to abstract some OpenGL operations and pull some
 *     code out of the {@link Canvas}' space.
 * </p>
 *
 *
 */
public abstract class GLBuffer
{
    /**
     * <p>Pushes data from internal buffers to OpenGL.</p>
     */
    public abstract void flush();

    /**
     * <p>Reset the buffer to a non-written state and prepare to begin
     * writing new drawing info from the beginning.</p>
     */
    public abstract void clear();

    /**
     * <p>Checks whether or not the buffer is prepared for instanced drawing
     * .</p>
     *
     * @return true if the buffer is doing instanced drawing,
     */
    public abstract boolean isInstancing();

    /**
     * <p>Sets whether or not to use instanced drawing.</p>
     *
     * @param enable true to enable instancing.
     */
    public abstract void setInstancing(boolean enable);

    /**
     * <p>Enables a vertex attribute array at the specified location index
     * set up for a 4x4 matrix.</p>
     *
     * <p>This method is the same as calling
     * <i>glEnableVertexAttribArray(location)</i> four times with
     * incrementing location indices.</p>
     *
     * @param location location index from shader.
     */
    protected void enableVertexAttribArrayMat4(int location)
    {
        for (int i = 0; i < 4; i++) {
            GL20.glEnableVertexAttribArray(location + i);
        }
    }

    /**
     * <p>Specifies whether or not a vertex attribute array setup for a 4x4
     * matrix should be set to change per vertex, per instance, or more.</p>
     *
     * <p>This method is the same as calling <i>glVertexxAttribDivisor
     * (location, divisor)</i> four times with incrementing location indices
     * .</p>
     *
     * @param location location index.
     * @param divisor divisor.
     */
    protected void vertexAttribDivisorMat4(int location, int divisor)
    {
        for (int i = 0; i < 4; i++) {
            GL33.glVertexAttribDivisor(location + i, divisor);
        }
    }

    /**
     * <p>Specifies the layout of data in a vertex attribute array setup for
     * a 4x4 matrix.</p>
     *
     * <p>This method is the same as calling <i>glVertexAttribPointer
     * (location, typeSize, type, normalized, stride, offset)</i> four times
     * but with incrementing location indices to represent each vector in
     * the matrix. This method assumes all values are floats and the matrix
     * is the only kind of data in the VBO.</p>
     *
     * @param location location index.
     * @param stride stride.
     * @param offset offset.
     */
    protected void attribPointerMat4(final int location, final int stride,
                                   final int offset)
    {
        GL20.glVertexAttribPointer(location, 4, GL11.GL_FLOAT, false, stride,
                offset);
        GL20.glVertexAttribPointer(location + 1, 4, GL11.GL_FLOAT, false,
                stride, offset + 16);
        GL20.glVertexAttribPointer(location + 2, 4, GL11.GL_FLOAT, false,
                stride, offset + 32);
        GL20.glVertexAttribPointer(location + 3, 4, GL11.GL_FLOAT, false,
                stride, offset + 48);
    }
}
