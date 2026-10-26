package org.devlive.metaforge.fs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
public class IOUtils
{
    private IOUtils()
    {
    }

    /**
     * Copies a file from the source path to the target path.
     *
     * @param source the path of the file to be copied
     * @param target the path where the file should be copied to
     * @param createdDir a flag indicating whether the parent directories of the target path should be created if they do not exist
     * @return true if the file was successfully copied, false otherwise
     */
    public static boolean copy(String source, String target, boolean createdDir)
    {
        try {
            Path targetPath = Paths.get(target);
            if (createdDir) {
                Files.createDirectories(targetPath.getParent());
            }

            Files.copy(Paths.get(source), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies the contents of the input stream to the specified target file path.
     *
     * @param stream the input stream containing the contents to be copied
     * @param target the path of the target file where the contents will be copied to
     * @param createdDir indicates whether the parent directory of the target file has been created
     * @return true if the contents were successfully copied, false otherwise
     */
    public static boolean copy(InputStream stream, String target, boolean createdDir)
            throws IOException
    {
        try {
            Path targetPath = Paths.get(target);
            if (createdDir) {
                Files.createDirectories(targetPath.getParent());
            }

            Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * This function takes a source file path and returns an InputStream object that can be used to read the contents of the file.
     *
     * @param source the path of the source file to read
     * @return an InputStream object representing the source file
     */
    public static InputStream reader(String source)
    {
        try {
            return Files.newInputStream(Paths.get(source), StandardOpenOption.READ);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes the file at the specified path.
     *
     * @param path the path of the file to be deleted
     * @return true if the file is successfully deleted, false otherwise
     */
    public static boolean delete(String path)
    {
        try {
            Files.delete(Paths.get(path));
            return true;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
