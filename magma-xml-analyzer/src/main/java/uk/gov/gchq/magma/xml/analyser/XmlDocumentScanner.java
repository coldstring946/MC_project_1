package uk.gov.gchq.magma.xml.analyzer;

import uk.gov.gchq.magma.xml.analyzer.ScanConfig;
import uk.gov.gchq.magma.xml.analyzer.DocumentInfo;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Scans XML documents and extracts basic information
 * Handles both JATS journal articles and simple XML documents
 */
public class XmlDocumentScanner {
    private static final Logger logger = LoggerFactory.getLogger(XmlDocumentScanner.class);
    
    public static void main(String[] args) {
        // Simple test setup - using your actual data path
        String inputDir = "C:\\Users\\INQUSIV_Desktop\\OneDrive - Inqusiv\\Projects\\Uni\\PlosOne";
        int maxFolders = 3;
        int maxFilesPerFolder = 5;
        
        if (args.length >= 3) {
            inputDir = args[0];
            maxFolders = Integer.parseInt(args[1]);
            maxFilesPerFolder = Integer.parseInt(args[2]);
        }
        
        ScanConfig config = new ScanConfig(inputDir, maxFolders, maxFilesPerFolder);
        XmlDocumentScanner scanner = new XmlDocumentScanner();
        
        System.out.println("Starting XML document scan with config: " + config);
        List<DocumentInfo> results = scanner.scanDocuments(config);
        
        // Simple console output
        System.out.println("\n=== SCAN RESULTS ===");
        System.out.printf("Processed %d documents%n", results.size());
        
        for (DocumentInfo doc : results) {
            System.out.printf("%n[%s] %s%n", 
                doc.isSuccessfullyProcessed() ? "OK" : "ERROR", 
                doc.getFileName());
            
            if (doc.getTitle() != null) {
                System.out.printf("  Title: %s%n", doc.getTitle());
            }
            if (doc.getDocumentType() != null) {
                System.out.printf("  Type: %s%n", doc.getDocumentType());
            }
            if (!doc.getAuthors().isEmpty()) {
                System.out.printf("  Authors: %s%n", String.join(", ", doc.getAuthors()));
            }
            if (!doc.isSuccessfullyProcessed() && doc.getErrorMessage() != null) {
                System.out.printf("  Error: %s%n", doc.getErrorMessage());
            }
            System.out.printf("  Size: %d bytes%n", doc.getFileSize());
        }
    }
    
    /**
     * Scan documents according to the configuration
     */
    public List<DocumentInfo> scanDocuments(ScanConfig config) {
        List<DocumentInfo> results = new ArrayList<>();
        Path inputPath = Paths.get(config.getInputDirectory());
        
        if (!Files.exists(inputPath)) {
            logger.error("Input directory does not exist: {}", inputPath);
            return results;
        }
        
        try {
            List<Path> selectedFiles = selectFilesToProcess(inputPath, config);
            logger.info("Selected {} files to process", selectedFiles.size());
            
            for (Path file : selectedFiles) {
                DocumentInfo docInfo = processXmlFile(file);
                results.add(docInfo);
            }
            
        } catch (IOException e) {
            logger.error("Error scanning directory: {}", e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Select files to process based on configuration limits
     */
    private List<Path> selectFilesToProcess(Path inputPath, ScanConfig config) throws IOException {
        List<Path> selectedFiles = new ArrayList<>();
        
        if (Files.isRegularFile(inputPath)) {
            // Single file
            if (isXmlFile(inputPath)) {
                selectedFiles.add(inputPath);
            }
            return selectedFiles;
        }
        
        // Directory processing
        try (Stream<Path> subDirs = Files.list(inputPath)) {
            List<Path> directories = subDirs
                .filter(Files::isDirectory)
                .limit(config.getMaxSubfolders())
                .toList();
            
            for (Path dir : directories) {
                try (Stream<Path> files = Files.list(dir)) {
                    List<Path> xmlFiles = files
                        .filter(Files::isRegularFile)
                        .filter(this::isXmlFile)
                        .limit(config.getMaxFilesPerFolder())
                        .toList();
                    
                    selectedFiles.addAll(xmlFiles);
                }
            }
        }
        
        // Also check for XML files directly in the input directory
        try (Stream<Path> files = Files.list(inputPath)) {
            List<Path> directFiles = files
                .filter(Files::isRegularFile)
                .filter(this::isXmlFile)
                .limit(config.getMaxFilesPerFolder())
                .toList();
            
            selectedFiles.addAll(directFiles);
        }
        
        return selectedFiles;
    }
    
    /**
     * Check if file is an XML file based on extension
     */
    private boolean isXmlFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".xml");
    }
    
    /**
     * Process a single XML file and extract information
     */
    private DocumentInfo processXmlFile(Path file) {
        DocumentInfo docInfo = new DocumentInfo(file);
        
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(file.toFile());
            Element root = document.getRootElement();
            
            // Extract basic information based on document structure
            extractDocumentInfo(root, docInfo);
            docInfo.setSuccessfullyProcessed(true);
            
            logger.debug("Successfully processed: {}", file.getFileName());
            
        } catch (DocumentException e) {
            docInfo.setErrorMessage("Failed to parse XML: " + e.getMessage());
            logger.warn("Failed to process {}: {}", file.getFileName(), e.getMessage());
        } catch (Exception e) {
            docInfo.setErrorMessage("Unexpected error: " + e.getMessage());
            logger.error("Unexpected error processing {}: {}", file.getFileName(), e.getMessage());
        }
        
        return docInfo;
    }
    
    /**
     * Extract information from XML document based on common patterns
     */
    private void extractDocumentInfo(Element root, DocumentInfo docInfo) {
        String rootName = root.getName().toLowerCase();
        
        // Try JATS journal article format first
        if ("article".equals(rootName)) {
            extractJatsInfo(root, docInfo);
        } else {
            // Fallback to generic XML processing
            extractGenericInfo(root, docInfo);
        }
    }
    
    /**
     * Extract information from JATS-format journal articles
     */
    private void extractJatsInfo(Element root, DocumentInfo docInfo) {
        docInfo.setDocumentType("journal-article");
        
        // Try to find title
        Element titleGroup = root.element("front");
        if (titleGroup != null) {
            titleGroup = titleGroup.element("article-meta");
            if (titleGroup != null) {
                titleGroup = titleGroup.element("title-group");
                if (titleGroup != null) {
                    Element title = titleGroup.element("article-title");
                    if (title != null) {
                        docInfo.setTitle(title.getTextTrim());
                    }
                }
            }
        }
        
        // Try to find authors
        Element articleMeta = root.selectSingleNode("//article-meta");
        if (articleMeta != null) {
            List<Element> contribGroups = articleMeta.elements("contrib-group");
            for (Element contribGroup : contribGroups) {
                List<Element> contributors = contribGroup.elements("contrib");
                for (Element contrib : contributors) {
                    Element name = contrib.element("name");
                    if (name != null) {
                        Element surname = name.element("surname");
                        Element givenNames = name.element("given-names");
                        if (surname != null) {
                            String authorName = surname.getTextTrim();
                            if (givenNames != null) {
                                authorName = givenNames.getTextTrim() + " " + authorName;
                            }
                            docInfo.addAuthor(authorName);
                        }
                    }
                }
            }
        }
        
        // Try to find abstract
        Element abstractEl = root.selectSingleNode("//abstract");
        if (abstractEl != null) {
            docInfo.setAbstractText(abstractEl.getTextTrim());
        }
        
        // Get full text from body
        Element body = root.selectSingleNode("//body");
        if (body != null) {
            docInfo.setFullText(body.getTextTrim());
        }
    }
    
    /**
     * Generic XML information extraction
     */
    private void extractGenericInfo(Element root, DocumentInfo docInfo) {
        docInfo.setDocumentType("xml-document");
        
        // Try to find title-like elements
        Element title = findElementByNames(root, Arrays.asList("title", "name", "heading"));
        if (title != null) {
            docInfo.setTitle(title.getTextTrim());
        }
        
        // Get all text content
        docInfo.setFullText(root.getTextTrim());
    }
    
    /**
     * Helper to find elements by common names
     */
    private Element findElementByNames(Element parent, List<String> names) {
        for (String name : names) {
            Element found = parent.element(name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}