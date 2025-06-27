package uk.gov.gchq.magma.xml.test.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simple configuration class for project paths and settings
 */
public class ProjectConfig {
    
    // Base paths
    private static final String PROJECT_ROOT = "C:\\MC_project_1\\mc-xml-test";
    private static final String DATA_ROOT = "C:\\Users\\INQUSIV_Desktop\\OneDrive - Inqusiv\\Projects\\Uni\\PlosOne";
    
    // Project directories
    public static final Path PROJECT_PATH = Paths.get(PROJECT_ROOT);
    public static final Path DATA_PATH = Paths.get(DATA_ROOT);
    public static final Path OUTPUT_PATH = PROJECT_PATH.resolve("data").resolve("output");
    public static final Path KEYWORDS_PATH = PROJECT_PATH.resolve("data").resolve("keywords");
    
    // File extensions to process
    public static final String[] XML_EXTENSIONS = {".xml"};
    public static final String[] TEXT_EXTENSIONS = {".txt", ".pdf"};
    
    // Keywords file
    public static final String EXPLOSIVES_KEYWORDS_FILE = "explosives_keywords.txt";
    
    // Processing settings
    public static final int MAX_FILES_TO_PROCESS = 100; // Start small for testing
    public static final boolean ENABLE_CUDA = true; // We'll use this later for ML features
    
    /**
     * Get the full path to the explosives keywords file
     */
    public static Path getExplosivesKeywordsPath() {
        return KEYWORDS_PATH.resolve(EXPLOSIVES_KEYWORDS_FILE);
    }
    
    /**
     * Get output file path with timestamp
     */
    public static Path getTimestampedOutputPath(String filename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String name = filename.substring(0, filename.lastIndexOf('.'));
        String ext = filename.substring(filename.lastIndexOf('.'));
        return OUTPUT_PATH.resolve(name + "_" + timestamp + ext);
    }
    
    /**
     * Simple validation that paths exist
     */
    public static boolean validatePaths() {
        return DATA_PATH.toFile().exists() && 
               PROJECT_PATH.toFile().exists();
    }
    
    /**
     * Create necessary directories if they don't exist
     */
    public static void createDirectories() {
        OUTPUT_PATH.toFile().mkdirs();
        KEYWORDS_PATH.toFile().mkdirs();
    }
}