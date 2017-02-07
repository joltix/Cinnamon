package com.cinnamon.utils;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * <p>Loaders facilitate loading files from a directory. Each Loader is
 * responsible for loading from a singular directory. Target filenames are
 * required to find and load.</p>
 *
 *
 */
public abstract class Loader<E, U>
{
    // Path to enclosing directory
    private String mPathToDir;

    /**
     * <p>Constructor for loading files within a given directory.</p>
     *
     * @param path path to directory.
     */
    protected Loader(String path)
    {
        mPathToDir = path;
    }

    /**
     * <p>Gets the path to the target directory.</p>
     *
     * @return path.
     */
    public final String getPath()
    {
        return mPathToDir;
    }

    /**
     * <p>Loads a file into an Object representation.</p>
     *
     * @param name filename.
     * @return Object.
     * @throws IllegalArgumentException if the file could not be found.
     */
    public E load(String name) throws FileNotFoundException
    {
        final String path = formatPath(mPathToDir, name);
        final U link = access(path);

        if (link == null) {
            throw new FileNotFoundException("File \"" + path + "\" not " +
                    "found");
        }

        return assemble(name, link);
    }

    /**
     * <p>Loads all files within the {@link Loader}'s directory whose
     * filenames match with names from a given list.</p>
     *
     * @param files filenames.
     * @throws FileNotFoundException if a given filename was not found in the
     * directory.
     */
    protected void loadDirectory(List<String> files)
            throws FileNotFoundException
    {
        for (final String name : files) {

            // Combine filename with directory path
            final String path = formatPath(mPathToDir, name);

            // Open connection
            final U link = access(path);
            if (link == null) {
                throw new FileNotFoundException("File \"" + path + "\" not " +
                        "found");
            }

            // Defer to subclass for how to store
            onDirectoryLoad(name, assemble(name, link));
        }
    }

    /**
     * <p>This method is called each time a file has been loaded due to
     * calling {@link #loadDirectory(List)}. Subclass implementations should
     * handle storing the loaded object.</p>
     *
     * @param name filename.
     * @param object object.
     */
    protected abstract void onDirectoryLoad(String name, E object);

    /**
     * <p>Combines a filename with a path and may perform any needed
     * adjustments to form an appropriate file path.</p>
     *
     * @param path path to enclosing directory.
     * @param name filename.extension.
     * @return path to file.
     */
    protected abstract String formatPath(String path, String name);

    /**
     * <p>Provides a {@link U} through which data can be read.</p>
     *
     * @param path path to file (including file.extension).
     * @return source to read from.
     */
    protected abstract U access(String path);

    /**
     * <p>Constructs an Object to represent the data read from a file.</p>
     *
     * @param name filename with extension.
     * @param link access to data.
     * @return com.cinnamon.object representation.
     */
    protected abstract E assemble(String name, U link);
}
