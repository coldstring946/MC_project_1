package uk.gov.gchq.magma.xml.analyzer;

/**
 * Simple configuration for controlling how many files to process
 */
public class ScanConfig {
    private final int maxSubfolders;
    private final int maxFilesPerFolder;
    private final String inputDirectory;
    
    public ScanConfig(String inputDirectory, int maxSubfolders, int maxFilesPerFolder) {
        this.inputDirectory = inputDirectory;
        this.maxSubfolders = maxSubfolders;
        this.maxFilesPerFolder = maxFilesPerFolder;
    }
    
    public int getMaxSubfolders() { return maxSubfolders; }
    public int getMaxFilesPerFolder() { return maxFilesPerFolder; }
    public String getInputDirectory() { return inputDirectory; }
    
    @Override
    public String toString() {
        return String.format("ScanConfig{dir='%s', maxFolders=%d, maxFiles=%d}", 
                           inputDirectory, maxSubfolders, maxFilesPerFolder);
    }
}