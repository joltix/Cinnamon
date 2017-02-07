package com.cinnamon.utils;

import java.io.InputStream;
import java.util.List;

/**
 * <p>ResourceLoader loads resources by opening {@link InputStream}s.
 * Subclasses are responsible for assembling meaning objects from the
 * streams through their implementations of {@link #assemble(String, Object)}
 * and {@link #loadDirectory(List)}.</p>
 *
 *
 */
public abstract class ResourceLoader<E> extends Loader<E, InputStream>
{
    // ClassLoader for opening InputStreams
    private ClassLoader mClassLoader;

    /**
     * <p>Constructor for loading files from a directory.</p>
     *
     * @param path path to directory.
     */
    public ResourceLoader(String path)
    {
        super(path);
        mClassLoader = this.getClass().getClassLoader();
    }

    @Override
    protected String formatPath(String path, String name)
    {
        int lastIndex = path.length() - 1;
        char last = path.charAt(lastIndex);
        if (last != '/') {
            path = path + '/';
        }

        return path + name;
    }

    @Override
    protected InputStream access(String path)
    {
        return mClassLoader.getResourceAsStream(path);
    }
}
