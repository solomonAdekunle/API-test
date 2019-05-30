package helpers;

import com.ec.sitemap.FileTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHelper {

    public static final String SITEMAP_OUTPUT_ROOT = System.getProperty("user.dir") + "/src/test/resources/sitemaps/tmp/output/files";

    private static Path tempPath;
    private static Path tempSitemapPath;
    private static Path tempSitemapFilesPath;

    public static Path getTempPath() {
        if(tempPath == null) {
            try {
                tempPath = Files.createTempDirectory("indexablefilters-acceptancetests");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return tempPath;
    }

    public static Path getTempSitemapPath() {
        if(tempSitemapPath == null) {
            try {
                tempSitemapPath = Files.createTempDirectory(getTempPath(), "sitemap");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return tempSitemapPath;
    }

    public static Path getTempSitemapFilesPath() {
        if(tempSitemapFilesPath == null) {
            try {
                tempSitemapFilesPath = Files.createTempDirectory(getTempSitemapPath(), "files");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return tempSitemapFilesPath;
    }

    public static void removeTemp() {
        if(tempPath != null) {
            FileTestUtils.removeDir(tempPath.toFile());
        }
    }
}
