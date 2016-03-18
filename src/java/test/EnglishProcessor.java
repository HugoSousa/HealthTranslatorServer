/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import opennlp.tools.util.Span;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jactiveresource.Inflector;

/**
 *
 * @author Hugo
 */
public class EnglishProcessor extends ConceptProcessor {

    public EnglishProcessor() {
        super();
        code = "en";
    }

    /**
     *
     * @param spans
     * @param i
     * @param text
     * @param forward_threshold
     * @return
     */
    @Override
    protected Concept processToken(Span[] spans, int i, String text, int forward_threshold) {

        String[] tokens = new String[forward_threshold];
        Span initialSpan = spans[i];
        Concept bestMatch = null;

        int initialIndex = i;

        for (int j = 0; j < forward_threshold; j++) {

            if (initialIndex + j >= spans.length) {
                break;
            }

            Span span = spans[initialIndex + j];
            String token = text.substring(span.getStart(), span.getEnd());
            tokens[j] = token;
            
            String finalToken = "";
            if (j == 0) {
                finalToken = token;
            } else if (j > 0) {
                for (int k = 0; k <= j; k++) {
                    //dont add a space if next token is "'s" <- incorrect tokenization
                    if(tokens[k].equals("'s") && k > 0)
                        finalToken = finalToken.substring(0,finalToken.length()-1);
                    
                    finalToken += tokens[k];
                    if (k < j) {

                        finalToken += " ";
                    }
                }
            }
            
            String queryToken = finalToken.toLowerCase();
            String originalString = finalToken;
            String singularQueryToken = null;
            try {
                singularQueryToken = Inflector.singularize(queryToken, "en");
            } catch (Exception ex) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            punctuationMatcher.reset(token);
            numberMatcher.reset(token);
            if (j == 0 && (queryToken.length() <= 2 || stopwords.containsKey(queryToken))) {
                break;
            } else if (punctuationMatcher.matches() || numberMatcher.matches()) {
                break;
            }

            Connection connMySQL = ServletContextClass.conn_MySQL;
            PreparedStatement stmt;

            try {
                connMySQL.setCatalog("umls_en");

                //long startTime = System.nanoTime();
                stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO mrc WHERE STR = ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

                stmt.setString(1, singularQueryToken);
                //System.out.println("TOKEN: " + singularQueryToken);
                ResultSet rs = stmt.executeQuery();
                //long endTime = System.nanoTime();
                //long duration = (endTime - startTime) / 1000000;
                //System.out.println("DURATION: " + duration + " ms for token " + queryToken);

                rs.last();
                int total = rs.getRow();
                rs.beforeFirst();
                //rs.next();

                if (total >= 1) {
                    //System.out.println("FOUND RESULTS FOR: " + singularQueryToken);
                    //iterate result set, check if it's CHV preferred or synonym
                    //if they map to different CUIs, check the one with TTY = PT

                    String CUI = null;
                    String CHVPreferred = null;
                    while (rs.next()) {
                        if (rs.getRow() == 1) {
                            //assign the first result at least, so it's not null
                            CUI = rs.getString("CUI");
                        } else {
                            if ( (! rs.getString("CUI").equals(CUI)) && ( rs.getString("TTY").equals("PT") || rs.getString("TTY").equals("SY"))) {
                                CUI = rs.getString("CUI");
                            }
                        }

                        if (rs.getString("TTY").equals("PT") && rs.getString("SAB").equals("CHV")) {
                            CHVPreferred = singularQueryToken;
                        }
                    }

                    stmt = connMySQL.prepareStatement("SELECT * FROM MRSTY WHERE CUI = ?;");
                    stmt.setString(1, CUI);
                    rs = stmt.executeQuery();
                    
                    ArrayList<String> tuis = new ArrayList<>();
                    while(rs.next()){
                        tuis.add(rs.getString("tui"));
                    }
                    
                    if (acceptedSemanticType(tuis)) {
                        
                        bestMatch = new Concept(originalString, new Span(initialSpan.getStart(), span.getEnd()), j+1);
                        bestMatch.CUI = CUI;

                        if (CHVPreferred == null) {

                            stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO WHERE CUI = ? AND SAB = 'CHV' AND TTY = 'PT';");
                            stmt.setString(1, CUI);
                            rs = stmt.executeQuery();
                            if (rs.next()) {
                                CHVPreferred = rs.getString("STR");
                            }/* else {
                                //the concept may not be in CHV
                                System.out.println("The concept " + CUI + " (" + singularQueryToken + ") is not in CHV.");
                            }
                            */
                        }

                        bestMatch.setCHVPreferred(CHVPreferred);

                    }
                }

                stmt.close();
                rs.close();
            } catch (SQLException ex) {
                Logger.getLogger(EnglishProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
        
        return bestMatch;
    }
    
    @Override
    protected String getDefinition(Concept concept) {
        
        long startTime = System.nanoTime();
        
        String definition = null;
        
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        //TODO search in MedlinePlus 1st
        
        try {
            connMySQL.setCatalog("umls_en");
            
            String query = "SELECT DEF FROM wikidef WHERE CUI = ?;";
            stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, concept.CUI);

            ResultSet rs = stmt.executeQuery();
            
            if(rs.next()){
                definition = rs.getString("DEF");
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(ConceptProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }catch (Exception ex) {
            System.out.println(ex);
        }
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        //System.out.println("PROCESSING FOR TOKEN " + concept.string + " (" + concept.CUI + ")" +": " + duration + " ms");
               
        return definition;
    }
    
    @Override
    protected ArrayList<ExternalReference> getExternalReferences(String concept) {
        
        String SNOMEDCode = null;
        
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        try{
            connMySQL.setCatalog("umls_en");
            stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO WHERE STR = ? AND SAB = 'SNOMEDCT_US';", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1, concept);
            ResultSet rs = stmt.executeQuery();
            
            
            if(! rs.next()){
                //only CHV results
                stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO WHERE STR = ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                stmt.setString(1, concept);
                rs = stmt.executeQuery();
                rs.next();
                String CUI = rs.getString("CUI");
                
                //get the CUI from CHV and search for SNOMED concepts with that CUI
                stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO WHERE CUI = ? AND SAB = 'SNOMEDCT_US';", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                stmt.setString(1, CUI);
                rs = stmt.executeQuery();
                
                if(rs.next()){
                    SNOMEDCode = rs.getString("SCUI");
                }
            }else{
                SNOMEDCode = rs.getString("SCUI");
            }
        }catch (SQLException ex) {
            Logger.getLogger(EnglishProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        ArrayList<ExternalReference> medlinePlusReferences = getMedlinePlusReferences(SNOMEDCode);
        //medlineplus
        //healthfinder.gov
        
        return medlinePlusReferences;
    }
    
    private ArrayList<ExternalReference> getMedlinePlusReferences(String SNOMEDCode){
        
        ArrayList<ExternalReference> result = new ArrayList<>();
        
        HttpGet httpGet = new HttpGet("https://apps.nlm.nih.gov/medlineplus/services/mpconnect_service.cfm?informationRecipient.languageCode.c=en&knowledgeResponseType=application/json&mainSearchCriteria.v.cs=2.16.840.1.113883.6.96&mainSearchCriteria.v.c=" + SNOMEDCode);
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
            
            JsonObject feed = obj.getJsonObject("feed");
            JsonArray entry = feed.getJsonArray("entry");
            
            if(entry.size() >= 1){
                JsonArray links = ((JsonObject)entry.get(0)).getJsonArray("link");
                
                for(JsonValue link: links){
                    String url = ((JsonObject)link).getString("href");
                    String label = ((JsonObject)link).getString("title");
                    ExternalReference ref = new ExternalReference(url, label);
                    result.add(ref);
                }
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
        
        return result;
    }   
    
    private boolean acceptedSemanticType(ArrayList<String> tuis) {
        
        for(String tui: tuis){
            if(! acceptedSemanticType(tui))
                return false;
        }
        
        return true;
    }
}
