package uk.gov.gchq.magma.xml.test;

import uk.gov.gchq.magma.xml.test.config.ProjectConfig;
import uk.gov.gchq.magma.xml.test.core.DocumentInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple main class to test our project setup
 * This will be the entry point for testing each module
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== MC XML Test Project ===");
        System.out.println("Starting basic setup test...\n");
        
        // Test 1: Validate configuration
        testConfiguration();
        
        // Test 2: List available files
        testFileDiscovery();
        
        // Test 3: Create sample DocumentInfo objects
        testDocumentInfoCreation();
        
        System.out.println("\n=== Setup test complete ===");
    }
    
    private static void testConfiguration() {
        System.out.println("1. Testing configuration...");
        
        // Check if paths exist
        boolean pathsValid = ProjectConfig.validatePaths();
        System.out.println("   Paths valid: " + pathsValid);
        System.out.println("   Project path: " + ProjectConfig.PROJECT_PATH);
        System.out.println("   Data path: " + ProjectConfig.DATA_PATH);
        
        // Create necessary directories
        ProjectConfig.createDirectories();
        System.out.println("   Directories created/verified");
        
        // Check CUDA availability flag
        System.out.println("   CUDA enabled: " + ProjectConfig.ENABLE_CUDA);
        System.out.println();
    }
    
    private static void testFileDiscovery() {
        System.out.println("2. Testing file discovery...");
        
        try {
            // Look for XML files in the data directory
            List<Path> xmlFiles = Files.walk(ProjectConfig.DATA_PATH)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".xml"))
                    .limit(10) // Just first 10 for testing
                    .collect(Collectors.toList());
            
            System.out.println("   Found " + xmlFiles.size() + " XML files (showing first 10):");
            xmlFiles.forEach(file -> 
                System.out.println("     - " + file.getFileName() + " (" + file.toFile().length() + " bytes)")
            );
            
        } catch (IOException e) {
            System.out.println("   Error discovering files: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testDocumentInfoCreation() {
        System.out.println("3. Testing DocumentInfo creation...");
        
        try {
            // Find a few files and create DocumentInfo objects
            List<Path> testFiles = Files.walk(ProjectConfig.DATA_PATH)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.toString().toLowerCase();
                        return name.endsWith(".xml") || name.endsWith(".txt") || name.endsWith(".pdf");
                    })
                    .limit(3)
                    .collect(Collectors.toList());
            
            System.out.println("   Creating DocumentInfo objects for " + testFiles.size() + " files:");
            
            for (Path file : testFiles) {
                DocumentInfo doc = new DocumentInfo(file);
                System.out.println("     " + doc.toString());
            }
            
        } catch (IOException e) {
            System.out.println("   Error creating DocumentInfo objects: " + e.getMessage());
        }
        System.out.println();
    }
}