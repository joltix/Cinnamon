package com.cinnamon.gfx;

import com.cinnamon.utils.ResourceLoader;

import java.io.*;
import java.util.List;

/**
 * <p>
 *     ShaderLoader loads shader source files from a jar's resources and
 *     constructs {@link ShaderObject} to be later linked for OpenGL use as
 *     {@link ShaderProgram}s.
 * </p>
 *
 *
 */
public final class ShaderLoader extends ResourceLoader<ShaderObject>
{
    // Filename extension for vertex shaders
    private static final String EXTENSION_VERTEX = ".vert";

    // Read only map for vertex and fragment shaders
    private ShaderFactory.ShaderMap mShaderObjMap;

    /**
     * <p>Constructor for a ShaderLoader.</p>
     *
     * @param path path to shader directory.
     */
    public ShaderLoader(String path)
    {
        super(path);
    }

    @Override
    protected ShaderObject assemble(String name, InputStream stream)
    {
        String source = extractSource(stream);

        // Missing file extension
        int dotIndex = name.indexOf('.');
        if (dotIndex == -1) {
            throw new IllegalStateException("Invalid filename: " +
                    name);
        }

        // Match to ShaderObject.Action
        ShaderObject.Type type;
        String extension = name.substring(dotIndex, name.length());
        if (extension.equalsIgnoreCase(EXTENSION_VERTEX)) {
            type = ShaderObject.Type.VERTEX;
        } else {
            type = ShaderObject.Type.FRAGMENT;
        }

        // Create vertex or fragment shader depending on file type
        String shortName = name.substring(0, dotIndex);

        // Close stream then construct shader
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ShaderObject(shortName, source, type);
    }

    /**
     * <p>Reads the shader's {@link InputStream} and returns its source as a
     * {@link String}.</p>
     *
     * @param stream shader file InputStream.
     * @return GLSL source.
     * @throws RuntimeException if the shader file couldn't be read.
     */
    private String extractSource(InputStream stream)
    {
        StringBuilder builder = new StringBuilder();

        try {
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader buffReader = new BufferedReader(streamReader);

            String line;
            while ((line = buffReader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }

            buffReader.close();
            streamReader.close();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not read from file \"" +
                    stream.toString() + "\"");
        }

        return builder.toString();
    }

    /**
     * <p>Loads all {@link ShaderObject}s in the directory whose filename is
     * on a given list. If a file on the list is not found in the directory,
     * this method continues on to attempt to load the next filename.</p>
     *
     * @param filenames image filenames to load.
     * @param shaders container for loaded Textures.
     */
    public void loadDirectory(List<String> filenames, ShaderFactory.ShaderMap
            shaders)
    {
        try {
            mShaderObjMap = shaders;

            super.loadDirectory(filenames);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDirectoryLoad(String name, ShaderObject object)
    {
        mShaderObjMap.put(name, object);
    }
}
