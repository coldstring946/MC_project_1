package uk.gov.gchq.magma.xml.analyzer;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple container for document metadata and content extracted from XML files
 */
public class DocumentInfo {
    private final Path filePath;
    private final String fileName;
    private final long fileSize;
    private final LocalDateTime scannedAt;
    
    // Basic document fields
    private String title;
    private String documentType;
    private List<String> authors;
    private String abstractText;
    private String fullText;
    
    // Processing status
    private boolean successfullyProcessed;
    private String errorMessage;
    
    public DocumentInfo(Path filePath) {
        this.filePath = filePath;
        this.fileName = filePath.getFileName().toString();
        this.fileSize = filePath.toFile().length();
        this.scannedAt = LocalDateTime.now();
        this.authors = new ArrayList<>();
        this.successfullyProcessed = false;
    }
    
    // Getters
    public Path getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public LocalDateTime getScannedAt() { return scannedAt; }
    public String getTitle() { return title; }
    public String getDocumentType() { return documentType; }
    public List<String> getAuthors() { return authors; }
    public String getAbstractText() { return abstractText; }
    public String getFullText() { return fullText; }
    public boolean isSuccessfullyProcessed() { return successfullyProcessed; }
    public String getErrorMessage() { return errorMessage; }
    
    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public void addAuthor(String author) { this.authors.add(author); }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }
    public void setFullText(String fullText) { this.fullText = fullText; }
    public void setSuccessfullyProcessed(boolean processed) { this.successfullyProcessed = processed; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    /**
     * Get all searchable text content combined
     */
    public String getAllText() {
        StringBuilder sb = new StringBuilder();
        if (title != null) sb.append(title).append(" ");
        if (abstractText != null) sb.append(abstractText).append(" ");
        if (fullText != null) sb.append(fullText);
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("DocumentInfo{file='%s', title='%s', type='%s', processed=%s}", 
                           fileName, title, documentType, successfullyProcessed);
    }
}