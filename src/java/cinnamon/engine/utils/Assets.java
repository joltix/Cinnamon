package cinnamon.engine.utils;

import java.io.*;
import java.nio.file.*;

/**
 * Loads files and resources.
 */
public final class Assets
{
    /**
     * Returns an array filled with all of a stream's bytes.
     */
    public static Assembler<byte[]> BYTE_ARRAY = InputStream::readAllBytes;

    // Opens resource streams
    private static final ClassLoader mLoader = Assets.class.getClassLoader();

    // Formats file paths
    private static final FileSystem mSystem = FileSystems.getDefault();

    private Assets() { }

    /**
     * Loads an object from a file.
     *
     * @param path file path.
     * @param assembler object building instructions.
     * @param <T> output object type.
     * @return object.
     * @throws NullPointerException if path or assembler is null.
     * @throws InvalidPathException if path cannot be used as a file path.
     * @throws IOException if an I/O error occurs while attempting to read the file.
     * @throws FileNotFoundException if the file cannot be found.
     * @throws SecurityException if the security manager will not allow the file to be read.
     */
    public static <T> T loadFile(String path, Assembler<T> assembler) throws IOException, FileNotFoundException
    {
        checkNotNull(path);
        checkNotNull(assembler);
        checkBlankString(path);

        final Path p = mSystem.getPath(path);
        final File file = p.toFile();

        checkFileExists(file);
        checkFileIsReadable(file);

        return assembler.assemble(Files.newInputStream(p));
    }

    /**
     * Loads an object from a resource.
     *
     * @param path resource path.
     * @param assembler object building instructions.
     * @param <T> output object type.
     * @return object.
     * @throws NullPointerException if path or assembler is null.
     * @throws IllegalArgumentException if path does not refer to a resource or the resource cannot be opened.
     * @throws InvalidPathException if path cannot be used as a resource path.
     * @throws IOException if an I/O error occurs while attempting to read the resource.
     */
    public static <T> T loadResource(String path, Assembler<T> assembler) throws IOException
    {
        checkNotNull(path);
        checkNotNull(assembler);
        checkBlankString(path);

        final String fullPath = FileSystems.getDefault().getPath(path).toString();
        final InputStream stream = mLoader.getResourceAsStream(fullPath);

        checkResourceExists(stream, fullPath);

        return assembler.assemble(stream);
    }

    /**
     * This method exists alongside {@link #checkFileIsReadable(File)} to potentially throw
     * {@code FileNotFoundException}. Even though {@code checkFileIsReadable(File)} can detect a file's
     * non-existence, that method's purpose is to detect the file's readability and to throw a general
     * {@code IOException}.
     */
    private static void checkFileExists(File file) throws FileNotFoundException
    {
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
    }

    private static void checkFileIsReadable(File file) throws IOException
    {
        if (!file.canRead()) {
            throw new IOException();
        }
    }

    /**
     * Unlike {@link #checkFileExists(File)}, this method throws an {@link IllegalArgumentException} instead of a
     * {@link FileNotFoundException} when the target asset is not found. This is because a resource is expected to be
     * found within the project's jar where a user should know the correct path. While the {@code java.io} APIs opt to
     * return {@code null} for invalid resources, here it is treated as a mistaken argument.
     *
     * @param stream resource stream.
     * @param path resource path.
     * @throws IllegalArgumentException if stream is null.
     */
    private static void checkResourceExists(InputStream stream, String path)
    {
        if (stream == null) {
            final String format = "%s does not refer to a resource";
            throw new IllegalArgumentException(String.format(format, path));
        }
    }

    private static void checkBlankString(String string)
    {
        if (string.trim().isEmpty()) {
            throw new InvalidPathException(string, "Empty string or string of spaces should not be used as a path");
        }
    }

    private static void checkNotNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Assembles an object from an {@code InputStream}.
     *
     * @param <T> output object.
     */
    public interface Assembler<T>
    {
        /**
         * Assembles an object from a stream.
         *
         * @param stream input.
         * @return output object.
         * @throws IOException if an I/O error occurs while reading from the stream.
         */
        T assemble(InputStream stream) throws IOException;
    }
}
