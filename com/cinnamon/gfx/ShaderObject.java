package com.cinnamon.gfx;

import com.cinnamon.utils.Identifiable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * <p>
 *     Represents an OpenGL shader object.
 * </p>
 */
public class ShaderObject implements Identifiable
{
    /**
     * Constants representing shader types.
     */
    public enum Type
    {
        /**
         * Constant for a vertex shader.
         */
        VERTEX,

        /**
         * Constant for a fragment shader.
         */
        FRAGMENT
    }

    // VERTEX or FRAGMENT (more to come)
    protected Type mType;

    // ShaderObject obj id given via OpenGL and associated source code
    private int mId = 0;
    private String mName;
    private String mSource;

    /**
     * <p>Constructor for initializing a shader with its source code.</p>
     *
     * @param filename source filename.
     * @param source GLSL source.
     * @param type {@link ShaderObject.Type#VERTEX} or
     * {@link ShaderObject.Type#FRAGMENT}.
     */
    public ShaderObject(String filename, String source, Type type)
    {
        mType = type;
        mName = filename;
        mSource = source;
        createShaderObject(filename);
    }

    /**
     * <p>Creates the shader object and compiles the GLSL source.</p>
     *
     * @throws IllegalStateException if the shader's source fails to compile.
     */
    private void createShaderObject(String filename)
    {
        // Create specific kind of shader obj
        switch (mType) {
            case VERTEX:
                mId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER); break;
            case FRAGMENT:
                mId = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER); break;
            default: throw new IllegalStateException("Failed to resolve " +
                    "shader type");
        }

        // Compile GLSL
        GL20.glShaderSource(mId, mSource);
        GL20.glCompileShader(mId);

        // Check for compilation issues
        int compiled = GL20.glGetShaderi(mId, GL20.GL_COMPILE_STATUS);
        if (compiled == GL11.GL_FALSE) {
            throw new IllegalStateException("ShaderObject \"" + filename + "\" " +
                    "failed to compile with file(" + mName + "): "
                    + GL20.glGetShaderInfoLog (mId));
        }
    }

    /**
     * <p>Detaches the ShaderObject from an attached {@link ShaderProgram}</p>
     *
     * <p>This method is the same as calling glDetachShader(shaderProgram,
     * shaderObj)</p>
     *
     * @param program {@link ShaderProgram} id.
     */
    public void detach(int program)
    {
        GL20.glDetachShader(program, mId);
    }

    /**
     * <p>Deletes the shader program. While this ShaderObject may still retain its
     * GLSL source, the shader id will become 0.</p>
     */
    public void delete()
    {
        GL20.glDeleteShader(mId);
        mId = 0;
    }

    @Override
    public int getId()
    {
        return mId;
    }

    /**
     * <p>Gets the name of the source file used to generate the shader.</p>
     *
     * @return source's filename.
     */
    public String getName()
    {
        return mName;
    }

    /**
     * <p>Gets the GLSL source used in this ShaderObject's creation.</p>
     *
     * @return source code.
     */
    public String getSource()
    {
        return mSource;
    }

    /**
     * <p>Gets the type of shader, either {@link ShaderObject.Type#VERTEX} or
     * {@link ShaderObject.Type#FRAGMENT}.</>
     *
     * @return kind of shader.
     */
    public Type getType()
    {
        return mType;
    }

    /**
     * <p>Checks whether or not the ShaderObject is of a specific
     * {@link ShaderObject.Type}.</p>
     *
     * @param type either {@link Type#VERTEX} or {@link Type#FRAGMENT}.
     * @return true if the Type matches.
     */
    public boolean isType(Type type)
    {
        return mType == type;
    }
}
