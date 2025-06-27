import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * File discovery and sampling module for XML corpus analysis
 * Takes file #500 from each folder of 1000 XML files
 */
public class FileSampler {
    
    private static final int TARGET_FILE_INDEX = 499; // 500th file (0-based indexing)
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java FileSampler <root-corpus-directory>");
            System.out.println("Example: java FileSampler /path/to/xml-corpus/");
            System.exit(1);
        }
        
        String rootPath = args[0];
        FileSampler sampler = new FileSampler();
        
        try {
            List<File> sampledFiles = sampler.sampleXMLFiles(rootPath);
            sampler.reportResults(sampledFiles);
            sampler.saveSampleList(sampledFiles, "sampled-files.txt");
            
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Discover folders and sample XML files from each
     */
    public List<File> sampleXMLFiles(String rootPath) throws IOException {
        Path root = Paths.get(rootPath);
        
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("Root path does not exist or is not a directory: " + rootPath);
        }
        
        List<File> sampledFiles = new ArrayList<>();
        
        // Find all subdirectories
        List<Path> subDirectories = Files.list(root)
                .filter(Files::isDirectory)
                .sorted()
                .collect(Collectors.toList());
        
        System.out.println("Found " + subDirectories.size() + " subdirectories");
        
        for (Path subDir : subDirectories) {
            File sampledFile = sampleFromDirectory(subDir.toFile());
            if (sampledFile != null) {
                sampledFiles.add(sampledFile);
                System.out.println("Sampled: " + sampledFile.getAbsolutePath());
            } else {
                System.out.println("Warning: Could not sample from " + subDir.getFileName());
            }
        }
        
        return sampledFiles;
    }
    
    /**
     * Sample the 500th XML file from a directory
     */
    private File sampleFromDirectory(File directory) {
        File[] xmlFiles = directory.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".xml"));
        
        if (xmlFiles == null || xmlFiles.length == 0) {
            System.out.println("No XML files found in: " + directory.getName());
            return null;
        }
        
        // Sort files alphabetically for consistent sampling
        Arrays.sort(xmlFiles, Comparator.comparing(File::getName));
        
        System.out.printf("Directory %s: %d XML files found%n", 
                         directory.getName(), xmlFiles.length);
        
        // Take the 500th file, or the last file if fewer than 500
        int targetIndex = Math.min(TARGET_FILE_INDEX, xmlFiles.length - 1);
        File selectedFile = xmlFiles[targetIndex];
        
        System.out.printf("  Selected file #%d: %s%n", 
                         targetIndex + 1, selectedFile.getName());
        
        return selectedFile;
    }
    
    /**
     * Report sampling results
     */
    private void reportResults(List<File> sampledFiles) {
        System.out.println("\n=== SAMPLING RESULTS ===");
        System.out.println("Total sampled files: " + sampledFiles.size());
        
        if (!sampledFiles.isEmpty()) {
            System.out.println("\nFirst few samples:");
            sampledFiles.stream()
                    .limit(5)
                    .forEach(file -> System.out.println("  " + file.getName()));
            
            if (sampledFiles.size() > 5) {
                System.out.println("  ... and " + (sampledFiles.size() - 5) + " more");
            }
        }
        
        // Calculate total file size
        long totalSize = sampledFiles.stream()
                .mapToLong(File::length)
                .sum();
        
        System.out.printf("\nTotal size of sampled files: %.2f MB%n", 
                         totalSize / (1024.0 * 1024.0));
    }
    
    /**
     * Save list of sampled files for next processing step
     */
    private void saveSampleList(List<File> sampledFiles, String outputFile) throws IOException {
        Path outputPath = Paths.get(outputFile);
        
        List<String> filePaths = sampledFiles.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
        
        Files.write(outputPath, filePaths);
        System.out.println("\nSaved file list to: " + outputPath.toAbsolutePath());
        System.out.println("Use this list for the next processing step.");
    }
    
    /**
     * Utility method to load previously sampled file list
     */
    public static List<File> loadSampledFiles(String listFile) throws IOException {
        return Files.readAllLines(Paths.get(listFile))
                .stream()
                .map(File::new)
                .filter(File::exists)
                .collect(Collectors.toList());
    }
}