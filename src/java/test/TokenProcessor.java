/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

/**
 *
 * @author Hugo
 */
public abstract class TokenProcessor {
    
    protected ConcurrentHashMap<String, String> stopwords;
    protected Tokenizer tokenizer; 
    protected HashSet<String> semanticTypes;
    protected Matcher punctuationMatcher;
    protected Matcher numberMatcher;
    protected String code;
    
    /**
     *
     */
    public TokenProcessor(){
        Pattern punctuationPattern = Pattern.compile("\\p{Punct}", Pattern.CASE_INSENSITIVE);
        Pattern numberPattern = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
        punctuationMatcher = punctuationPattern.matcher("");
        numberMatcher = numberPattern.matcher("");
    }

    
    protected void setSemanticTypes(HashSet<String> semanticTypes){
        this.semanticTypes = semanticTypes;
    }
    
    protected void setTokenizer(Tokenizer tokenizer){
        this.tokenizer = tokenizer;
    };
    
    protected void setStopwords(ConcurrentHashMap<String, String> stopwords){
        this.stopwords = stopwords;
    }
    
    protected boolean acceptedSemanticType(String sty) {
        return semanticTypes.contains(sty);
    }
    
    protected Concept processToken(Span[] spans, int i, String text, int forward_threshold){
        return null;
    };
    
    protected String getDefinition(Concept concept){

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
}
