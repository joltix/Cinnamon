package com.cinnamon.gfx;


import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 *     ShaderFactory initiates shader source file loading, texture loading,
 *     and provides lookup for said resources. Subclasses may designate their
 *     own {@link ShaderProgram}s to be linked by overriding
 *     {@link #onLoad(ShaderMap)}.
 * </p>
 *
 *
 */
public abstract class ShaderFactory
{
    // ShaderProgram lookup map
    private final HashMap<String, ShaderProgram> mShaderMap = new
            HashMap<String, ShaderProgram>();

    // ShaderObject storage
    private final Object mShaderLock = new Object();
    private final ShaderMap mShaderObjMap;

    // List of known shader source filenames
    private final ArrayList<String> mShaderFiles;

    // Texture storage
    private final Object mTexLock = new Object();
    private final TextureMap mTextureMap;

    // List of known texture filenames
    private final ArrayList<String> mTextureFiles;

    // Whether or not gfx resources are ready for use
    private volatile boolean mLoaded = false;

    /**
     * <p>Constructor for a ShaderFactory.</p>
     *
     * @param shaderFilenames shader source files to load.
     * @param textureFilenames texture images files to load.
     */
    protected ShaderFactory(List<String> shaderFilenames, List<String>
            textureFilenames)
    {
        // Defensive copy shader and texture file names
        mShaderFiles = new ArrayList<String>(shaderFilenames);
        mTextureFiles = new ArrayList<String>(textureFilenames);

        // Create shader obj and texture lookups
        mShaderObjMap = new ShaderMap();
        mTextureMap = new TextureMap();
    }

    /**
     * <p>Loads all ShaderFactory files and resources.</p>
     */
    public final void load()
    {
        // Load files/resources
        loadFiles(mShaderObjMap, mTextureMap, mShaderFiles, mTextureFiles);

        // Defer ShaderProgram linking to subclasses
        onLoad(mShaderObjMap);
        mLoaded = true;
    }

    /**
     * <p>Loads all shader source and texture files.</p>
     *
     * @param shaders {@link ShaderObject} lookup for storage.
     * @param textures {@link Texture} lookup for storage.
     * @param shaderNames shader source filenames.
     * @param textureNames texture filenames.
     */
    protected abstract void loadFiles(ShaderMap shaders, TextureMap textures,
                                      List<String> shaderNames,
                                      List<String> textureNames);

    /**
     * <p>Called when {@link #load()} is called, after OpenGL has been
     * initialized and OpenGL's functions are available. This method should be
     * used to assemble {@link ShaderProgram}s for retrieval by the
     * {@link Canvas}.</p>
     *
     * @param shaders {@link ShaderObject} lookup for assembling ShaderPrograms.
     */
    protected abstract void onLoad(ShaderMap shaders);

    /**
     * <p>Checks whether or not the {@link ShaderFactory} has completed loading
     * {@link ShaderObject}s, {@link ShaderProgram}s, and {@link Texture}s.</p>
     *
     * @return true if gfx assets are ready to use.
     */
    public final boolean isLoaded()
    {
        return mLoaded;
    }

    /**
     * <p>Creates a {@link ShaderProgram} and stores a reference retrievable
     * by the given name through {@link #getShader(String)}.</p>
     *
     * @param name shader name.
     * @param vertex vertex shader object.
     * @param fragment fragment shader object.
     * @throws IllegalArgumentException if a ShaderProgram is already
     * associated with the desired name key.
     */
    protected final int createShader(String name, ShaderObject vertex,
                                      ShaderObject fragment)
    {
        int progId;
        synchronized (mShaderLock) {
            ShaderProgram prog = mShaderMap.get(name);
            if (prog != null) {
                throw new IllegalArgumentException("Shader program with name \""
                        + name + "\" already exists");
            }

            // Assemble the shader program
            prog = new ShaderProgram(vertex, fragment);

            progId = prog.getId();

            // Store shader for reference
            mShaderMap.put(name, prog);
        }

        return progId;
    }

    /**
     * <p>Gets a {@link List} of known shader source files.</p>
     *
     * @return shader filenames.
     */
    public final List<String> getShaderNames()
    {
        return mShaderFiles;
    }

    /**
     * <p>Gets the {@link ShaderProgram} associated with a given name.</p>
     *
     * @param name shader name.
     * @return shaer.
     */
    public final ShaderProgram getShader(String name)
    {
        synchronized (mShaderLock) {
            return mShaderMap.get(name);
        }
    }

    /**
     * <p>Gets the {@link Texture} associated with the given id.</p>
     *
     * @param id Texture id.
     * @return Texture.
     */
    public final Texture getTexture(int id)
    {
        synchronized (mTexLock) {
            return (id == Texture.NULL) ? null : mTextureMap.get(id);
        }
    }

    /**
     * <p>Gets the {@link Texture} associated with the given name.</p>
     *
     * @param name Texture name.
     * @return Texture.
     */
    public final Texture getTexture(String name)
    {
        synchronized (mTexLock) {
            return mTextureMap.get(name);
        }
    }

    /**
     * <p>Deletes all OpenGL assigned ids and clears references to all
     * {@link Texture}s, {@link ShaderObject}s, and {@link ShaderProgram}s.</p>
     */
    public final void clear()
    {
        synchronized (mShaderLock) {

            // Delete and clear ShaderObjects
            mShaderObjMap.clear();

            // Delete ShaderPrograms
            for (ShaderProgram prog : mShaderMap.values()) {
                prog.delete();
            }

            // Clear ShaderProgram lookup refs
            mShaderMap.clear();
        }

        synchronized (mTexLock) {
            // Delete and clear Textures
            mTextureMap.clear();
        }
    }

    /**
     * <p>
     *     ShaderMap exposes {@link ShaderObject} lookup to
     *     {@link ShaderFactory} subclasses while preventing the removal of
     *     ShaderObjects.
     * </p>
     */
    protected class ShaderMap
    {
        // ShaderObject lookup
        private final HashMap<String, ShaderObject> mNameMap = new HashMap<String,
         ShaderObject>();

        /**
         * <p>Constructor for a ShaderMap.</p>
         */
        public ShaderMap()
        {
        }

        /**
         * <p>Gets the {@link ShaderObject} associated with the given name.</p>
         *
         * @param name ShaderObject name.
         * @return ShaderObject.
         */
        public ShaderObject get(String name)
        {
            return mNameMap.get(name);
        }

        /**
         * <p>Stores a {@link ShaderObject} with a name.</p>
         *
         * @param name name.
         * @param object ShaderObject.
         */
        void put(String name, ShaderObject object)
        {
            mNameMap.put(name, object);
        }

        /**
         * <p>Gets the number of stored {@link ShaderObject}s.</p>
         *
         * @return ShaderObject count.
         */
        public int size()
        {
            return mNameMap.size();
        }

        /**
         * <p>Clears all {@link ShaderObject}s.</p>
         */
        private void clear()
        {
            // Delete shader obj ids
            for (final ShaderObject shader : mNameMap.values()) {
                shader.delete();
            }

            // Clear lookup map
            mNameMap.clear();
        }
    }

    /**
     * <p>
     *     TextureMap exposes {@link Texture} lookup to {@link ShaderFactory}
     *     subclasses while preventing the removal of the Textures.
     * </p>
     */
    protected class TextureMap
    {
        // Texture name and id lookup maps
        private final HashMap<String, Texture> mNameMap = new HashMap<String,
                Texture>();
        private final HashMap<Integer, Texture> mIdMap = new HashMap<Integer,
         Texture>();

        /**
         * <p>Constructor for a TextureMap.</p>
         */
        public TextureMap()
        {
        }

        /**
         * <p>Gets the {@link Texture} associated with the given name.</p>
         *
         * @param name Texture name.
         * @return Texture.
         */
        public Texture get(String name)
        {
            return mNameMap.get(name);
        }

        /**
         * <p>Gets the {@link Texture} associated with the given Texture id.</p>
         *
         * @param id Texture id.
         * @return Texture.
         */
        public Texture get(int id)
        {
            return mIdMap.get(id);
        }

        /**
         * <p>Stores a {@link Texture} with a name.</p>
         *
         * @param name name.
         * @param texture Texture.
         */
        void put(String name, Texture texture)
        {
            mNameMap.put(name, texture);
            mIdMap.put(texture.getId(), texture);
        }

        /**
         * <p>Gets the number of stored {@link Texture}s.</p>
         *
         * @return Texture count.
         */
        public int size()
        {
            return mIdMap.size();
        }

        /**
         * <p>Clears all {@link Texture}s.</p>
         */
        private void clear()
        {
            // Delete the teture ids and clear Texture references
            for (final Texture texture : mIdMap.values()) {
                GL11.glDeleteTextures(texture.getId());
            }

            // Clear lookup maps
            mNameMap.clear();
            mIdMap.clear();
        }
    }
}
