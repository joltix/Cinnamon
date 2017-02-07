package com.cinnamon.demo;

import com.cinnamon.gfx.ShaderFactory;
import com.cinnamon.gfx.ShaderLoader;
import com.cinnamon.gfx.ShaderObject;
import com.cinnamon.gfx.TextureLoader;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 *     Demo {@link ShaderFactory}.
 * </p>
 *
 *
 */
public class DemoShaderFactory extends ShaderFactory
{
    private static final String[] SHADERS = new String[] {"demo.vert",
            "demo_texture.frag", "demo_color.frag"};
    private static final String[] TEXTURES = new String[] {"demo_character.png",
            "demo_background.png"};

    private static final String SHADER_PATH = "com/cinnamon/demo/shaders";
    private static final String TEXTURE_PATH = "com/cinnamon/demo/images";

    public DemoShaderFactory()
    {
        super(Arrays.asList(SHADERS), Arrays.asList(TEXTURES));
    }

    @Override
    protected void loadFiles(ShaderMap shaders,
                             TextureMap textures,
                             List<String> shaderNames,
                             List<String> textureNames)
    {
        final ShaderLoader shaderLoader = new ShaderLoader(SHADER_PATH);
        shaderLoader.loadDirectory(shaderNames, shaders);

        final TextureLoader textureLoader = new TextureLoader(TEXTURE_PATH);
        textureLoader.loadDirectory(textureNames, textures);
    }

    @Override
    protected void onLoad(ShaderMap shaders)
    {
        final ShaderObject vObj = shaders.get("demo.vert");
        final ShaderObject colFragObj = shaders.get("demo_color.frag");
        createShader("demo_color", vObj, colFragObj);

        final ShaderObject texFragObj = shaders.get("demo_texture.frag");
        createShader("demo_texture", vObj, texFragObj);

        vObj.delete();
        colFragObj.delete();
        texFragObj.delete();
    }
}
