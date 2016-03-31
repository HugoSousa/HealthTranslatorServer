/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.concept;

import ht.utils.LoggerFactory;
import ht.details.ExternalReference;
import ht.utils.ServletContextClass;
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
import opennlp.tools.util.Span;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import ht.utils.Inflector;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Hugo
 */
public class EnglishProcessor extends ConceptProcessor {

    private static Logger logger; 
    
    public EnglishProcessor() {
        super();
        code = "en";
        
        logger = LoggerFactory.createLogger(EnglishProcessor.class.getName());
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
    public Concept processToken(Span[] spans, int i, String text, int forward_threshold) {

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
                logger.log(Level.SEVERE, null, ex);
            }
            
            punctuationMatcher.reset(token);
            numberMatcher.reset(token);
            if (j == 0 && (queryToken.length() <= 3 || stopwords.containsKey(queryToken))) {
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
                    
                    if(allResultsFromCHV(rs) && filterCHVOnly){
                        return null;
                    }
                    
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
                    
                    if (isAcceptedSemanticType(tuis)) {
                        
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
                logger.log(Level.SEVERE, null, ex);
            }
        }        
        
        return bestMatch;
    }
    
    @Override
    public String getDefinition(Concept concept) {
        
        long startTime = System.nanoTime();
        
        String definition = null;
        
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        try {
            connMySQL.setCatalog("umls_en");
        
        //TODO search in MedlinePlus 1st
            /*
            stmt = connMySQL.prepareStatement("SELECT DEF FROM MRDEF WHERE CUI = ? AND SAB = 'MEDLINEPLUS'", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1, concept.CUI);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                definition = rs.getString("DEF");
            }else{
            */
                String query = "SELECT DEF FROM wikidef WHERE CUI = ?;";
                stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

                stmt.setString(1, concept.CUI);

                ResultSet rs = stmt.executeQuery();

                if(rs.next()){
                    definition = rs.getString("DEF");
                }
            //}
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        //System.out.println("PROCESSING FOR TOKEN " + concept.string + " (" + concept.CUI + ")" +": " + duration + " ms");
        //getExternalReferences(concept);
        
        return definition;
    }
    
    @Override
    public ArrayList<ExternalReference> getExternalReferences(Concept concept) {
        
        String SNOMEDCode = getSNOMEDCode(concept);
        ArrayList<ExternalReference> medlinePlusReferences = getMedlinePlusReferences(SNOMEDCode);
        ArrayList<ExternalReference> healthFinderReferences = getHealthFinderReferences(concept.string);
        //ExternalReference BMJReference = getBMJReference(concept.string);
        //ArrayList<ExternalReference> healthlineReferences = getHealthlineReferences(concept.string);
        ArrayList<ExternalReference> mayoClinicReferences = getMayoClinicReferences(concept.string);
        ArrayList<ExternalReference> NIHReferences = getNIHReferences(concept.string);
        
        ArrayList<ExternalReference> resultList = new ArrayList<>();
        resultList.addAll(medlinePlusReferences);
        resultList.addAll(healthFinderReferences);
        resultList.addAll(mayoClinicReferences);
        resultList.addAll(NIHReferences);
        
        return resultList;
    }
    
    private String getSNOMEDCode(Concept concept){
        
        String SNOMEDCode = null;
        
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        try{
            connMySQL.setCatalog("umls_en");
            stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO WHERE CUI = ? AND SAB = 'SNOMEDCT_US';", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1, concept.CUI);
            ResultSet rs = stmt.executeQuery();
            
            while(rs.next()){
                if(SNOMEDCode == null || (SNOMEDCode != null && rs.getString("TTY").equals("PT")))
                    SNOMEDCode = rs.getString("SCUI");
            }
        }catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
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
            logger.log(Level.SEVERE, null, ex);
        } finally {
            try {
                response1.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
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
                
            if(Integer.parseInt(total) > 0){
                
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
                    JsonObject topic = res.getJsonObject("Topics");
                    
                    String url = topic.getString("AccessibleVersion");
                    String label = topic.getString("Title");
                    ExternalReference ref = new ExternalReference(url, label, "Healthfinder");
                    result.add(ref);
                }

                
            }
            EntityUtils.consume(entity1);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            try {
                response1.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        
        return result;
    }
    
    private ExternalReference getBMJReference(String concept){
        
        try {
            concept = concept.replaceAll(" ", "%20");
            Document doc = Jsoup.connect("http://bestpractice.bmj.com/best-practice/search.html?searchableText=%22" + concept + "%22").followRedirects(false).get();
            Response response = Jsoup.connect("http://bestpractice.bmj.com/best-practice/search.html?searchableText=%22" + concept + "%22").followRedirects(false).execute();
            System.out.println(response.url());
            
            System.out.println("DOC: " + doc.body());
            Element elem = doc.select("#content ul.nav-tabs li.selected a").first();
            
            if(elem != null){
                String resultString = elem.text();
            }
            
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    private ArrayList<ExternalReference> getHealthlineReferences(String concept){
        //THE RESULTS ARE LOADED DYNAMICALLY, SO JSOUP DOESN'T WORK
        
        ArrayList<ExternalReference> result = new ArrayList<> ();
        
        try {
            concept = URLEncoder.encode(concept, "utf-8");//concept.replaceAll(" ", "%20");
            //Document doc = Jsoup.connect("http://www.healthline.com/search?q1=" + concept ).get();
            Response response = Jsoup.connect("http://www.healthline.com/search?q1=" + concept ).execute();
            System.out.println(response.url());
            
            /*
            System.out.println("DOC: " + doc.body());
            Element elem = doc.select("#___gcse_1 div.gsc-wrapper div.gsc-resultsbox-visible table.gsc-table-result td.gsc-table-cell-snippet-close a.gs-title").first();
            
            if(elem != null){
                String url = elem.attr("data-ctorig");
                String label = elem.text();
                ExternalReference ref = new ExternalReference(url, label, "healthline");
                result.add(ref);
                //String resultString = elem.text();
            }
                    */
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
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
                }
                
                ExternalReference ref = new ExternalReference(url, label, "Mayo Clinic");
                result.add(ref);
            }
                    
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
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
            logger.log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    @Override
    public ArrayList<String> getSemanticTypes(String cui){
        
        ArrayList<String> stys = new ArrayList<>();
        
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        String database = "umls_" + code;
        try {
            connMySQL.setCatalog(database);
            
            String query = "SELECT * FROM mrsty WHERE CUI = ?;";
            stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, cui);

            ResultSet rs = stmt.executeQuery();
            
            while(rs.next()){
                String sty = rs.getString("STY");
                if(! stys.contains(sty))
                    stys.add(sty);
            }
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return stys;
    }
}
