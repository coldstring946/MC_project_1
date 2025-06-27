package uk.gov.gchq.magma.xml.analyzer;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple test runner to demonstrate and test the analysis components
 */
public class TestRunner {
    
    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        
        try {
            // Test with sample data directory or current directory
            String testDir = args.length > 0 ? args[0] : ".";
            runner.runComprehensiveTest(testDir);
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void runComprehensiveTest(String directoryPath) throws Exception {
        System.out.println("=== MagmaCore XML Document Analysis Test ===\n");
        
        // Initialize components
        XmlDocumentScanner scanner = new XmlDocumentScanner();
        TemporalKeywordAnalyzer temporalAnalyzer = new TemporalKeywordAnalyzer();
        SemanticTripleExtractor tripleExtractor = new SemanticTripleExtractor();
        ClassificationComparisonFramework comparisonFramework = new ClassificationComparisonFramework();
        
        // Find XML files
        List<File> xmlFiles = findXmlFiles(directoryPath);
        System.out.println("Found " + xmlFiles.size() + " XML files to analyze\n");
        
        if (xmlFiles.isEmpty()) {
            System.out.println("No XML files found. Creating sample data for testing...");
            testWithSampleData(scanner, temporalAnalyzer, tripleExtractor, comparisonFramework);
            return;
        }
        
        // Process documents
        List<DocumentInfo> documents = new ArrayList<>();
        for (File xmlFile : xmlFiles) {
            try {
                System.out.println("Processing: " + xmlFile.getName());
                DocumentInfo doc = scanner.scanDocument(xmlFile);
                documents.add(doc);
                
                // Print basic info
                System.out.println("  Title: " + doc.getTitle());
                System.out.println("  Authors: " + doc.getAuthors());
                System.out.println("  Explosive Score: " + String.format("%.2f", doc.getExplosiveContentScore()));
                System.out.println("  Matched Terms: " + doc.getMatchedExplosiveTerms());
                System.out.println();
                
            } catch (Exception e) {
                System.err.println("Failed to process " + xmlFile.getName() + ": " + e.getMessage());
            }
        }
        
        if (documents.isEmpty()) {
            System.out.println("No documents were successfully processed.");
            return;
        }
        
        // Run temporal analysis
        System.out.println("\n=== Temporal Keyword Analysis ===");
        runTemporalAnalysis(documents, temporalAnalyzer);
        
        // Run semantic triple extraction
        System.out.println("\n=== Semantic Triple Extraction ===");
        runSemanticTripleAnalysis(documents, tripleExtractor);
        
        // Run classification comparison
        System.out.println("\n=== Classification Comparison ===");
        runClassificationComparison(documents, comparisonFramework);
    }
    
    private void runTemporalAnalysis(List<DocumentInfo> documents, TemporalKeywordAnalyzer analyzer) {
        TemporalKeywordAnalyzer.TemporalAnalysisResult result = analyzer.analyzeKeywordTrends(documents, "YEAR");
        
        System.out.println("Temporal Analysis Results:");
        System.out.println("- Total documents: " + result.getTotalDocuments());
        System.out.println("- Time granularity: " + result.getTimeGranularity());
        System.out.println("- Unique keywords tracked: " + result.getKeywordTrends().size());
        
        System.out.println("\nEmerging Keywords (top 5):");
        result.getEmergingKeywords().stream().limit(5).forEach(keyword -> {
            TemporalKeywordAnalyzer.KeywordTrend trend = result.getKeywordTrends().get(keyword);
            System.out.println("  " + keyword + " (trend: " + String.format("%.3f", trend.getOverallTrend()) + ")");
        });
        
        System.out.println("\nDeclining Keywords (top 5):");
        result.getDecliningKeywords().stream().limit(5).forEach(keyword -> {
            TemporalKeywordAnalyzer.KeywordTrend trend = result.getKeywordTrends().get(keyword);
            System.out.println("  " + keyword + " (trend: " + String.format("%.3f", trend.getOverallTrend()) + ")");
        });
    }
    
    private void runSemanticTripleAnalysis(List<DocumentInfo> documents, SemanticTripleExtractor extractor) {
        int totalTriples = 0;
        int totalExplosiveTriples = 0;
        
        System.out.println("Semantic Triple Extraction Results:");
        
        for (DocumentInfo doc : documents.stream().limit(3).toList()) { // Limit to first 3 for demo
            String text = doc.getAbstractText() != null ? doc.getAbstractText() : "";
            if (text.isEmpty()) continue;
            
            SemanticTripleExtractor.TripleExtractionResult result = extractor.extractTriples(text);
            
            System.out.println("\nDocument: " + doc.getFileName());
            System.out.println("  Paragraph-level triples: " + result.getParagraphLevelTriples().size());
            System.out.println("  Sentence-level triples: " + result.getSentenceLevelTriples().size());
            
            // Show some example triples
            List<SemanticTripleExtractor.SemanticTriple> allTriples = new ArrayList<>();
            allTriples.addAll(result.getParagraphLevelTriples());
            allTriples.addAll(result.getSentenceLevelTriples());
            
            List<SemanticTripleExtractor.SemanticTriple> explosiveTriples = 
                extractor.filterExplosiveRelevantTriples(allTriples);
            
            System.out.println("  Explosive-relevant triples: " + explosiveTriples.size());
            
            totalTriples += allTriples.size();
            totalExplosiveTriples += explosiveTriples.size();
            
            // Show top 3 triples by confidence
            allTriples.stream()
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(3)
                .forEach(triple -> System.out.println("    " + triple));
        }
        
        System.out.println("\nOverall Statistics:");
        System.out.println("  Total triples extracted: " + totalTriples);
        System.out.println("  Explosive-relevant triples: " + totalExplosiveTriples);
        if (totalTriples > 0) {
            System.out.println("  Explosive relevance ratio: " + 
                String.format("%.2f%%", (double) totalExplosiveTriples / totalTriples * 100));
        }
    }
    
    private void runClassificationComparison(List<DocumentInfo> documents, ClassificationComparisonFramework framework) {
        ClassificationComparisonFramework.ComparisonAnalysis analysis = 
            framework.compareClassificationApproaches(documents);
        
        System.out.println("Classification Comparison Results:");
        System.out.println("- Documents analyzed: " + analysis.getResults().size());
        System.out.println("- Score correlation: " + String.format("%.3f", analysis.getAverageScoreCorrelation()));
        System.out.println("- Classification agreement rate: " + 
            String.format("%.2f%%", analysis.getClassificationAgreementRate() * 100));
        
        // Show some detailed results
        System.out.println("\nDetailed Results (first 5 documents):");
        analysis.getResults().stream().limit(5).forEach(result -> {
            System.out.println("  Document: " + result.getDocumentId());
            System.out.println("    Rule-based score: " + String.format("%.3f", result.getRuleBasedScore()));
            System.out.println("    Statistical score: " + String.format("%.3f", result.getStatisticalScore()));
            System.out.println("    Score difference: " + String.format("%.3f", result.getScoreDifference()));
            System.out.println("    Classification agreement: " + result.hasClassificationAgreement());
            System.out.println("    Rule-based classifications: " + result.getRuleBasedClassifications());
            System.out.println("    Statistical classifications: " + result.getStatisticalClassifications());
            System.out.println();
        });
        
        // Feature importance
        System.out.println("Feature Importance Analysis:");
        analysis.getFeatureImportanceComparison().entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + 
                String.format("%.4f", entry.getValue())));
    }
    
    private List<File> findXmlFiles(String directoryPath) {
        List<File> xmlFiles = new ArrayList<>();
        File dir = new File(directoryPath);
        
        if (dir.isDirectory()) {
            File[] files = dir.listFiles((file, name) -> name.toLowerCase().endsWith(".xml"));
            if (files != null) {
                for (File file : files) {
                    xmlFiles.add(file);
                }
            }
        } else if (dir.exists() && dir.getName().toLowerCase().endsWith(".xml")) {
            xmlFiles.add(dir);
        }
        
        return xmlFiles;
    }
    
    private void testWithSampleData(XmlDocumentScanner scanner, 
                                   TemporalKeywordAnalyzer temporalAnalyzer,
                                   SemanticTripleExtractor tripleExtractor,
                                   ClassificationComparisonFramework comparisonFramework) {
        
        System.out.println("Running test with sample data...\n");
        
        // Create sample documents
        List<DocumentInfo> sampleDocs = createSampleDocuments();
        
        // Test temporal analysis
        System.out.println("=== Sample Temporal Analysis ===");
        TemporalKeywordAnalyzer.TemporalAnalysisResult temporalResult = 
            temporalAnalyzer.analyzeKeywordTrends(sampleDocs, "YEAR");
        
        System.out.println("Sample temporal analysis completed with " + 
            temporalResult.getKeywordTrends().size() + " keywords tracked");
        
        // Test semantic triple extraction
        System.out.println("\n=== Sample Semantic Triple Extraction ===");
        String sampleText = "TNT detonates at high temperature. The explosive shows increased sensitivity to pressure. Nitroglycerine reacts with nitrocellulose to form propellant.";
        
        SemanticTripleExtractor.TripleExtractionResult tripleResult = tripleExtractor.extractTriples(sampleText);
        System.out.println("Extracted " + (tripleResult.getParagraphLevelTriples().size() + 
            tripleResult.getSentenceLevelTriples().size()) + " triples from sample text:");
        
        tripleResult.getSentenceLevelTriples().forEach(triple -> 
            System.out.println("  " + triple));
        
        // Test classification comparison
        System.out.println("\n=== Sample Classification Comparison ===");
        ClassificationComparisonFramework.ComparisonAnalysis comparisonResult = 
            comparisonFramework.compareClassificationApproaches(sampleDocs);
        
        System.out.println("Comparison analysis completed for " + 
            comparisonResult.getResults().size() + " sample documents");
        System.out.println("Score correlation: " + 
            String.format("%.3f", comparisonResult.getAverageScoreCorrelation()));
    }
    
    private List<DocumentInfo> createSampleDocuments() {
        List<DocumentInfo> docs = new ArrayList<>();
        
        // Create sample document 1
        DocumentInfo doc1 = new DocumentInfo("sample1.xml");
        doc1.setTitle("Analysis of TNT Detonation Properties");
        doc1.setAbstractText("This study examines the detonation characteristics of TNT under various pressure and temperature conditions. The explosive shows increased sensitivity at elevated temperatures.");
        doc1.setPublicationDate(LocalDateTime.of(2020, 1, 15, 0, 0));
        doc1.setExplosiveContentScore(8.5);
        doc1.setTotalWordCount(150);
        Map<String, Integer> keywords1 = Map.of("TNT", 5, "detonation", 3, "explosive", 4, "temperature", 2, "pressure", 2);
        doc1.setKeywordCounts(keywords1);
        doc1.setMatchedExplosiveTerms(keywords1.keySet());
        docs.add(doc1);
        
        // Create sample document 2
        DocumentInfo doc2 = new DocumentInfo("sample2.xml");
        doc2.setTitle("Nitroglycerine Migration in Propellant Systems");
        doc2.setAbstractText("Investigation of nitroglycerine migration patterns in double base propellants. The study focuses on plasticizer effects and thermal decomposition processes.");
        doc2.setPublicationDate(LocalDateTime.of(2021, 6, 10, 0, 0));
        doc2.setExplosiveContentScore(7.2);
        doc2.setTotalWordCount(200);
        Map<String, Integer> keywords2 = Map.of("nitroglycerine", 8, "propellant", 6, "migration", 4, "plasticizer", 3, "decomposition", 2);
        doc2.setKeywordCounts(keywords2);
        doc2.setMatchedExplosiveTerms(keywords2.keySet());
        docs.add(doc2);
        
        // Create sample document 3
        DocumentInfo doc3 = new DocumentInfo("sample3.xml");
        doc3.setTitle("Synthesis of Novel Halolactone Explosives");
        doc3.setAbstractText("Novel halolactone compounds were synthesized and evaluated for explosive properties. The compounds show promising detonation characteristics and thermal stability.");
        doc3.setPublicationDate(LocalDateTime.of(2022, 3, 20, 0, 0));
        doc3.setExplosiveContentScore(9.1);
        doc3.setTotalWordCount(180);
        Map<String, Integer> keywords3 = Map.of("halolactone", 4, "explosive", 6, "synthesis", 3, "detonation", 4, "thermal", 2, "stability", 2);
        doc3.setKeywordCounts(keywords3);
        doc3.setMatchedExplosiveTerms(keywords3.keySet());
        docs.add(doc3);
        
        // Create sample document 4
        DocumentInfo doc4 = new DocumentInfo("sample4.xml");
        doc4.setTitle("Ammonium Nitrate Sensitivity Analysis");
        doc4.setAbstractText("Comprehensive analysis of ammonium nitrate sensitivity to various stimuli. The study examines ignition thresholds and safety parameters for industrial applications.");
        doc4.setPublicationDate(LocalDateTime.of(2023, 1, 5, 0, 0));
        doc4.setExplosiveContentScore(6.8);
        doc4.setTotalWordCount(220);
        Map<String, Integer> keywords4 = Map.of("ammonium nitrate", 7, "sensitivity", 5, "ignition", 3, "explosive", 2, "safety", 3);
        doc4.setKeywordCounts(keywords4);
        doc4.setMatchedExplosiveTerms(keywords4.keySet());
        docs.add(doc4);
        
        return docs;
    }
}