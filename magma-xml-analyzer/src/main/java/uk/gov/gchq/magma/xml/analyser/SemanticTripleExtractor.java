package uk.gov.gchq.magma.xml.analyzer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts semantic triples (Subject-Predicate-Object) from text at paragraph and sentence level
 * This is a rule-based approach that will be compared with statistical methods
 */
public class SemanticTripleExtractor {
    
    public static class SemanticTriple {
        private String subject;
        private String predicate;
        private String object;
        private String sourceText;
        private TextLevel textLevel;
        private double confidence;
        
        public enum TextLevel {
            PARAGRAPH, SENTENCE
        }
        
        public SemanticTriple(String subject, String predicate, String object, String sourceText, TextLevel level) {
            this.subject = subject.trim();
            this.predicate = predicate.trim();
            this.object = object.trim();
            this.sourceText = sourceText;
            this.textLevel = level;
            this.confidence = 1.0; // Default confidence
        }
        
        // Getters and setters
        public String getSubject() { return subject; }
        public String getPredicate() { return predicate; }
        public String getObject() { return object; }
        public String getSourceText() { return sourceText; }
        public TextLevel getTextLevel() { return textLevel; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        @Override
        public String toString() {
            return String.format("(%s, %s, %s) [%s, conf=%.2f]", 
                               subject, predicate, object, textLevel, confidence);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof SemanticTriple)) return false;
            SemanticTriple other = (SemanticTriple) obj;
            return Objects.equals(subject, other.subject) &&
                   Objects.equals(predicate, other.predicate) &&
                   Objects.equals(object, other.object);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(subject, predicate, object);
        }
    }
    
    public static class TripleExtractionResult {
        private List<SemanticTriple> paragraphLevelTriples;
        private List<SemanticTriple> sentenceLevelTriples;
        private Map<String, Integer> predicateFrequency;
        private Map<String, Integer> subjectFrequency;
        private Map<String, Integer> objectFrequency;
        private int totalParagraphs;
        private int totalSentences;
        
        public TripleExtractionResult() {
            this.paragraphLevelTriples = new ArrayList<>();
            this.sentenceLevelTriples = new ArrayList<>();
            this.predicateFrequency = new HashMap<>();
            this.subjectFrequency = new HashMap<>();
            this.objectFrequency = new HashMap<>();
        }
        
        // Getters and setters
        public List<SemanticTriple> getParagraphLevelTriples() { return paragraphLevelTriples; }
        public List<SemanticTriple> getSentenceLevelTriples() { return sentenceLevelTriples; }
        public Map<String, Integer> getPredicateFrequency() { return predicateFrequency; }
        public Map<String, Integer> getSubjectFrequency() { return subjectFrequency; }
        public Map<String, Integer> getObjectFrequency() { return objectFrequency; }
        public int getTotalParagraphs() { return totalParagraphs; }
        public void setTotalParagraphs(int totalParagraphs) { this.totalParagraphs = totalParagraphs; }
        public int getTotalSentences() { return totalSentences; }
        public void setTotalSentences(int totalSentences) { this.totalSentences = totalSentences; }
        
        public void addTriple(SemanticTriple triple) {
            if (triple.getTextLevel() == SemanticTriple.TextLevel.PARAGRAPH) {
                paragraphLevelTriples.add(triple);
            } else {
                sentenceLevelTriples.add(triple);
            }
            
            // Update frequency counters
            predicateFrequency.merge(triple.getPredicate(), 1, Integer::sum);
            subjectFrequency.merge(triple.getSubject(), 1, Integer::sum);
            objectFrequency.merge(triple.getObject(), 1, Integer::sum);
        }
    }
    
    // Patterns for extracting semantic relationships
    private final List<ExtractionPattern> extractionPatterns;
    private final Set<String> explosiveEntities;
    private final Set<String> actionVerbs;
    private final Set<String> propertyTerms;
    
    public SemanticTripleExtractor() {
        this.extractionPatterns = initializeExtractionPatterns();
        this.explosiveEntities = initializeExplosiveEntities();
        this.actionVerbs = initializeActionVerbs();
        this.propertyTerms = initializePropertyTerms();
    }
    
    private static class ExtractionPattern {
        Pattern pattern;
        String description;
        int subjectGroup;
        int predicateGroup;
        int objectGroup;
        
        ExtractionPattern(String regex, String description, int subjectGroup, int predicateGroup, int objectGroup) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.description = description;
            this.subjectGroup = subjectGroup;
            this.predicateGroup = predicateGroup;
            this.objectGroup = objectGroup;
        }
    }
    
    /**
     * Extract semantic triples from document text
     */
    public TripleExtractionResult extractTriples(String documentText) {
        TripleExtractionResult result = new TripleExtractionResult();
        
        // Split into paragraphs
        String[] paragraphs = documentText.split("\\n\\s*\\n");
        result.setTotalParagraphs(paragraphs.length);
        
        int totalSentences = 0;
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) continue;
            
            // Extract paragraph-level triples
            List<SemanticTriple> paragraphTriples = extractTriplesFromText(
                paragraph, SemanticTriple.TextLevel.PARAGRAPH);
            paragraphTriples.forEach(result::addTriple);
            
            // Split paragraph into sentences
            String[] sentences = splitIntoSentences(paragraph);
            totalSentences += sentences.length;
            
            // Extract sentence-level triples
            for (String sentence : sentences) {
                if (sentence.trim().isEmpty()) continue;
                
                List<SemanticTriple> sentenceTriples = extractTriplesFromText(
                    sentence, SemanticTriple.TextLevel.SENTENCE);
                sentenceTriples.forEach(result::addTriple);
            }
        }
        
        result.setTotalSentences(totalSentences);
        return result;
    }
    
    private List<SemanticTriple> extractTriplesFromText(String text, SemanticTriple.TextLevel level) {
        List<SemanticTriple> triples = new ArrayList<>();
        
        for (ExtractionPattern pattern : extractionPatterns) {
            Matcher matcher = pattern.pattern.matcher(text);
            while (matcher.find()) {
                try {
                    String subject = matcher.group(pattern.subjectGroup);
                    String predicate;
                    String object;
                    
                    // Special handling for temperature/pressure patterns
                    if (pattern.description.equals("Property at value")) {
                        predicate = "measured_at";
                        object = matcher.group(pattern.objectGroup);
                    } else {
                        predicate = matcher.group(pattern.predicateGroup);
                        object = matcher.group(pattern.objectGroup);
                    }
                    
                    if (isValidTriple(subject, predicate, object)) {
                        SemanticTriple triple = new SemanticTriple(subject, predicate, object, text, level);
                        triple.setConfidence(calculateTripleConfidence(triple));
                        triples.add(triple);
                    }
                } catch (Exception e) {
                    // Skip malformed matches
                }
            }
        }
        
        return triples;
    }
    
    private boolean isValidTriple(String subject, String predicate, String object) {
        return subject != null && predicate != null && object != null &&
               subject.length() > 2 && predicate.length() > 2 && object.length() > 2 &&
               !subject.equals(predicate) && !predicate.equals(object) && !subject.equals(object);
    }
    
    private double calculateTripleConfidence(SemanticTriple triple) {
        double confidence = 0.5; // Base confidence
        
        // Boost confidence if subject or object is explosive-related
        if (explosiveEntities.stream().anyMatch(entity -> 
                triple.getSubject().toLowerCase().contains(entity.toLowerCase()) ||
                triple.getObject().toLowerCase().contains(entity.toLowerCase()))) {
            confidence += 0.3;
        }
        
        // Boost confidence if predicate is a known action verb
        if (actionVerbs.contains(triple.getPredicate().toLowerCase())) {
            confidence += 0.2;
        }
        
        // Boost confidence if object is a property term
        if (propertyTerms.stream().anyMatch(prop -> 
                triple.getObject().toLowerCase().contains(prop.toLowerCase()))) {
            confidence += 0.2;
        }
        
        return Math.min(1.0, confidence);
    }
    
    private String[] splitIntoSentences(String text) {
        // Simple sentence splitting - can be enhanced with NLP libraries
        return text.split("(?<=[.!?])\\s+");
    }
    
    private List<ExtractionPattern> initializeExtractionPatterns() {
        List<ExtractionPattern> patterns = new ArrayList<>();
        
        // Pattern 1: Subject [verb] Object
        patterns.add(new ExtractionPattern(
            "(\\b\\w+(?:\\s+\\w+){0,2})\\s+(increases?|decreases?|causes?|produces?|exhibits?|shows?|demonstrates?)\\s+(\\w+(?:\\s+\\w+){0,3})",
            "Simple Subject-Verb-Object", 1, 2, 3));
        
        // Pattern 2: Subject has/contains Object
        patterns.add(new ExtractionPattern(
            "(\\b\\w+(?:\\s+\\w+){0,2})\\s+(has|contains?|includes?)\\s+(\\w+(?:\\s+\\w+){0,3})",
            "Subject has/contains Object", 1, 2, 3));
        
        // Pattern 3: Subject is/was/were Object
        patterns.add(new ExtractionPattern(
            "(\\b\\w+(?:\\s+\\w+){0,2})\\s+(is|was|were|are)\\s+(\\w+(?:\\s+\\w+){0,3})",
            "Subject is Object", 1, 2, 3));
        
        // Pattern 4: Object is/was formed/created by Subject
        patterns.add(new ExtractionPattern(
            "(\\w+(?:\\s+\\w+){0,3})\\s+(?:is|was|were)\\s+(formed|created|produced|synthesized)\\s+by\\s+(\\w+(?:\\s+\\w+){0,2})",
            "Object formed by Subject", 3, 2, 1));
        
        // Pattern 5: Temperature/Pressure relationships - create custom handling
        patterns.add(new ExtractionPattern(
            "(\\w+(?:\\s+\\w+){0,2})\\s+at\\s+(\\d+(?:\\.\\d+)?\\s*(?:°C|°F|K|atm|bar|Pa|psi))",
            "Property at value", 1, 1, 2)); // Use 1 as placeholder, handle specially
        
        // Pattern 6: Chemical reactions
        patterns.add(new ExtractionPattern(
            "(\\w+(?:\\s+\\w+){0,2})\\s+(reacts?\\s+with|combines?\\s+with|forms?)\\s+(\\w+(?:\\s+\\w+){0,2})",
            "Chemical reaction", 1, 2, 3));
        
        return patterns;
    }
    
    private Set<String> initializeExplosiveEntities() {
        return new HashSet<>(Arrays.asList(
            "TNT", "RDX", "HMX", "PETN", "TATB", "nitroglycerine", "nitrocellulose",
            "ammonium nitrate", "potassium perchlorate", "explosive", "propellant",
            "detonator", "blast", "detonation", "combustion", "ignition"
        ));
    }
    
    private Set<String> initializeActionVerbs() {
        return new HashSet<>(Arrays.asList(
            "increases", "decreases", "causes", "produces", "exhibits", "shows",
            "demonstrates", "has", "contains", "includes", "forms", "creates",
            "synthesizes", "reacts", "combines", "detonates", "explodes", "burns"
        ));
    }
    
    private Set<String> initializePropertyTerms() {
        return new HashSet<>(Arrays.asList(
            "temperature", "pressure", "density", "velocity", "sensitivity",
            "stability", "performance", "energy", "power", "strength",
            "composition", "structure", "property", "characteristic"
        ));
    }
    
    /**
     * Filter triples by relevance to explosives domain
     */
    public List<SemanticTriple> filterExplosiveRelevantTriples(List<SemanticTriple> triples) {
        return triples.stream()
            .filter(triple -> isExplosiveRelevant(triple))
            .collect(ArrayList::new, (list, triple) -> list.add(triple), List::addAll);
    }
    
    private boolean isExplosiveRelevant(SemanticTriple triple) {
        String text = (triple.getSubject() + " " + triple.getPredicate() + " " + triple.getObject()).toLowerCase();
        return explosiveEntities.stream().anyMatch(entity -> text.contains(entity.toLowerCase())) ||
               propertyTerms.stream().anyMatch(prop -> text.contains(prop.toLowerCase()));
    }
}