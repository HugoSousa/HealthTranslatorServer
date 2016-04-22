/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.details;

import ht.concept.Concept;
import ht.utils.Inflector;
import ht.utils.LoggerFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
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
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Hugo
 */
public class ExternalReferencesExtractor {
    
    private final CloseableHttpClient httpclient = HttpClients.createDefault();
    private final Logger logger;
    private final Connection conn;
    
    public ExternalReferencesExtractor(Connection conn){
        this.conn = conn;
        logger = LoggerFactory.createLogger(ExternalReferencesExtractor.class.getName());
    }
    
    public ArrayList<ExternalReference> getEnglishExternalReferences(Concept concept) {
        
        ArrayList<ExternalReference> resultList = new ArrayList<>();
        
        String SNOMEDCode = getSNOMEDCode(concept);
        ArrayList<ExternalReference> medlinePlusReferences = new ArrayList<>();
        if(SNOMEDCode != null)
            medlinePlusReferences= getMedlinePlusReferences(SNOMEDCode);
        
        ArrayList<ExternalReference> healthFinderReferences = getHealthFinderReferences(concept.string);
        ArrayList<ExternalReference> mayoClinicReferences = getMayoClinicReferences(concept.string);
        ArrayList<ExternalReference> NIHReferences = getNIHReferences(concept.string);
        ExternalReference wikipediaReference = getWikipediaReference(concept, "en");
        
        String preferredTerm = getPreferredTermEnglish(concept.CUI);
        if(preferredTerm != null && ! preferredTerm.equalsIgnoreCase(concept.string)){
            ArrayList<ExternalReference> healthFinderReferencesPreferred = getHealthFinderReferences(preferredTerm);
            ArrayList<ExternalReference> mayoClinicReferencesPreferred = getMayoClinicReferences(preferredTerm);
            ArrayList<ExternalReference> NIHReferencesPreferred = getNIHReferences(preferredTerm);
            
            resultList.addAll(healthFinderReferencesPreferred);
            resultList.addAll(mayoClinicReferencesPreferred);
            resultList.addAll(NIHReferencesPreferred);
        }
        
        try{
            if(concept.CHVPreferred != null && ! (Inflector.singularize(concept.CHVPreferred, "en")).equalsIgnoreCase(Inflector.singularize(concept.string, "en")) && ! (Inflector.singularize(concept.CHVPreferred, "en")).equalsIgnoreCase(Inflector.singularize(preferredTerm, "en"))){
                ArrayList<ExternalReference> healthFinderReferencesLay = getHealthFinderReferences(concept.CHVPreferred);
                ArrayList<ExternalReference> mayoClinicReferencesLay = getMayoClinicReferences(concept.CHVPreferred);
                ArrayList<ExternalReference> NIHReferencesLay = getNIHReferences(concept.CHVPreferred);

                resultList.addAll(healthFinderReferencesLay);
                resultList.addAll(mayoClinicReferencesLay);
                resultList.addAll(NIHReferencesLay);
            }
        }catch(Exception ex){
            logger.log(Level.SEVERE, null, ex);
        }
        
        resultList.addAll(medlinePlusReferences);
        resultList.addAll(healthFinderReferences);
        resultList.addAll(mayoClinicReferences);
        resultList.addAll(NIHReferences);
        if(wikipediaReference != null) resultList.add(wikipediaReference);
        
        return resultList;
    }
    
    private String getPreferredTermEnglish(String cui){
        //Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        try {
            conn.setCatalog("umls_en");
        
            String query = "select * from MRCONSO WHERE cui = ? AND ts = 'P' AND stt = 'PF' AND ispref = 'Y' AND lat = 'ENG';";
            stmt = conn.prepareStatement(query);

            stmt.setString(1, cui);

            ResultSet rs = stmt.executeQuery();

            if(rs.next()){
                return rs.getString("STR");
            }
        
            stmt.close();
            rs.close();
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    public ArrayList<ExternalReference> getPortugueseExternalReferences(Concept concept) {
        
        ArrayList<ExternalReference> resultList = new ArrayList<>();
        
        ArrayList<ExternalReference> infopediaReferences = getInfopediaReferences(concept.string);
        ArrayList<ExternalReference> medicoRespondeReferences = getMedicoRespondeReferences(concept.string);
        ExternalReference wikipediaReference = getWikipediaReference(concept, "pt");
        
        String preferredTerm = getPreferredTermPortuguese(concept.CUI);
        if(preferredTerm != null && ! preferredTerm.equalsIgnoreCase(concept.string)){
            ArrayList<ExternalReference> infopediaReferencesPreferred = getInfopediaReferences(preferredTerm);
            ArrayList<ExternalReference> medicoRespondeReferencesPreferred = getMedicoRespondeReferences(preferredTerm);
            
            resultList.addAll(infopediaReferencesPreferred);
            resultList.addAll(medicoRespondeReferencesPreferred);
        }
        
        try{
            if(concept.CHVPreferred != null && ! (Inflector.singularize(concept.CHVPreferred, "pt")).equalsIgnoreCase(Inflector.singularize(concept.string, "pt")) && ! (Inflector.singularize(concept.CHVPreferred, "pt")).equalsIgnoreCase(Inflector.singularize(preferredTerm, "pt"))){
                ArrayList<ExternalReference> infopediaReferencesLay = getInfopediaReferences(concept.CHVPreferred);
                ArrayList<ExternalReference> medicoRespondeReferencesLay = getMedicoRespondeReferences(concept.CHVPreferred);

                resultList.addAll(infopediaReferencesLay);
                resultList.addAll(medicoRespondeReferencesLay);
            }
        }catch(Exception ex){
            logger.log(Level.SEVERE, null, ex);
        }

        resultList.addAll(infopediaReferences);
        resultList.addAll(medicoRespondeReferences);
        if(wikipediaReference != null) resultList.add(wikipediaReference);
        
        return resultList;
    }
    
    private String getPreferredTermPortuguese(String cui){
        //Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        try {
            conn.setCatalog("umls_en");
        
            String query = "select * from MRCONSO WHERE cui = ? AND ts = 'P' AND stt = 'PF' AND ispref = 'Y' AND lat = 'POR';";
            stmt = conn.prepareStatement(query);

            stmt.setString(1, cui);

            ResultSet rs = stmt.executeQuery();

            if(rs.next()){
                return rs.getString("STR");
            }
            
            stmt.close();
            rs.close();  
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    
    private String getSNOMEDCode(Concept concept){
        
        String SNOMEDCode = null;
        
        //Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        try{
            conn.setCatalog("umls_en");
            stmt = conn.prepareStatement("SELECT * FROM MRCONSO WHERE CUI = ? AND SAB = 'SNOMEDCT_US';", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1, concept.CUI);
            ResultSet rs = stmt.executeQuery();
            
            while(rs.next()){
                if(SNOMEDCode == null || (SNOMEDCode != null && rs.getString("TTY").equals("PT")))
                    SNOMEDCode = rs.getString("SCUI");
            }
            
            stmt.close();
            rs.close();
            
        }catch (SQLException ex) {
            //logger.log(Level.SEVERE, null, ex);
        }
        
        return SNOMEDCode;
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
                    ExternalReference ref = new ExternalReference(url, label, "MedlinePlus");
                    result.add(ref);
                }
            }
            EntityUtils.consume(entity1);
        } catch (IOException ex) {
            //logger.log(Level.SEVERE, null, ex);
        } finally {
            try {
                response1.close();
            } catch (IOException ex) {
                //logger.log(Level.SEVERE, null, ex);
            }
        }
        
        return result;
    }   
    
    private ArrayList<ExternalReference> getHealthFinderReferences(String concept){
        
        ArrayList<ExternalReference> result = new ArrayList<>();
        
        concept = concept.replaceAll(" ", "%20");
        HttpGet httpGet = new HttpGet("http://healthfinder.gov/developer/Search.json?api_key=nqmqcoaowrbqxksg&keyword=%22" + concept +"%22");
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
           
            JsonObject res = obj.getJsonObject("Result");
            String total = res.getString("Total");
                
            if(Integer.parseInt(total) > 0 && !res.isNull("Topics")){
                
                //if only 1 topic, return a JsonObject. Otherwise, returns JsonArray
                try{
                    JsonArray topics = res.getJsonArray("Topics");
                    
                    for(JsonValue topic: topics){
                        String url = ((JsonObject)topic).getString("AccessibleVersion");
                        String label = ((JsonObject)topic).getString("Title");
                        ExternalReference ref = new ExternalReference(url, label, "Healthfinder");
                        result.add(ref);
                    }
                }catch(java.lang.ClassCastException ex){
                    try{
                        JsonObject topic = res.getJsonObject("Topics");

                        String url = topic.getString("AccessibleVersion");
                        String label = topic.getString("Title");
                        ExternalReference ref = new ExternalReference(url, label, "Healthfinder");
                        result.add(ref);
                    }catch(java.lang.ClassCastException ex2){
                        //no topic
                    }
                }

                
            }
            EntityUtils.consume(entity1);
        } catch (IOException ex) {
            //logger.log(Level.SEVERE, null, ex);
        } finally {
            try {
                response1.close();
            } catch (IOException ex) {
                //logger.log(Level.SEVERE, null, ex);
            }
        }
        
        return result;
    }
    
    private ArrayList<ExternalReference> getMayoClinicReferences(String concept){
        
        ArrayList<ExternalReference> result = new ArrayList<> ();
        
        try {
            concept = URLEncoder.encode(concept, "utf-8");
            Document doc = Jsoup.connect("http://www.mayoclinic.org/search/search-results?site=patient-care&q=" + concept ).get();
                    
            Element elem = doc.select("div#mayo-wrapper div#main-content div.directory li h3 a").first();
            
            if(elem != null){
                String url = elem.attr("href");
                String label = elem.text();
                
                //remove "- Mayo Clinic" from the label
                int i = label.indexOf("- Mayo Clinic");
                if(i != -1){
                    label = label.substring(0, i);
                }else{
                    i = label.indexOf("- Mayo ...");
                    if(i != -1){
                        label = label.substring(0, i);
                }
                }
                
                ExternalReference ref = new ExternalReference(url, label, "Mayo Clinic");
                result.add(ref);
            }
                    
        } catch (IOException ex) {
            //logger.log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    private ArrayList<ExternalReference> getNIHReferences(String concept){
                
        ArrayList<ExternalReference> result = new ArrayList<> ();
        
        try {
            concept = URLEncoder.encode(concept, "utf-8");
            Document doc = Jsoup.connect("http://search.nih.gov/search?utf8=%E2%9C%93&affiliate=hip&query=" + concept ).get();
                    
            Elements elems = doc.select("#best-bets div.boosted-content");
            
            if(elems != null){
                for(Element elem: elems){
                    Element a = elem.select("h4.title a").first();
                    String url = a.attr("href");
                    String label = a.text();
                    
                    ExternalReference ref = new ExternalReference(url, label, "NIH Health Information");
                    result.add(ref);
                }
            }
                    
        } catch (IOException ex) {
            //logger.log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    
    private ArrayList<ExternalReference> getInfopediaReferences(String concept){
        
        ArrayList<ExternalReference> result = new ArrayList<> ();
        
        try {
            concept = URLEncoder.encode(concept, "utf-8");
            String url = "http://www.infopedia.pt/dicionarios/termos-medicos/" + concept;
            Document doc = Jsoup.connect(url).timeout(4000).get();
                    
            Element elem = doc.select("#infoCliqueContainer").first();
            
            if(elem != null){
                String label = elem.select("span.dolEntrinfoEntrada").first().text();
                
                ExternalReference ref = new ExternalReference(url, label, "Infopedia");
                result.add(ref);
            }
                    
        } catch (IOException ex) {
            //logger.log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    private ArrayList<ExternalReference> getMedicoRespondeReferences(String concept){
        
        String originalConcept = concept;
        ArrayList<ExternalReference> result = new ArrayList<> ();
        
        try {
            concept = URLEncoder.encode(concept, "utf-8");
            String url = "http://medicoresponde.com.br/busca/?s=" + concept;
            Document doc = Jsoup.connect(url).get();
            
            //if doesn't have div.box, there's no results
            Elements elems = doc.select("#container div.box");
            
            if(elems != null){
                //String label = elem.select("span.dolEntrinfoEntrada").first().text();
                
                ExternalReference ref = new ExternalReference(url, originalConcept, "Medico Responde");
                result.add(ref);
            }
                    
        } catch (IOException ex) {
            //logger.log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    private ExternalReference getWikipediaReference(Concept concept, String language){
        PreparedStatement stmt;
        
        try {
            conn.setCatalog("umls_" + language);
        
            String query = "select url from wikidef where cui = ?;";
            stmt = conn.prepareStatement(query);

            stmt.setString(1, concept.CUI);

            ResultSet rs = stmt.executeQuery();

            if(rs.next()){
                String url = rs.getString("url");
                String title = url.substring(url.lastIndexOf("/") + 1) ;
                title = title.replace("_", " ");
                title = java.net.URLDecoder.decode(title, "UTF-8");
                
                return new ExternalReference(url, title, "Wikipedia " + language.toUpperCase());
            }
        
            stmt.close();
            rs.close();
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return null;
    }

}
