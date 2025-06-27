package uk.gov.gchq.magma.xml.analyzer;

import java.io.File;
import java.util.*;

/**
 * Enhanced HQDM converter that builds relationships between document entities
 * This is a simplified version that doesn't require the full MagmaCore dependency
 */
public class EnhancedHqdmConverterWithRelationships {
    
    public static class HqdmEntity {
        private String id;
        private String type;
        private Map<String, Object> properties;
        private Set<String> relationships;
        
        public HqdmEntity(String id, String type) {
            this.id = id;
            this.type = type;
            this.properties = new HashMap<>();
            this.relationships = new HashSet<>();
        }
        
        // Getters and setters
        public String getId() { return id; }
        public String getType() { return type; }
        public Map<String, Object> getProperties() { return properties; }
        public Set<String> getRelationships() { return relationships; }
        
        public void addProperty(String key, Object value) {
            properties.put(key, value);
        }
        
        public void addRelationship(String relationshipId) {
            relationships.add(relationshipId);
        }
        
        @Override
        public String toString() {
            return String.format("HqdmEntity{id='%s', type='%s', properties=%d, relationships=%d}", 
                               id, type, properties.size(), relationships.size());
        }
    }
    
    public static class DocumentRelationshipGraph {
        private Map<String, HqdmEntity> entities;
        private Map<String, Set<String>> relationships;
        private String documentId;
        
        public DocumentRelationshipGraph(String documentId) {
            this.documentId = documentId;
            this.entities = new HashMap<>();
            this.relationships = new HashMap<>();
        }
        
        public void addEntity(HqdmEntity entity) {
            entities.put(entity.getId(), entity);
        }
        
        public void addRelationship(String fromId, String toId, String relationshipType) {
            String relationshipKey = relationshipType + ":" + fromId + "->" + toId;
            relationships.computeIfAbsent(relationshipKey, k -> new HashSet<>()).add(toId);
            
            // Add to entity relationships
            HqdmEntity fromEntity = entities.get(fromId);
            if (fromEntity != null) {
                fromEntity.addRelationship(relationshipKey);
            }
        }
        
        // Getters
        public Map<String, HqdmEntity> getEntities() { return entities; }
        public Map<String, Set<String>> getRelationships() { return relationships; }
        public String getDocumentId() { return documentId; }
        
        public void printSummary() {
            System.out.println("Document: " + documentId);
            System.out.println("  Entities: " + entities.size());
            System.out.println("  Relationships: " + relationships.size());
            
            // Group entities by type
            Map<String, Long> entityTypes = new HashMap<>();
            entities.values().forEach(entity -> 
                entityTypes.merge(entity.getType(), 1L, Long::sum));
            
            System.out.println("  Entity types:");
            entityTypes.forEach((type, count) -> 
                System.out.println("    " + type + ": " + count));
        }
    }
    
    private final XmlDocumentScanner documentScanner;
    private final SemanticTripleExtractor tripleExtractor;
    
    public EnhancedHqdmConverterWithRelationships() {
        this.documentScanner = new XmlDocumentScanner();
        this.tripleExtractor = new SemanticTripleExtractor();
    }
    
    /**
     * Convert document to HQDM entities with relationships
     */
    public DocumentRelationshipGraph convertDocument(File xmlFile) throws Exception {
        // Scan document for basic info
        DocumentInfo docInfo = documentScanner.scanDocument(xmlFile);
        
        DocumentRelationshipGraph graph = new DocumentRelationshipGraph(docInfo.getFileName());
        
        // Create document entity
        HqdmEntity documentEntity = new HqdmEntity("doc_" + docInfo.getFileName(), "Document");
        documentEntity.addProperty("title", docInfo.getTitle());
        documentEntity.addProperty("explosive_score", docInfo.getExplosiveContentScore());
        documentEntity.addProperty("word_count", docInfo.getTotalWordCount());
        graph.addEntity(documentEntity);
        
        // Create author entities
        if (docInfo.getAuthors() != null) {
            for (int i = 0; i < docInfo.getAuthors().size(); i++) {
                String author = docInfo.getAuthors().get(i);
                String authorId = "author_" + docInfo.getFileName() + "_" + i;
                
                HqdmEntity authorEntity = new HqdmEntity(authorId, "Person");
                authorEntity.addProperty("name", author);
                graph.addEntity(authorEntity);
                
                // Relationship: document authored_by author
                graph.addRelationship(documentEntity.getId(), authorId, "authored_by");
            }
        }
        
        // Extract semantic entities from triples
        String documentText = docInfo.getAbstractText() != null ? docInfo.getAbstractText() : "";
        if (!documentText.isEmpty()) {
            extractSemanticEntities(documentText, graph, documentEntity.getId());
        }
        
        // Create keyword entities
        if (docInfo.getMatchedExplosiveTerms() != null) {
            for (String keyword : docInfo.getMatchedExplosiveTerms()) {
                String keywordId = "keyword_" + keyword.replaceAll("\\s+", "_");
                
                HqdmEntity keywordEntity = new HqdmEntity(keywordId, "Concept");
                keywordEntity.addProperty("term", keyword);
                keywordEntity.addProperty("frequency", docInfo.getKeywordCounts().getOrDefault(keyword, 0));
                graph.addEntity(keywordEntity);
                
                // Relationship: document mentions keyword
                graph.addRelationship(documentEntity.getId(), keywordId, "mentions");
            }
        }
        
        return graph;
    }
    
    private void extractSemanticEntities(String text, DocumentRelationshipGraph graph, String documentId) {
        SemanticTripleExtractor.TripleExtractionResult tripleResult = tripleExtractor.extractTriples(text);
        
        // Combine all triples
        List<SemanticTripleExtractor.SemanticTriple> allTriples = new ArrayList<>();
        allTriples.addAll(tripleResult.getParagraphLevelTriples());
        allTriples.addAll(tripleResult.getSentenceLevelTriples());
        
        // Extract entities from triples
        Set<String> subjects = new HashSet<>();
        Set<String> objects = new HashSet<>();
        Set<String> predicates = new HashSet<>();
        
        for (SemanticTripleExtractor.SemanticTriple triple : allTriples) {
            subjects.add(triple.getSubject());
            objects.add(triple.getObject());
            predicates.add(triple.getPredicate());
        }
        
        // Create entities for subjects and objects
        for (String subject : subjects) {
            String entityId = "entity_" + subject.replaceAll("\\s+", "_").toLowerCase();
            HqdmEntity entity = new HqdmEntity(entityId, "SemanticEntity");
            entity.addProperty("label", subject);
            entity.addProperty("role", "subject");
            graph.addEntity(entity);
        }
        
        for (String object : objects) {
            String entityId = "entity_" + object.replaceAll("\\s+", "_").toLowerCase();
            if (!graph.getEntities().containsKey(entityId)) {
                HqdmEntity entity = new HqdmEntity(entityId, "SemanticEntity");
                entity.addProperty("label", object);
                entity.addProperty("role", "object");
                graph.addEntity(entity);
            }
        }
        
        // Create relationships from triples
        for (SemanticTripleExtractor.SemanticTriple triple : allTriples) {
            String subjectId = "entity_" + triple.getSubject().replaceAll("\\s+", "_").toLowerCase();
            String objectId = "entity_" + triple.getObject().replaceAll("\\s+", "_").toLowerCase();
            
            graph.addRelationship(subjectId, objectId, triple.getPredicate());
        }
    }
    
    /**
     * Analyze relationships across multiple documents
     */
    public Map<String, Integer> analyzeGlobalRelationships(List<DocumentRelationshipGraph> graphs) {
        Map<String, Integer> relationshipFrequency = new HashMap<>();
        
        for (DocumentRelationshipGraph graph : graphs) {
            for (String relationship : graph.getRelationships().keySet()) {
                relationshipFrequency.merge(relationship, 1, Integer::sum);
            }
        }
        
        return relationshipFrequency;
    }
    
    /**
     * Find entities that appear across multiple documents
     */
    public Map<String, List<String>> findCrossDocumentEntities(List<DocumentRelationshipGraph> graphs) {
        Map<String, List<String>> entityToDocuments = new HashMap<>();
        
        for (DocumentRelationshipGraph graph : graphs) {
            for (HqdmEntity entity : graph.getEntities().values()) {
                String entityLabel = (String) entity.getProperties().get("label");
                if (entityLabel != null) {
                    entityToDocuments.computeIfAbsent(entityLabel, k -> new ArrayList<>())
                                   .add(graph.getDocumentId());
                }
            }
        }
        
        // Return only entities that appear in multiple documents
        return entityToDocuments.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);
    }
    
    public static void main(String[] args) {
        EnhancedHqdmConverterWithRelationships converter = new EnhancedHqdmConverterWithRelationships();
        
        try {
            String directory = args.length > 0 ? args[0] : ".";
            
            System.out.println("=== Enhanced HQDM Converter with Relationships ===\n");
            
            // Find XML files
            File dir = new File(directory);
            File[] xmlFiles = dir.listFiles((file, name) -> name.toLowerCase().endsWith(".xml"));
            
            if (xmlFiles == null || xmlFiles.length == 0) {
                System.out.println("No XML files found in directory: " + directory);
                System.out.println("Running with sample data demonstration...\n");
                demonstrateWithSampleData(converter);
                return;
            }
            
            List<DocumentRelationshipGraph> allGraphs = new ArrayList<>();
            
            // Process each document
            for (File xmlFile : xmlFiles) {
                try {
                    System.out.println("Processing: " + xmlFile.getName());
                    DocumentRelationshipGraph graph = converter.convertDocument(xmlFile);
                    allGraphs.add(graph);
                    graph.printSummary();
                    System.out.println();
                    
                } catch (Exception e) {
                    System.err.println("Failed to process " + xmlFile.getName() + ": " + e.getMessage());
                }
            }
            
            if (!allGraphs.isEmpty()) {
                // Analyze global patterns
                System.out.println("=== Global Relationship Analysis ===");
                Map<String, Integer> globalRelationships = converter.analyzeGlobalRelationships(allGraphs);
                System.out.println("Most common relationships:");
                globalRelationships.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(10)
                    .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue()));
                
                System.out.println("\n=== Cross-Document Entities ===");
                Map<String, List<String>> crossDocEntities = converter.findCrossDocumentEntities(allGraphs);
                System.out.println("Entities appearing in multiple documents:");
                crossDocEntities.entrySet().stream()
                    .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                    .limit(10)
                    .forEach(entry -> System.out.println("  " + entry.getKey() + 
                           " (appears in " + entry.getValue().size() + " documents)"));
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demonstrateWithSampleData(EnhancedHqdmConverterWithRelationships converter) {
        System.out.println("=== Sample Data Demonstration ===");
        System.out.println("This would create HQDM entities and relationships from:");
        System.out.println("- Document metadata (title, authors, publication info)");
        System.out.println("- Semantic triples (subject-predicate-object relationships)");
        System.out.println("- Keyword entities and their frequencies");
        System.out.println("- Cross-document entity relationships");
        System.out.println();
        System.out.println("Example entities that would be created:");
        System.out.println("- Document entity with properties (title, explosive_score, word_count)");
        System.out.println("- Author entities linked to documents via 'authored_by' relationships");
        System.out.println("- Concept entities for explosive terms with 'mentions' relationships");
        System.out.println("- Semantic entities from extracted triples with process relationships");
        System.out.println();
        System.out.println("To test with real data, place XML files in the directory and run again.");
    }
}