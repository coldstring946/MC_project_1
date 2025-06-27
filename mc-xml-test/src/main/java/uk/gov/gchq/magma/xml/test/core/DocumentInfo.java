package uk.gov.gchq.magma.xml.test.core;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple container for document information
 * Keeps track of basic metadata about processed documents
 */
public class DocumentInfo {
    
    private final Path filePath;
    private final String fileName;
    private final long fileSize;
    private final String documentType;
    
    // Basic content info
    private String title;
    private String abstractText;
    private int wordCount;
    private boolean isExplosivesRelated;
    
    // Keyword matches
    private Map<String, Integer> keywordCounts;
    
    // Processing info
    private long processingTime;
    private boolean processingSuccessful;
    private String errorMessage;
    
    public DocumentInfo(Path filePath) {
        this.filePath = filePath;
        this.fileName = filePath.getFileName().toString();
        this.fileSize = filePath.toFile().length();
        this.documentType = determineDocumentType(fileName);
        this.keywordCounts = new HashMap<>();
        this.processingSuccessful = false;
    }
    
    private String determineDocumentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".xml")) return "XML";
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".txt")) return "TEXT";
        return "UNKNOWN";
    }
    
    // Getters
    public Path getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getDocumentType() { return documentType; }
    public String getTitle() { return title; }
    public String getAbstractText() { return abstractText; }
    public int getWordCount() { return wordCount; }
    public boolean isExplosivesRelated() { return isExplosivesRelated; }
    public Map<String, Integer> getKeywordCounts() { return keywordCounts; }
    public long getProcessingTime() { return processingTime; }
    public boolean isProcessingSuccessful() { return processingSuccessful; }
    public String getErrorMessage() { return errorMessage; }
    
    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public void setExplosivesRelated(boolean explosivesRelated) { this.isExplosivesRelated = explosivesRelated; }
    public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
    public void setProcessingSuccessful(boolean successful) { this.processingSuccessful = successful; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    // Keyword operations
    public void addKeywordMatch(String keyword) {
        keywordCounts.put(keyword, keywordCounts.getOrDefault(keyword, 0) + 1);
    }
    
    public int getTotalKeywordMatches() {
        return keywordCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    @Override
    public String toString() {
        return String.format("DocumentInfo{file='%s', type='%s', size=%d, explosives=%b, keywords=%d}", 
                fileName, documentType, fileSize, isExplosivesRelated, getTotalKeywordMatches());
    }
}