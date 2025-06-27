import uk.gov.gchq.hqdm.model.*;
import uk.gov.gchq.hqdm.services.SpatioTemporalExtentServices;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Simple XML to HQDM parser for PLoS research articles
 * Extracts basic metadata and creates HQDM entities
 */
public class XMLToHQDMParser {
    
    private static final String BASE_IRI = "http://research.example.org/";
    private final Set<HqdmObject> hqdmObjects = new HashSet<>();
    private final Model rdfModel = ModelFactory.createDefaultModel();
    
    public static void main(String[] args) {
        XMLToHQDMParser parser = new XMLToHQDMParser();
        
        try {
            List<File> xmlFiles = loadSampledFiles("sampled-files.txt");
            System.out.println("Loading " + xmlFiles.size() + " XML files...");
            
            parser.processXMLFiles(xmlFiles);
            parser.exportResults();
            
        } catch (Exception e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load the list of sampled XML files
     */
    private static List<File> loadSampledFiles(String listFile) throws IOException {
        return Files.readAllLines(Paths.get(listFile))
                .stream()
                .map(File::new)
                .filter(File::exists)
                .collect(ArrayList::new, (list, file) -> list.add(file), List::addAll);
    }
    
    /**
     * Process all XML files and create HQDM entities
     */
    public void processXMLFiles(List<File> xmlFiles) {
        int processed = 0;
        int errors = 0;
        
        for (File xmlFile : xmlFiles) {
            try {
                processXMLFile(xmlFile);
                processed++;
                
                if (processed % 50 == 0) {
                    System.out.println("Processed " + processed + " files...");
                }
                
            } catch (Exception e) {
                errors++;
                System.err.println("Error processing " + xmlFile.getName() + ": " + e.getMessage());
            }
        }
        
        System.out.println("\n=== PROCESSING COMPLETE ===");
        System.out.println("Successfully processed: " + processed + " files");
        System.out.println("Errors: " + errors + " files");
        System.out.println("Total HQDM objects created: " + hqdmObjects.size());
    }
    
    /**
     * Process a single XML file and extract metadata
     */
    private void processXMLFile(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        
        // Extract basic metadata
        ArticleMetadata metadata = extractArticleMetadata(doc);
        
        // Create HQDM entities
        createHQDMEntities(metadata, xmlFile);
    }
    
    /**
     * Extract basic article metadata from XML
     */
    private ArticleMetadata extractArticleMetadata(Document doc) {
        ArticleMetadata metadata = new ArticleMetadata();
        
        // DOI
        NodeList doiNodes = doc.getElementsByTagName("article-id");
        for (int i = 0; i < doiNodes.getLength(); i++) {
            Element doiEl = (Element) doiNodes.item(i);
            if ("doi".equals(doiEl.getAttribute("pub-id-type"))) {
                metadata.doi = doiEl.getTextContent().trim();
                break;
            }
        }
        
        // Title
        NodeList titleNodes = doc.getElementsByTagName("article-title");
        if (titleNodes.getLength() > 0) {
            metadata.title = titleNodes.item(0).getTextContent().trim();
        }
        
        // Journal
        NodeList journalNodes = doc.getElementsByTagName("journal-title");
        if (journalNodes.getLength() > 0) {
            metadata.journal = journalNodes.item(0).getTextContent().trim();
        }
        
        // Publication date
        metadata.publicationDate = extractPublicationDate(doc);
        
        // Authors
        metadata.authors = extractAuthors(doc);
        
        // Keywords/Subjects
        metadata.subjects = extractSubjects(doc);
        
        return metadata;
    }
    
    /**
     * Extract publication date
     */
    private String extractPublicationDate(Document doc) {
        NodeList pubDates = doc.getElementsByTagName("pub-date");
        for (int i = 0; i < pubDates.getLength(); i++) {
            Element pubDate = (Element) pubDates.item(i);
            if ("epub".equals(pubDate.getAttribute("pub-type")) || 
                "ppub".equals(pubDate.getAttribute("pub-type"))) {
                
                String year = getElementText(pubDate, "year");
                String month = getElementText(pubDate, "month");
                String day = getElementText(pubDate, "day");
                
                if (year != null) {
                    return year + (month != null ? "-" + String.format("%02d", Integer.parseInt(month)) : "") +
                           (day != null ? "-" + String.format("%02d", Integer.parseInt(day)) : "");
                }
            }
        }
        return null;
    }
    
    /**
     * Extract authors
     */
    private List<AuthorInfo> extractAuthors(Document doc) {
        List<AuthorInfo> authors = new ArrayList<>();
        NodeList contribGroups = doc.getElementsByTagName("contrib-group");
        
        for (int i = 0; i < contribGroups.getLength(); i++) {
            Element contribGroup = (Element) contribGroups.item(i);
            NodeList contribs = contribGroup.getElementsByTagName("contrib");
            
            for (int j = 0; j < contribs.getLength(); j++) {
                Element contrib = (Element) contribs.item(j);
                if ("author".equals(contrib.getAttribute("contrib-type"))) {
                    AuthorInfo author = new AuthorInfo();
                    
                    // Name
                    Element nameEl = (Element) contrib.getElementsByTagName("name").item(0);
                    if (nameEl != null) {
                        author.surname = getElementText(nameEl, "surname");
                        author.givenNames = getElementText(nameEl, "given-names");
                    }
                    
                    // Affiliation (simplified)
                    NodeList affRefs = contrib.getElementsByTagName("xref");
                    for (int k = 0; k < affRefs.getLength(); k++) {
                        Element affRef = (Element) affRefs.item(k);
                        if ("aff".equals(affRef.getAttribute("ref-type"))) {
                            author.affiliationRef = affRef.getAttribute("rid");
                            break;
                        }
                    }
                    
                    authors.add(author);
                }
            }
        }
        
        return authors;
    }
    
    /**
     * Extract subjects/keywords
     */
    private List<String> extractSubjects(Document doc) {
        List<String> subjects = new ArrayList<>();
        NodeList subjGroups = doc.getElementsByTagName("subj-group");
        
        for (int i = 0; i < subjGroups.getLength(); i++) {
            Element subjGroup = (Element) subjGroups.item(i);
            NodeList subjectNodes = subjGroup.getElementsByTagName("subject");
            
            for (int j = 0; j < subjectNodes.getLength(); j++) {
                String subject = subjectNodes.item(j).getTextContent().trim();
                if (!subject.isEmpty()) {
                    subjects.add(subject);
                }
            }
        }
        
        return subjects;
    }
    
    /**
     * Helper method to get text content of a child element
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }
    
    /**
     * Create HQDM entities from extracted metadata
     */
    private void createHQDMEntities(ArticleMetadata metadata, File xmlFile) {
        try {
            // Create Article entity (Document)
            String articleIRI = BASE_IRI + "article/" + sanitizeForIRI(metadata.doi != null ? metadata.doi : xmlFile.getName());
            Document article = SpatioTemporalExtentServices.createDocument(articleIRI);
            
            // Add basic properties (simplified for now)
            if (metadata.title != null) {
                // Note: HQDM doesn't have direct title property, this is simplified
                // In a full implementation, we'd use proper HQDM patterns
            }
            
            hqdmObjects.add(article);
            
            // Create Person entities for authors
            for (AuthorInfo author : metadata.authors) {
                String personIRI = BASE_IRI + "person/" + sanitizeForIRI(author.surname + "_" + author.givenNames);
                Person person = SpatioTemporalExtentServices.createPerson(personIRI);
                hqdmObjects.add(person);
            }
            
            // Create Organization for journal
            if (metadata.journal != null) {
                String orgIRI = BASE_IRI + "organization/" + sanitizeForIRI(metadata.journal);
                Organization org = SpatioTemporalExtentServices.createOrganization(orgIRI);
                hqdmObjects.add(org);
            }
            
            // Create ClassOfEvent for subjects
            for (String subject : metadata.subjects) {
                String classIRI = BASE_IRI + "subject/" + sanitizeForIRI(subject);
                // Using a generic Class for subjects (simplified)
                Class subjectClass = SpatioTemporalExtentServices.createClass(classIRI);
                hqdmObjects.add(subjectClass);
            }
            
        } catch (Exception e) {
            System.err.println("Error creating HQDM entities for " + xmlFile.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Sanitize string for use in IRI
     */
    private String sanitizeForIRI(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
    
    /**
     * Export results to RDF and summary files
     */
    public void exportResults() throws IOException {
        // Convert HQDM objects to RDF (simplified)
        System.out.println("\nExporting results...");
        
        // Export summary statistics
        exportSummary();
        
        // Export to Turtle format (basic)
        exportToTurtle();
        
        System.out.println("Results exported to:");
        System.out.println("  - parsing-summary.txt");
        System.out.println("  - articles-basic.ttl");
    }
    
    /**
     * Export parsing summary
     */
    private void exportSummary() throws IOException {
        try (PrintWriter writer = new PrintWriter("parsing-summary.txt")) {
            writer.println("=== XML TO HQDM PARSING SUMMARY ===");
            writer.println("Generated: " + LocalDate.now());
            writer.println();
            writer.println("Total HQDM Objects Created: " + hqdmObjects.size());
            writer.println();
            
            // Count by type
            Map<String, Integer> typeCounts = new HashMap<>();
            for (HqdmObject obj : hqdmObjects) {
                String type = obj.getClass().getSimpleName();
                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            }
            
            writer.println("Objects by Type:");
            typeCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> writer.println("  " + entry.getKey() + ": " + entry.getValue()));
        }
    }
    
    /**
     * Export basic RDF/Turtle representation
     */
    private void exportToTurtle() throws IOException {
        try (FileOutputStream fos = new FileOutputStream("articles-basic.ttl")) {
            // This is a simplified export - in a full implementation,
            // we'd properly convert HQDM objects to RDF
            PrintWriter writer = new PrintWriter(fos);
            writer.println("@prefix ex: <" + BASE_IRI + "> .");
            writer.println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
            writer.println("@prefix hqdm: <https://github.com/hqdmTop/hqdmFramework/> .");
            writer.println();
            
            // Write basic triples for each object
            for (HqdmObject obj : hqdmObjects) {
                writer.println("<" + obj.getId() + "> rdf:type hqdm:" + obj.getClass().getSimpleName() + " .");
            }
            
            writer.flush();
        }
    }
    
    // Data classes for holding extracted metadata
    private static class ArticleMetadata {
        String doi;
        String title;
        String journal;
        String publicationDate;
        List<AuthorInfo> authors = new ArrayList<>();
        List<String> subjects = new ArrayList<>();
    }
    
    private static class AuthorInfo {
        String surname;
        String givenNames;
        String affiliationRef;
    }
}