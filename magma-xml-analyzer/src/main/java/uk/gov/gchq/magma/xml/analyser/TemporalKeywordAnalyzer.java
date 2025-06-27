package uk.gov.gchq.magma.xml.analyzer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes keyword trends and changes over time across document collections
 */
public class TemporalKeywordAnalyzer {
    
    public static class KeywordTrend {
        private String keyword;
        private Map<String, Integer> countByTimeInterval; // time interval -> count
        private double overallTrend; // positive = increasing, negative = decreasing
        private LocalDateTime firstSeen;
        private LocalDateTime lastSeen;
        
        public KeywordTrend(String keyword) {
            this.keyword = keyword;
            this.countByTimeInterval = new LinkedHashMap<>();
        }
        
        // Getters and setters
        public String getKeyword() { return keyword; }
        public Map<String, Integer> getCountByTimeInterval() { return countByTimeInterval; }
        public double getOverallTrend() { return overallTrend; }
        public void setOverallTrend(double overallTrend) { this.overallTrend = overallTrend; }
        public LocalDateTime getFirstSeen() { return firstSeen; }
        public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }
        public LocalDateTime getLastSeen() { return lastSeen; }
        public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
        
        public void addCount(String timeInterval, int count) {
            countByTimeInterval.merge(timeInterval, count, Integer::sum);
        }
    }
    
    public static class TemporalAnalysisResult {
        private Map<String, KeywordTrend> keywordTrends;
        private List<String> emergingKeywords;
        private List<String> decliningKeywords;
        private String timeGranularity;
        private int totalDocuments;
        
        public TemporalAnalysisResult(String timeGranularity) {
            this.timeGranularity = timeGranularity;
            this.keywordTrends = new HashMap<>();
            this.emergingKeywords = new ArrayList<>();
            this.decliningKeywords = new ArrayList<>();
        }
        
        // Getters
        public Map<String, KeywordTrend> getKeywordTrends() { return keywordTrends; }
        public List<String> getEmergingKeywords() { return emergingKeywords; }
        public List<String> getDecliningKeywords() { return decliningKeywords; }
        public String getTimeGranularity() { return timeGranularity; }
        public int getTotalDocuments() { return totalDocuments; }
        public void setTotalDocuments(int totalDocuments) { this.totalDocuments = totalDocuments; }
        
        public void addKeywordTrend(String keyword, KeywordTrend trend) {
            keywordTrends.put(keyword, trend);
        }
        
        public void setEmergingKeywords(List<String> emergingKeywords) {
            this.emergingKeywords = emergingKeywords;
        }
        
        public void setDecliningKeywords(List<String> decliningKeywords) {
            this.decliningKeywords = decliningKeywords;
        }
    }
    
    private final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    private final DateTimeFormatter quarterFormatter = DateTimeFormatter.ofPattern("yyyy-'Q'Q");
    
    /**
     * Analyze keyword trends over time with specified granularity
     * @param documents List of analyzed documents
     * @param granularity "YEAR", "QUARTER", or "MONTH"
     * @return Temporal analysis results
     */
    public TemporalAnalysisResult analyzeKeywordTrends(List<DocumentInfo> documents, String granularity) {
        TemporalAnalysisResult result = new TemporalAnalysisResult(granularity);
        result.setTotalDocuments(documents.size());
        
        // Group documents by time intervals
        Map<String, List<DocumentInfo>> documentsByInterval = groupDocumentsByTime(documents, granularity);
        
        // Collect all unique keywords
        Set<String> allKeywords = documents.stream()
            .filter(doc -> doc.getMatchedExplosiveTerms() != null)
            .flatMap(doc -> doc.getMatchedExplosiveTerms().stream())
            .collect(Collectors.toSet());
        
        // Calculate trends for each keyword
        for (String keyword : allKeywords) {
            KeywordTrend trend = calculateKeywordTrend(keyword, documentsByInterval);
            result.addKeywordTrend(keyword, trend);
        }
        
        // Identify emerging and declining keywords
        identifyTrendingKeywords(result);
        
        return result;
    }
    
    private Map<String, List<DocumentInfo>> groupDocumentsByTime(List<DocumentInfo> documents, String granularity) {
        return documents.stream()
            .filter(doc -> doc.getPublicationDate() != null)
            .collect(Collectors.groupingBy(doc -> formatTimeInterval(doc.getPublicationDate(), granularity)));
    }
    
    private String formatTimeInterval(LocalDateTime date, String granularity) {
        switch (granularity.toUpperCase()) {
            case "YEAR":
                return date.format(yearFormatter);
            case "MONTH":
                return date.format(monthFormatter);
            case "QUARTER":
                int quarter = (date.getMonthValue() - 1) / 3 + 1;
                return date.getYear() + "-Q" + quarter;
            default:
                return date.format(yearFormatter);
        }
    }
    
    private KeywordTrend calculateKeywordTrend(String keyword, Map<String, List<DocumentInfo>> documentsByInterval) {
        KeywordTrend trend = new KeywordTrend(keyword);
        
        LocalDateTime firstSeen = null;
        LocalDateTime lastSeen = null;
        
        for (Map.Entry<String, List<DocumentInfo>> entry : documentsByInterval.entrySet()) {
            String timeInterval = entry.getKey();
            List<DocumentInfo> docs = entry.getValue();
            
            int keywordCount = docs.stream()
                .filter(doc -> doc.getKeywordCounts() != null && doc.getKeywordCounts().containsKey(keyword))
                .mapToInt(doc -> doc.getKeywordCounts().get(keyword))
                .sum();
            
            if (keywordCount > 0) {
                trend.addCount(timeInterval, keywordCount);
                
                // Track first and last seen dates
                for (DocumentInfo doc : docs) {
                    if (doc.getKeywordCounts() != null && doc.getKeywordCounts().containsKey(keyword)) {
                        LocalDateTime pubDate = doc.getPublicationDate();
                        if (firstSeen == null || pubDate.isBefore(firstSeen)) {
                            firstSeen = pubDate;
                        }
                        if (lastSeen == null || pubDate.isAfter(lastSeen)) {
                            lastSeen = pubDate;
                        }
                    }
                }
            }
        }
        
        trend.setFirstSeen(firstSeen);
        trend.setLastSeen(lastSeen);
        trend.setOverallTrend(calculateTrendSlope(trend.getCountByTimeInterval()));
        
        return trend;
    }
    
    private double calculateTrendSlope(Map<String, Integer> countByInterval) {
        if (countByInterval.size() < 2) return 0.0;
        
        List<String> sortedIntervals = new ArrayList<>(countByInterval.keySet());
        Collections.sort(sortedIntervals);
        
        // Simple linear regression to calculate slope
        int n = sortedIntervals.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i; // time index
            double y = countByInterval.get(sortedIntervals.get(i));
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        // Calculate slope: (n*ΣXY - ΣX*ΣY) / (n*ΣX² - (ΣX)²)
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) return 0.0;
        
        return (n * sumXY - sumX * sumY) / denominator;
    }
    
    private void identifyTrendingKeywords(TemporalAnalysisResult result) {
        double trendThreshold = 0.1; // Minimum slope to consider as trending
        
        List<String> emerging = result.getKeywordTrends().entrySet().stream()
            .filter(entry -> entry.getValue().getOverallTrend() > trendThreshold)
            .sorted((a, b) -> Double.compare(b.getValue().getOverallTrend(), a.getValue().getOverallTrend()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        List<String> declining = result.getKeywordTrends().entrySet().stream()
            .filter(entry -> entry.getValue().getOverallTrend() < -trendThreshold)
            .sorted((a, b) -> Double.compare(a.getValue().getOverallTrend(), b.getValue().getOverallTrend()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        result.setEmergingKeywords(emerging);
        result.setDecliningKeywords(declining);
    }
    
    /**
     * Compare keyword usage between two time periods
     */
    public Map<String, Double> compareTimePeriods(List<DocumentInfo> documents, 
                                                 LocalDateTime period1Start, LocalDateTime period1End,
                                                 LocalDateTime period2Start, LocalDateTime period2End) {
        
        List<DocumentInfo> period1Docs = documents.stream()
            .filter(doc -> doc.getPublicationDate() != null)
            .filter(doc -> !doc.getPublicationDate().isBefore(period1Start) && 
                          !doc.getPublicationDate().isAfter(period1End))
            .collect(Collectors.toList());
        
        List<DocumentInfo> period2Docs = documents.stream()
            .filter(doc -> doc.getPublicationDate() != null)
            .filter(doc -> !doc.getPublicationDate().isBefore(period2Start) && 
                          !doc.getPublicationDate().isAfter(period2End))
            .collect(Collectors.toList());
        
        Map<String, Integer> period1Counts = aggregateKeywordCounts(period1Docs);
        Map<String, Integer> period2Counts = aggregateKeywordCounts(period2Docs);
        
        // Calculate relative change for each keyword
        Map<String, Double> changes = new HashMap<>();
        Set<String> allKeywords = new HashSet<>(period1Counts.keySet());
        allKeywords.addAll(period2Counts.keySet());
        
        for (String keyword : allKeywords) {
            int count1 = period1Counts.getOrDefault(keyword, 0);
            int count2 = period2Counts.getOrDefault(keyword, 0);
            
            double change;
            if (count1 == 0) {
                change = count2 > 0 ? Double.POSITIVE_INFINITY : 0.0;
            } else {
                change = ((double) count2 - count1) / count1;
            }
            changes.put(keyword, change);
        }
        
        return changes;
    }
    
    private Map<String, Integer> aggregateKeywordCounts(List<DocumentInfo> documents) {
        Map<String, Integer> aggregated = new HashMap<>();
        
        for (DocumentInfo doc : documents) {
            if (doc.getKeywordCounts() != null) {
                for (Map.Entry<String, Integer> entry : doc.getKeywordCounts().entrySet()) {
                    aggregated.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        
        return aggregated;
    }
}