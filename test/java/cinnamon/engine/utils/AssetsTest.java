package cinnamon.engine.utils;

import cinnamon.engine.utils.Assets.Assembler;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;

public class AssetsTest
{
    private static final String TEST_FILE_TEXT = "test_text.txt";

    // Test text file's expected contents; each String is an entire line
    private static final String[] TEST_FILE_TEXT_LINES = {"apple", "coconut", "orange", "melon"};

    private static final String FILE_PATH = "test/java/cinnamon/engine/utils/" + TEST_FILE_TEXT;

    private static final String RESOURCE_PATH = "/cinnamon/engine/utils/" + TEST_FILE_TEXT;

    @Test
    public void testLoadFile() throws IOException
    {
        final List<String> lines = Assets.loadFile(FILE_PATH, new StringListAssembler());

        Assert.assertArrayEquals(lines.toArray(), TEST_FILE_TEXT_LINES);
    }

    @Test (expected = NullPointerException.class)
    public void testLoadFileNPEPath() throws IOException
    {
        Assets.loadFile(null, new DummyAssembler());
    }

    @Test (expected = NullPointerException.class)
    public void testLoadFileNPEAssembler() throws IOException
    {
        Assets.loadFile(FILE_PATH, null);
    }

    @Test (expected = InvalidPathException.class)
    public void testLoadFileInvalidPathExceptionEmptyPath() throws IOException
    {
        Assets.loadFile("", new DummyAssembler());
    }

    @Test (expected = InvalidPathException.class)
    public void testLoadFileInvalidPathExceptionWhitespacePath() throws IOException
    {
        Assets.loadFile(" ", new DummyAssembler());
    }

    @Test (expected = FileNotFoundException.class)
    public void testLoadFileFileNotFoundException() throws IOException
    {
        final String brokenPath = FILE_PATH.substring(0, FILE_PATH.length() - 1);

        Assets.loadFile(brokenPath, new DummyAssembler());
    }

    @Test
    public void testLoadResource() throws IOException
    {
        final Object object = Assets.loadResource(RESOURCE_PATH, new DummyAssembler());

        Assert.assertSame(Object.class, object.getClass());
    }

    @Test (expected = NullPointerException.class)
    public void testLoadResourceNPEPath() throws IOException
    {
        Assets.loadResource(null, new DummyAssembler());
    }

    @Test (expected = NullPointerException.class)
    public void testLoadResourceNPEAssembler() throws IOException
    {
        Assets.loadResource(RESOURCE_PATH, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testLoadResourceIAEResourceNotFound() throws IOException
    {
        final String brokenPath = FILE_PATH.substring(0, FILE_PATH.length() - 1);

        Assets.loadResource(brokenPath, new DummyAssembler());
    }

    @Test (expected = InvalidPathException.class)
    public void testLoadResourceInvalidPathExceptionEmptyPath() throws IOException
    {
        Assets.loadResource("", new DummyAssembler());
    }

    @Test (expected = InvalidPathException.class)
    public void testLoadResourceInvalidPathExceptionWhitespacePath() throws IOException
    {
        Assets.loadResource(" ", new DummyAssembler());
    }

    private class DummyAssembler implements Assembler<Object>
    {
        @Override
        public Object assemble(InputStream stream) throws IOException
        {
            // Force possible IOException
            stream.readAllBytes();

            return new Object();
        }
    }

    /**
     * Assembles a {@code List} of {@code String}s such that each entry is a line of text from the stream.
     */
    private class StringListAssembler implements Assembler<List<String>>
    {
        @Override
        public List<String> assemble(InputStream stream) throws IOException
        {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            final List<String> list = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }

            reader.close();
            return list;
        }
    }
}
