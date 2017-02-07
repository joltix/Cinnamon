package com.cinnamon.gfx;

import com.cinnamon.utils.Identifiable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * <p>
 *     Represents a shader program combining a vertex shader and fragment
 *     shader for drawing operations. Both vertex and fragment are detached upon
 *     the ShaderProgram's construction.
 * </p>
 */
public final class ShaderProgram implements Identifiable
{
    // OpenGL assigned id
    private int mId = -1;

    /**
     * <p>Assigns the vertex and fragment shaders that make up the shader
     * program.</p>
     *
     * @param vertShader vertex shader.
     * @param fragShader fragment shader.
     * @throws IllegalStateException if the shader program fails to link.
     */
    public ShaderProgram(ShaderObject vertShader, ShaderObject fragShader)
    {
        // Record new program instance
        mId = GL20.glCreateProgram();

        // Assemble components
        final int vId = vertShader.getId();
        final int fId = fragShader.getId();
        GL20.glAttachShader(mId, vId);
        GL20.glAttachShader(mId, fId);
        GL30.glBindFragDataLocation(mId, 0, "outColor");
        GL20.glLinkProgram(mId);

        // Validate
        checkForErrors(mId);

        // Detach shaders in case they need to be reused with another
        vertShader.detach(mId);
        fragShader.detach(mId);
    }


    /**
     * <p>Checks for errors in the program linking.</p>
     *
     * @param id program id.
     * @throws IllegalStateException if the shader program fails to link.
     */
    private void checkForErrors(int id)
    {
        final int linked = GL20.glGetProgrami(id, GL20.GL_LINK_STATUS);
        if (linked == GL11.GL_FALSE) {
            throw new IllegalStateException("ShaderObject program \"" + id +
                    "\" failed to link: " + GL20.glGetProgramInfoLog(id));
        }
    }


    /**
     * <p>Sets a uniform property of a given name with a 4x4 matrix of
     * values.</p>
     *
     * @param property uniform's name.
     * @param values values.
     * @param transpose true to transpose the values' matrix.
     */
    public void setMatrix4f(String property, float[] values,
                            boolean transpose)
    {
        final int location = GL20.glGetUniformLocation(this.getId(), property);
        GL20.glUniformMatrix4fv(location, transpose, values);
    }


    /**
     * <p>Sets a uniform property of a given name with a 3 dimension vector of
     * floats.</p>
     *
     * @param property uniform's name.
     * @param f0 value 1.
     * @param f1 value 2.
     * @param f2 value 3.
     */
    public void setVector3f(String property, float f0, float f1, float f2)
    {
        final int location = GL20.glGetUniformLocation(this.getId(), property);
        GL20.glUniform3f(location, f0, f1, f2);
    }

    /**
     * <p>Sets a uniform property of a given name with a 4 dimension vector
     * of floats.</p>
     *
     * @param property uniform's name.
     * @param f0 value 1.
     * @param f1 value 2.
     * @param f2 value 3.
     * @param f3 value 4.
     */
    public void setVector4f(String property, float f0, float f1, float f2,
                            float f3)
    {
        final int location = GL20.glGetUniformLocation(this.getId(), property);
        GL20.glUniform4f(location, f0, f1, f2, f3);
    }

    /**
     * <p>Sets a uniform property of a given name with a boolean.</p>
     *
     * @param property uniform's name.
     * @param value boolean.
     */
    public void setBoolean(String property, boolean value)
    {
        final int location = GL20.glGetUniformLocation(this.getId(), property);
        GL20.glUniform1i(location, (value) ? 1 : 0);
    }

    /**
     * <p>Sets a uniform property with a boolean.</p>
     *
     * @param location uniform index.
     * @param value true or false.
     */
    public void setBoolean(int location, boolean value)
    {
        GL20.glUniform1i(location, (value) ? 1: 0);
    }

    /**
     * <p>Sets the ShaderProgram to be used for the current drawing
     * operations.</p>
     */
    public void use()
    {
        GL20.glUseProgram(this.getId());
    }


    /**
     * <p>Deletes the ShaderProgram.</p>
     *
     * <p>This method is the same as calling glDeleteProgram(program).</p>
     */
    public void delete()
    {
        GL20.glDeleteProgram(this.getId());
    }

    @Override
    public int getId() {
        return mId;
    }
}