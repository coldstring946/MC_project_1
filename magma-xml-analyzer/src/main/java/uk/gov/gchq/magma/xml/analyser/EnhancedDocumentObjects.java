package uk.gov.gchq.magma.xml.analyzer;

import uk.gov.gchq.magmacore.hqdm.model.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced container class for HQDM objects with relationships
 * Includes associations between documents, authors, and journals
 */
public class EnhancedDocumentObjects {
    
    private Thing document;
    private List<Person> authors = new ArrayList<>();
    private Organization journal;
    private Association publicationRelationship;  // Document published in Journal
    private List<Association> authorshipRelationships = new ArrayList<>();  // Authors authored Document
    
    // Constructors
    public EnhancedDocumentObjects() {}
    
    public EnhancedDocumentObjects(Thing document) {
        this.document = document;
    }
    
    // Getters and Setters
    public Thing getDocument() {
        return document;
    }
    
    public void setDocument(Thing document) {
        this.document = document;
    }
    
    public List<Person> getAuthors() {
        return authors;
    }
    
    public void setAuthors(List<Person> authors) {
        this.authors = authors != null ? authors : new ArrayList<>();
    }
    
    public void addAuthor(Person author) {
        if (this.authors == null) {
            this.authors = new ArrayList<>();
        }
        this.authors.add(author);
    }
    
    public Organization getJournal() {
        return journal;
    }
    
    public void setJournal(Organization journal) {
        this.journal = journal;
    }
    
    public Association getPublicationRelationship() {
        return publicationRelationship;
    }
    
    public void setPublicationRelationship(Association publicationRelationship) {
        this.publicationRelationship = publicationRelationship;
    }
    
    public List<Association> getAuthorshipRelationships() {
        return authorshipRelationships;
    }
    
    public void setAuthorshipRelationships(List<Association> authorshipRelationships) {
        this.authorshipRelationships = authorshipRelationships != null ? authorshipRelationships : new ArrayList<>();
    }
    
    public void addAuthorshipRelationship(Association authorship) {
        if (this.authorshipRelationships == null) {
            this.authorshipRelationships = new ArrayList<>();
        }
        this.authorshipRelationships.add(authorship);
    }
    
    // Utility methods
    public boolean hasDocument() {
        return document != null;
    }
    
    public boolean hasAuthors() {
        return authors != null && !authors.isEmpty();
    }
    
    public boolean hasJournal() {
        return journal != null;
    }
    
    public boolean hasPublicationRelationship() {
        return publicationRelationship != null;
    }
    
    public boolean hasAuthorshipRelationships() {
        return authorshipRelationships != null && !authorshipRelationships.isEmpty();
    }
    
    /**
     * Get total count of all HQDM objects created (including relationships)
     */
    public int getTotalObjectCount() {
        int count = 0;
        if (document != null) count++;
        if (authors != null) count += authors.size();
        if (journal != null) count++;
        if (publicationRelationship != null) count++;
        if (authorshipRelationships != null) count += authorshipRelationships.size();
        return count;
    }
    
    /**
     * Get count of relationship objects only
     */
    public int getRelationshipCount() {
        int count = 0;
        if (publicationRelationship != null) count++;
        if (authorshipRelationships != null) count += authorshipRelationships.size();
        return count;
    }
    
    /**
     * Get a detailed summary of created objects and relationships
     */
    public String getDetailedSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("HQDM Objects and Relationships Created:\n");
        summary.append("=====================================\n");
        
        // Core objects
        summary.append("Core Objects:\n");
        summary.append("- Document: ").append(document != null ? "✓" : "✗").append("\n");
        summary.append("- Authors: ").append(authors != null ? authors.size() : 0).append("\n");
        summary.append("- Journal: ").append(journal != null ? "✓" : "✗").append("\n");
        
        // Relationships
        summary.append("\nRelationships:\n");
        summary.append("- Publication Relationship: ").append(publicationRelationship != null ? "✓" : "✗").append("\n");
        summary.append("- Authorship Relationships: ").append(authorshipRelationships != null ? authorshipRelationships.size() : 0).append("\n");
        
        // Totals
        summary.append("\nTotals:\n");
        summary.append("- Total Objects: ").append(getTotalObjectCount()).append("\n");
        summary.append("- Relationship Objects: ").append(getRelationshipCount()).append("\n");
        summary.append("- Core Entity Objects: ").append(getTotalObjectCount() - getRelationshipCount());
        
        return summary.toString();
    }
    
    /**
     * Get a simple summary of the graph structure
     */
    public String getGraphStructure() {
        StringBuilder graph = new StringBuilder();
        graph.append("HQDM Graph Structure:\n");
        
        if (document != null) {
            graph.append("Document: ").append(document.getId()).append("\n");
            
            // Authors relationships
            if (hasAuthors() && hasAuthorshipRelationships()) {
                for (int i = 0; i < authors.size(); i++) {
                    Person author = authors.get(i);
                    graph.append("  ├─ authored_by ─> Person: ").append(author.getId()).append("\n");
                }
            }
            
            // Journal relationship
            if (hasJournal() && hasPublicationRelationship()) {
                graph.append("  └─ published_in ─> Journal: ").append(journal.getId()).append("\n");
            }
        }
        
        return graph.toString();
    }
    
    @Override
    public String toString() {
        return String.format("EnhancedDocumentObjects{document=%s, authors=%d, journal=%s, relationships=%d}", 
                           document != null ? document.getId() : "null", 
                           authors != null ? authors.size() : 0,
                           journal != null ? "present" : "null",
                           getRelationshipCount());
    }
}