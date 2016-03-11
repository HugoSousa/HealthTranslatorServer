/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

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
    
    protected String getDefinition(String concept){

        String definition = null;
        
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
        
        return definition;
        
    };
}
