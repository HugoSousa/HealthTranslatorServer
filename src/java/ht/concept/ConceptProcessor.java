/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.concept;

import ht.details.ExternalReference;
import ht.details.Relationship;
import ht.utils.LoggerFactory;
import ht.details.RelationshipExtractor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 *
 * @author Hugo
 */
public abstract class ConceptProcessor {
    
    protected Connection conn;
    public TokenizerModel tokenizerModel;
    protected ConcurrentHashMap<String, String> stopwords;
    protected HashSet<String> acceptedSemanticTypes;
    protected Matcher punctuationMatcher;
    protected Matcher numberMatcher;
    protected String code;
    protected CloseableHttpClient httpclient = HttpClients.createDefault();
    
    //recognizeCHVOnly - if set to false, concepts only found in CHV are not recognized
    public boolean recognizeOnlyCHV = true;
    
    //allAccepted: true - all semantic types must be accepted / false - at least one semantic type must be accepted
    public boolean allAccepted = false; 
    
    private static Logger logger;
    
    public ConceptProcessor(Connection conn){
        Pattern punctuationPattern = Pattern.compile("\\p{Punct}", Pattern.CASE_INSENSITIVE);
        Pattern numberPattern = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
        punctuationMatcher = punctuationPattern.matcher("");
        numberMatcher = numberPattern.matcher("");
        
        logger = LoggerFactory.createLogger(ConceptProcessor.class.getName());
        
        this.conn = conn;
    }

    public ConceptProcessor(Connection conn, ConcurrentHashMap<String, String> stopwords, TokenizerModel tokenizerModel, HashSet<String> acceptedSemanticTypes){
        Pattern punctuationPattern = Pattern.compile("\\p{Punct}", Pattern.CASE_INSENSITIVE);
        Pattern numberPattern = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
        punctuationMatcher = punctuationPattern.matcher("");
        numberMatcher = numberPattern.matcher("");
        
        logger = LoggerFactory.createLogger(ConceptProcessor.class.getName());
        this.tokenizerModel = tokenizerModel;
        this.conn = conn;
        this.stopwords = stopwords;
        this.acceptedSemanticTypes = acceptedSemanticTypes;
    }
    
    public void setAcceptedSemanticTypes(HashSet<String> semanticTypes){
        this.acceptedSemanticTypes = semanticTypes;
    }

    
    public void setStopwords(ConcurrentHashMap<String, String> stopwords){
        this.stopwords = stopwords;
    }
    
    protected boolean isAcceptedSemanticType(ArrayList<String> tuis) {
        ArrayList<String> copy = new ArrayList<>();
        
        for(int i = 0; i < tuis.size(); i++){
            String tui = tuis.get(i);
            if(acceptedSemanticTypes.contains(tui)){
                if(! allAccepted)
                    return true;
                else{
                    copy.add(tui);
                    if(copy.size() == tuis.size())
                        return true;
                }
            }else{
                if(allAccepted)
                    return false;
            }
        }
        return false;
    }
    
    public Concept processToken(Span[] spans, int i, String text, int forward_threshold){
        return null;
    };
    
    public String getDefinition(Concept concept){

        //String definition = null;
        
        //from wikipedia
        /*
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("https://" + code + ".wikipedia.org/w/api.php?action=query&prop=extracts&explaintext&redirects&exsentences=2&format=json&titles=" + concept.replace(" ", "%20"));
        CloseableHttpResponse response1 = null;
        try {
            response1 = httpclient.execute(httpGet);
        
            //System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String json = EntityUtils.toString(entity1, "UTF-8");
            //System.out.println("JSON: " + json);

            JsonReader reader = Json.createReader(new StringReader(json));
            JsonObject obj = reader.readObject();
            reader.close();
            
            JsonObject query = obj.getJsonObject("query");
            JsonObject pages = query.getJsonObject("pages");
            
            //TODO if there are multiple changes - disambiguation, how?
            if(pages.size() == 1){
                String key = (String)pages.keySet().toArray()[0];
                JsonObject page = pages.getJsonObject(key);
                //String title = page.getString("title");
                definition = page.getString("extract");
                
                //System.out.println("TITLE: " + title);
                //System.out.println("DEFINITION: " + definition);
            }
 
            EntityUtils.consume(entity1);
        } catch (IOException ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                response1.close();
            } catch (IOException ex) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        */
        
        //long startTime = System.nanoTime();
        /*
        //from DBpedia lookup local mirror
       
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost:1111/api/search/KeywordSearch?MaxHits=1&QueryString=" + concept.toLowerCase().replace(" ", "%20"));
        //HttpGet httpGet = new HttpGet("http://lookup.dbpedia.org//api/search/KeywordSearch?MaxHits=1&QueryString=" + concept.replace(" ", "%20"));
        httpGet.addHeader("Accept", "application/json");
        CloseableHttpResponse response1 = null;
        try {
            response1 = httpclient.execute(httpGet);
        
            //System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String json = EntityUtils.toString(entity1, "UTF-8");
            //System.out.println("JSON: " + json);

            JsonReader reader = Json.createReader(new StringReader(json));
            JsonObject obj = reader.readObject();
            reader.close();
            
            JsonArray results = obj.getJsonArray("results");
            
            //TODO if there are multiple changes - disambiguation, how?
            if(results.size() == 1){
                JsonObject result = results.getJsonObject(0);
                //String title = page.getString("title");
                definition = result.getString("description", "null");
                
                //System.out.println("TITLE: " + title);
                //System.out.println("DEFINITION: " + definition);
            }
 
            EntityUtils.consume(entity1);
        } catch (IOException ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                response1.close();
            } catch (IOException ex) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        */
        
        //get from database - depends on the 
        /*
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        String database = "umls_" + code;
        try {
            connMySQL.setCatalog(database);
            
            String query = "SELECT DEF FROM wikidef WHERE CUI = ?;";
            stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, concept.CUI);

            ResultSet rs = stmt.executeQuery();
            
            if(rs.next()){
                definition = rs.getString("DEF");
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(TokenProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        System.out.println("PROCESSING FOR TOKEN " + concept.string + " (" + concept.CUI + ")" +": " + duration + " ms");
        */
        return null;
        
    };
    
    protected boolean allResultsFromCHV(ResultSet rs){
        
        boolean result = true;
        try {
            while(rs.next()){
                if( ! rs.getString("SAB").equals("CHV")){
                    result = false;
                    break;
                }
            }
            
            rs.beforeFirst();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        return result;
    }
    
    public ArrayList<ExternalReference> getExternalReferences(Concept concept){
        return null;
    }
    
    public ArrayList<SemanticType> getSemanticTypes(String cui){
        return null;
    }
    
    public HashMap<String, HashSet<Relationship>> getRelationships(String sty, String cui){
        
        //relationships that don't point to metadata, interesting/understandable by health consumers, and not too broad 
        
        HashMap<String, List<String>> rules = new HashMap<>();
        HashMap<String, HashSet<Relationship>> rels;
        rules.put("empty", null);
        rules.put("same_as", null);
        rules.put("due_to", null);
        rules.put("cause_of", null);
        rules.put("inverse_isa", null);
        rules.put("isa", null);
        rules.put("has_finding_site", null);
        rules.put("finding_site_of", null);
        rules.put("has_causative_agent", null);
        rules.put("causative_agent_of", null);
        rules.put("has_part", null);
        rules.put("part_of", null);
        rules.put("has_associated_morphology", null);
        rules.put("associated_morphology_of", null);
        rules.put("uses", null);
        rules.put("used_by", null);
        rules.put("has_active_ingredient", null);
        rules.put("active_ingredient_of", null);
        rules.put("occurs_before", null);
        rules.put("occurs_after", null);
        rules.put("occurs_in", null);    
        rules.put("uses_device", null);
        rules.put("device_used_by", null);
        rules.put("has_location", null);
        rels = new RelationshipExtractor(conn).extract(rules, cui, code);
        /*
        switch(sty.toUpperCase()){
            case "T047":
                rules.put("same_as", null);
                rules.put("due_to", asList("T046", "T047"));
                rules.put("cause_of", asList("T046", "T047"));
                rules.put("inverse_isa", asList("T046", "T047"));
                rules.put("isa", asList("T046", "T047"));
                rules.put("finding_site_of", asList("T022", "T023"));
                rels = RelationshipExtractor.extract(rules, cui, code);
                return rels;
            case "T121":
                rules.put("same_as", null);
                rules.put("inverse_isa", asList("T121"));
                rules.put("isa", asList("T121"));
                rules.put("has_causative_agent", asList("T047"));
                rels = RelationshipExtractor.extract(rules, cui, code);
                return rels;
            default:
                return null;
        }
        */
        return rels;
    }
    
    public String conceptExists(String concept){
        return null;
    }
    
    public boolean hasRating(String tuid, String cui){
        
        //Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        String database = "umls_" + code;
        try {
            conn.setCatalog(database);
            
            String query = "SELECT * FROM rating WHERE tuid = ? AND cui = ?;";
            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, tuid);
            stmt.setString(2, cui);

            ResultSet rs = stmt.executeQuery();
            if(! rs.next()){
                return false;
            }
            
            stmt.close();
            rs.close();   
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
}
