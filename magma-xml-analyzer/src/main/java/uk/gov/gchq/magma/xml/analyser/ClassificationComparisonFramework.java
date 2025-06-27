package uk.gov.gchq.magma.xml.analyzer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Framework for comparing rule-based semantic classification with statistical approaches
 */
public class ClassificationComparisonFramework {
    
    public static class ClassificationResult {
        private String documentId;
        private double ruleBasedScore;
        private double statisticalScore;
        private Map<String, Double> ruleBasedFeatures;
        private Map<String, Double> statisticalFeatures;
        private List<String> ruleBasedClassifications;
        private List<String> statisticalClassifications;
        private String actualClassification; // Ground truth if available
        
        public ClassificationResult(String documentId) {
            this.documentId = documentId;
            this.ruleBasedFeatures = new HashMap<>();
            this.statisticalFeatures = new HashMap<>();
            this.ruleBasedClassifications = new ArrayList<>();
            this.statisticalClassifications = new ArrayList<>();
        }
        
        // Getters and setters
        public String getDocumentId() { return documentId; }
        public double getRuleBasedScore() { return ruleBasedScore; }
        public void setRuleBasedScore(double ruleBasedScore) { this.ruleBasedScore = ruleBasedScore; }
        public double getStatisticalScore() { return statisticalScore; }
        public void setStatisticalScore(double statisticalScore) { this.statisticalScore = statisticalScore; }
        public Map<String, Double> getRuleBasedFeatures() { return ruleBasedFeatures; }
        public Map<String, Double> getStatisticalFeatures() { return statisticalFeatures; }
        public List<String> getRuleBasedClassifications() { return ruleBasedClassifications; }
        public List<String> getStatisticalClassifications() { return statisticalClassifications; }
        public String getActualClassification() { return actualClassification; }
        public void setActualClassification(String actualClassification) { this.actualClassification = actualClassification; }
        
        public double getScoreDifference() {
            return Math.abs(ruleBasedScore - statisticalScore);
        }
        
        public boolean hasClassificationAgreement() {
            return !Collections.disjoint(ruleBasedClassifications, statisticalClassifications);
        }
    }
    
    public static class ComparisonAnalysis {
        private List<ClassificationResult> results;
        private double averageScoreCorrelation;
        private double classificationAgreementRate;
        private Map<String, Double> featureImportanceComparison;
        private Map<String, Integer> classificationDiscrepancies;
        private double ruleBasedPrecision;
        private double statisticalPrecision;
        private double ruleBasedRecall;
        private double statisticalRecall;
        
        public ComparisonAnalysis() {
            this.results = new ArrayList<>();
            this.featureImportanceComparison = new HashMap<>();
            this.classificationDiscrepancies = new HashMap<>();
        }
        
        // Getters
        public List<ClassificationResult> getResults() { return results; }
        public double getAverageScoreCorrelation() { return averageScoreCorrelation; }
        public void setAverageScoreCorrelation(double averageScoreCorrelation) { this.averageScoreCorrelation = averageScoreCorrelation; }
        public double getClassificationAgreementRate() { return classificationAgreementRate; }
        public void setClassificationAgreementRate(double classificationAgreementRate) { this.classificationAgreementRate = classificationAgreementRate; }
        public Map<String, Double> getFeatureImportanceComparison() { return featureImportanceComparison; }
        public Map<String, Integer> getClassificationDiscrepancies() { return classificationDiscrepancies; }
        public double getRuleBasedPrecision() { return ruleBasedPrecision; }
        public void setRuleBasedPrecision(double ruleBasedPrecision) { this.ruleBasedPrecision = ruleBasedPrecision; }
        public double getStatisticalPrecision() { return statisticalPrecision; }
        public void setStatisticalPrecision(double statisticalPrecision) { this.statisticalPrecision = statisticalPrecision; }
        public double getRuleBasedRecall() { return ruleBasedRecall; }
        public void setRuleBasedRecall(double ruleBasedRecall) { this.ruleBasedRecall = ruleBasedRecall; }
        public double getStatisticalRecall() { return statisticalRecall; }
        public void setStatisticalRecall(double statisticalRecall) { this.statisticalRecall = statisticalRecall; }
        
        public void addResult(ClassificationResult result) {
            results.add(result);
        }
    }
    
    private final SemanticTripleExtractor tripleExtractor;
    
    public ClassificationComparisonFramework() {
        this.tripleExtractor = new SemanticTripleExtractor();
    }
    
    /**
     * Perform comprehensive comparison between rule-based and statistical classification
     */
    public ComparisonAnalysis compareClassificationApproaches(List<DocumentInfo> documents) {
        ComparisonAnalysis analysis = new ComparisonAnalysis();
        
        for (DocumentInfo doc : documents) {
            ClassificationResult result = classifyDocument(doc);
            analysis.addResult(result);
        }
        
        // Calculate comparison metrics
        calculateCorrelationMetrics(analysis);
        calculateAgreementMetrics(analysis);
        calculatePerformanceMetrics(analysis);
        analyzeFeatureImportance(analysis);
        
        return analysis;
    }
    
    private ClassificationResult classifyDocument(DocumentInfo doc) {
        ClassificationResult result = new ClassificationResult(doc.getFileName());
        
        // Rule-based classification
        performRuleBasedClassification(doc, result);
        
        // Statistical classification (simple TF-IDF based approach)
        performStatisticalClassification(doc, result);
        
        return result;
    }
    
    private void performRuleBasedClassification(DocumentInfo doc, ClassificationResult result) {
        // Extract semantic triples from document abstract and content
        String documentText = (doc.getAbstractText() != null ? doc.getAbstractText() : "") + 
                             " " + extractContentFromDocument(doc);
        
        SemanticTripleExtractor.TripleExtractionResult tripleResult = 
            tripleExtractor.extractTriples(documentText);
        
        // Rule-based features
        Map<String, Double> features = result.getRuleBasedFeatures();
        
        // Feature 1: Semantic triple density
        int totalTriples = tripleResult.getParagraphLevelTriples().size() + 
                          tripleResult.getSentenceLevelTriples().size();
        features.put("triple_density", (double) totalTriples / Math.max(1, doc.getTotalWordCount()));
        
        // Feature 2: Explosive-relevant triple ratio
        List<SemanticTripleExtractor.SemanticTriple> allTriples = new ArrayList<>();
        allTriples.addAll(tripleResult.getParagraphLevelTriples());
        allTriples.addAll(tripleResult.getSentenceLevelTriples());
        
        List<SemanticTripleExtractor.SemanticTriple> explosiveTriples = 
            tripleExtractor.filterExplosiveRelevantTriples(allTriples);
        
        features.put("explosive_triple_ratio", 
                    totalTriples > 0 ? (double) explosiveTriples.size() / totalTriples : 0.0);
        
        // Feature 3: Average triple confidence
        double avgConfidence = allTriples.stream()
            .mapToDouble(SemanticTripleExtractor.SemanticTriple::getConfidence)
            .average().orElse(0.0);
        features.put("avg_triple_confidence", avgConfidence);
        
        // Feature 4: Predicate diversity
        Set<String> uniquePredicates = allTriples.stream()
            .map(SemanticTripleExtractor.SemanticTriple::getPredicate)
            .collect(Collectors.toSet());
        features.put("predicate_diversity", (double) uniquePredicates.size());
        
        // Feature 5: Subject-object coherence (entities appearing in both roles)
        Set<String> subjects = allTriples.stream()
            .map(SemanticTripleExtractor.SemanticTriple::getSubject)
            .collect(Collectors.toSet());
        Set<String> objects = allTriples.stream()
            .map(SemanticTripleExtractor.SemanticTriple::getObject)
            .collect(Collectors.toSet());
        
        Set<String> coherentEntities = new HashSet<>(subjects);
        coherentEntities.retainAll(objects);
        features.put("entity_coherence", (double) coherentEntities.size() / 
                    Math.max(1, subjects.size() + objects.size()));
        
        // Calculate rule-based score
        double ruleBasedScore = calculateRuleBasedScore(features, doc);
        result.setRuleBasedScore(ruleBasedScore);
        
        // Rule-based classifications
        classifyBasedOnRules(features, doc, result);
    }
    
    private void performStatisticalClassification(DocumentInfo doc, ClassificationResult result) {
        Map<String, Double> features = result.getStatisticalFeatures();
        
        // Statistical features based on keyword analysis
        if (doc.getKeywordCounts() != null) {
            // Feature 1: Total keyword frequency
            int totalKeywords = doc.getKeywordCounts().values().stream().mapToInt(Integer::intValue).sum();
            features.put("keyword_frequency", (double) totalKeywords / Math.max(1, doc.getTotalWordCount()));
            
            // Feature 2: Unique keyword ratio
            features.put("unique_keyword_ratio", (double) doc.getKeywordCounts().size() / Math.max(1, totalKeywords));
            
            // Feature 3: Top keyword dominance
            int maxKeywordCount = doc.getKeywordCounts().values().stream().mapToInt(Integer::intValue).max().orElse(0);
            features.put("top_keyword_dominance", (double) maxKeywordCount / Math.max(1, totalKeywords));
            
            // Feature 4: Keyword distribution entropy
            features.put("keyword_entropy", calculateEntropy(doc.getKeywordCounts()));
        }
        
        // Feature 5: Document length normalized score
        features.put("length_normalized_score", doc.getExplosiveContentScore() / Math.log(Math.max(10, doc.getTotalWordCount())));
        
        // Feature 6: Title keyword presence
        features.put("title_keyword_presence", titleContainsKeywords(doc) ? 1.0 : 0.0);
        
        // Calculate statistical score
        double statisticalScore = calculateStatisticalScore(features, doc);
        result.setStatisticalScore(statisticalScore);
        
        // Statistical classifications
        classifyBasedOnStatistics(features, doc, result);
    }
    
    private double calculateRuleBasedScore(Map<String, Double> features, DocumentInfo doc) {
        // Weighted combination of rule-based features
        double score = 0.0;
        score += features.getOrDefault("explosive_triple_ratio", 0.0) * 0.3;
        score += features.getOrDefault("avg_triple_confidence", 0.0) * 0.25;
        score += Math.min(1.0, features.getOrDefault("triple_density", 0.0) * 100) * 0.2;
        score += Math.min(1.0, features.getOrDefault("predicate_diversity", 0.0) / 10) * 0.15;
        score += features.getOrDefault("entity_coherence", 0.0) * 0.1;
        
        return score;
    }
    
    private double calculateStatisticalScore(Map<String, Double> features, DocumentInfo doc) {
        // Weighted combination of statistical features
        double score = 0.0;
        score += Math.min(1.0, features.getOrDefault("keyword_frequency", 0.0) * 50) * 0.3;
        score += features.getOrDefault("unique_keyword_ratio", 0.0) * 0.2;
        score += (1.0 - features.getOrDefault("top_keyword_dominance", 0.0)) * 0.15; // Diversity is good
        score += Math.min(1.0, features.getOrDefault("keyword_entropy", 0.0) / 3) * 0.15;
        score += Math.min(1.0, features.getOrDefault("length_normalized_score", 0.0) / 10) * 0.15;
        score += features.getOrDefault("title_keyword_presence", 0.0) * 0.05;
        
        return score;
    }
    
    private void classifyBasedOnRules(Map<String, Double> features, DocumentInfo doc, ClassificationResult result) {
        List<String> classifications = result.getRuleBasedClassifications();
        
        // Classification based on semantic structure
        if (features.getOrDefault("explosive_triple_ratio", 0.0) > 0.3) {
            classifications.add("high_explosive_semantic_content");
        }
        
        if (features.getOrDefault("avg_triple_confidence", 0.0) > 0.7) {
            classifications.add("high_confidence_relationships");
        }
        
        if (features.getOrDefault("entity_coherence", 0.0) > 0.2) {
            classifications.add("coherent_explosive_narrative");
        }
        
        if (features.getOrDefault("predicate_diversity", 0.0) > 5) {
            classifications.add("diverse_explosive_processes");
        }
    }
    
    private void classifyBasedOnStatistics(Map<String, Double> features, DocumentInfo doc, ClassificationResult result) {
        List<String> classifications = result.getStatisticalClassifications();
        
        // Classification based on statistical features
        if (features.getOrDefault("keyword_frequency", 0.0) > 0.02) {
            classifications.add("high_keyword_density");
        }
        
        if (features.getOrDefault("unique_keyword_ratio", 0.0) > 0.5) {
            classifications.add("diverse_explosive_terminology");
        }
        
        if (features.getOrDefault("title_keyword_presence", 0.0) > 0.5) {
            classifications.add("title_indicates_explosive_content");
        }
        
        if (doc.getExplosiveContentScore() > 5.0) {
            classifications.add("high_statistical_explosive_score");
        }
    }
    
    private void calculateCorrelationMetrics(ComparisonAnalysis analysis) {
        List<ClassificationResult> results = analysis.getResults();
        
        if (results.size() < 2) {
            analysis.setAverageScoreCorrelation(0.0);
            return;
        }
        
        // Calculate Pearson correlation between rule-based and statistical scores
        double[] ruleScores = results.stream().mapToDouble(ClassificationResult::getRuleBasedScore).toArray();
        double[] statScores = results.stream().mapToDouble(ClassificationResult::getStatisticalScore).toArray();
        
        double correlation = calculatePearsonCorrelation(ruleScores, statScores);
        analysis.setAverageScoreCorrelation(correlation);
    }
    
    private void calculateAgreementMetrics(ComparisonAnalysis analysis) {
        List<ClassificationResult> results = analysis.getResults();
        
        long agreementCount = results.stream()
            .mapToLong(result -> result.hasClassificationAgreement() ? 1 : 0)
            .sum();
        
        double agreementRate = (double) agreementCount / results.size();
        analysis.setClassificationAgreementRate(agreementRate);
        
        // Track specific discrepancies
        for (ClassificationResult result : results) {
            if (!result.hasClassificationAgreement()) {
                String discrepancy = "Rule: " + result.getRuleBasedClassifications() + 
                                   " vs Stat: " + result.getStatisticalClassifications();
                analysis.getClassificationDiscrepancies().merge(discrepancy, 1, Integer::sum);
            }
        }
    }
    
    private void calculatePerformanceMetrics(ComparisonAnalysis analysis) {
        // If ground truth is available, calculate precision/recall
        List<ClassificationResult> results = analysis.getResults();
        
        // For now, use high explosive content score as pseudo ground truth
        List<ClassificationResult> groundTruthPositives = results.stream()
            .filter(r -> r.getStatisticalScore() > 0.5) // Arbitrary threshold
            .collect(Collectors.toList());
        
        // Calculate precision and recall for both approaches
        // This is a simplified example - in practice you'd have real ground truth
        analysis.setRuleBasedPrecision(0.0); // Placeholder
        analysis.setStatisticalPrecision(0.0); // Placeholder
        analysis.setRuleBasedRecall(0.0); // Placeholder
        analysis.setStatisticalRecall(0.0); // Placeholder
    }
    
    private void analyzeFeatureImportance(ComparisonAnalysis analysis) {
        List<ClassificationResult> results = analysis.getResults();
        Map<String, Double> importance = analysis.getFeatureImportanceComparison();
        
        // Calculate feature importance based on variance and correlation with final scores
        Set<String> allFeatures = new HashSet<>();
        results.forEach(r -> {
            allFeatures.addAll(r.getRuleBasedFeatures().keySet());
            allFeatures.addAll(r.getStatisticalFeatures().keySet());
        });
        
        for (String feature : allFeatures) {
            double variance = calculateFeatureVariance(results, feature);
            importance.put(feature, variance);
        }
    }
    
    // Helper methods
    private String extractContentFromDocument(DocumentInfo doc) {
        // In a real implementation, this would extract full document text
        return doc.getAbstractText() != null ? doc.getAbstractText() : "";
    }
    
    private double calculateEntropy(Map<String, Integer> distribution) {
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return 0.0;
        
        return distribution.values().stream()
            .mapToDouble(count -> {
                double p = (double) count / total;
                return p > 0 ? -p * Math.log(p) / Math.log(2) : 0;
            })
            .sum();
    }
    
    private boolean titleContainsKeywords(DocumentInfo doc) {
        if (doc.getTitle() == null || doc.getMatchedExplosiveTerms() == null) return false;
        String lowerTitle = doc.getTitle().toLowerCase();
        return doc.getMatchedExplosiveTerms().stream()
            .anyMatch(keyword -> lowerTitle.contains(keyword.toLowerCase()));
    }
    
    private double calculatePearsonCorrelation(double[] x, double[] y) {
        if (x.length != y.length) return 0.0;
        
        double meanX = Arrays.stream(x).average().orElse(0.0);
        double meanY = Arrays.stream(y).average().orElse(0.0);
        
        double numerator = 0.0, sumX2 = 0.0, sumY2 = 0.0;
        
        for (int i = 0; i < x.length; i++) {
            double deltaX = x[i] - meanX;
            double deltaY = y[i] - meanY;
            numerator += deltaX * deltaY;
            sumX2 += deltaX * deltaX;
            sumY2 += deltaY * deltaY;
        }
        
        double denominator = Math.sqrt(sumX2 * sumY2);
        return denominator > 0 ? numerator / denominator : 0.0;
    }
    
    private double calculateFeatureVariance(List<ClassificationResult> results, String feature) {
        List<Double> values = new ArrayList<>();
        
        for (ClassificationResult result : results) {
            Double value = result.getRuleBasedFeatures().get(feature);
            if (value == null) {
                value = result.getStatisticalFeatures().get(feature);
            }
            if (value != null) {
                values.add(value);
            }
        }
        
        if (values.size() < 2) return 0.0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
    }
}