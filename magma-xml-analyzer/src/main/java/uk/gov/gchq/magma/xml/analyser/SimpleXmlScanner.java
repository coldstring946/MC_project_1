package uk.gov.gchq.magma.xml.analyzer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Simple, standalone XML scanner to test basic functionality
 * This avoids conflicts with existing code
 */
public class SimpleXmlScanner {
    
    public static void main(String[] args) {
        String inputDir = "C:\\Users\\INQUSIV_Desktop\\OneDrive - Inqusiv\\Projects\\Uni\\PlosOne";
        int maxFiles = 3;
        
        if (args.length >= 2) {
            inputDir = args[0];
            maxFiles = Integer.parseInt(args[1]);
        }
        
        System.out.println("=== Simple XML Scanner Test ===");
        System.out.println("Scanning directory: " + inputDir);
        System.out.println("Max files to process: " + maxFiles);
        System.out.println();
        
        SimpleXmlScanner scanner = new SimpleXmlScanner();
        scanner.scanDirectory(inputDir, maxFiles);
    }
    
    public void scanDirectory(String directoryPath, int maxFiles) {
        Path path = Paths.get(directoryPath);
        
        if (!Files.exists(path)) {
            System.out.println("ERROR: Directory does not exist: " + directoryPath);
            return;
        }
        
        System.out.println("Directory exists: " + path.toAbsolutePath());
        
        try {
            // List all files and subdirectories
            System.out.println("\n=== Directory Contents ===");
            try (Stream<Path> paths = Files.list(path)) {
                List<Path> items = paths.limit(10).toList(); // Limit to first 10 items
                
                for (Path item : items) {
                    if (Files.isDirectory(item)) {
                        System.out.println("[DIR]  " + item.getFileName());
                    } else {
                        System.out.printf("[FILE] %s (%d bytes)%n", 
                                         item.getFileName(), 
                                         Files.size(item));
                    }
                }
            }
            
            // Find XML files
            System.out.println("\n=== XML Files Found ===");
            try (Stream<Path> paths = Files.walk(path, 2)) { // Walk 2 levels deep
                List<Path> xmlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
                    .limit(maxFiles)
                    .toList();
                
                if (xmlFiles.isEmpty()) {
                    System.out.println("No XML files found.");
                } else {
                    for (Path xmlFile : xmlFiles) {
                        System.out.printf("Found XML: %s (%d bytes)%n", 
                                         xmlFile.getFileName(), 
                                         Files.size(xmlFile));
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error scanning directory: " + e.getMessage());
            e.printStackTrace();
        }
    }
}