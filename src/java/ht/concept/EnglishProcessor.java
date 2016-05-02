/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.concept;

import ht.utils.LoggerFactory;
import ht.details.ExternalReference;
import ht.details.ExternalReferencesExtractor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.util.Span;
import ht.utils.Inflector;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import opennlp.tools.tokenize.TokenizerModel;

/**
 *
 * @author Hugo
 */
public class EnglishProcessor extends ConceptProcessor {

    private static Logger logger; 
    
    public EnglishProcessor(Connection conn) {
        super(conn);
        code = "en";
        
        logger = LoggerFactory.createLogger(EnglishProcessor.class.getName());
    }
    
    public EnglishProcessor(Connection conn, ConcurrentHashMap<String, String> stopwords, TokenizerModel tokenizerModel, HashSet<String> acceptedSemanticTypes){
        super(conn, stopwords, tokenizerModel, acceptedSemanticTypes);
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
            
            //Connection connMySQL = ServletContextClass.conn_MySQL;
            PreparedStatement stmt;

            try {
                conn.setCatalog("umls_en");

                //long startTime = System.nanoTime();
                stmt = conn.prepareStatement("SELECT * FROM MRCONSO mrc WHERE STR = ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

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
                    
                    if(allResultsFromCHV(rs) && ! recognizeOnlyCHV){
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

                    stmt = conn.prepareStatement("SELECT * FROM MRSTY WHERE CUI = ?;");
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

                            stmt = conn.prepareStatement("SELECT * FROM MRCONSO WHERE CUI = ? AND SAB = 'CHV' AND TTY = 'PT';");
                            stmt.setString(1, CUI);
                            rs = stmt.executeQuery();
                            if (rs.next()) {
                                CHVPreferred = rs.getString("STR");
                                bestMatch.setCHVPreferred(CHVPreferred);
                            }/* else {
                                //the concept may not be in CHV
                                System.out.println("The concept " + CUI + " (" + singularQueryToken + ") is not in CHV.");
                            }
                            */
                        }
                        
                        String UMLSPreferred = null;
                        stmt = conn.prepareStatement("select * from MRCONSO c WHERE c.ts = 'P' AND c.stt = 'PF' AND c.ispref = 'Y' AND c.lat = 'ENG' AND c.cui = ?;");
                        stmt.setString(1, CUI);
                        rs = stmt.executeQuery();
                        if (rs.next()) {
                            UMLSPreferred = rs.getString("STR");
                        }
                        
                        bestMatch.setUMLSPreferred(UMLSPreferred);

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
        
        //Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        try {
            conn.setCatalog("umls_en");
        
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
            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, concept.CUI);

            ResultSet rs = stmt.executeQuery();

            if(rs.next()){
                definition = rs.getString("DEF");
            }
            //}
            stmt.close();
            rs.close();
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
        return new ExternalReferencesExtractor(conn).getEnglishExternalReferences(concept);
    }
    
    @Override
    public ArrayList<SemanticType> getSemanticTypes(String cui){
        
        ArrayList<SemanticType> stys = new ArrayList<>();
        
        //Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        String database = "umls_" + code;
        try {
            conn.setCatalog(database);
            
            String query = "SELECT * FROM mrsty WHERE CUI = ?;";
            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, cui);

            ResultSet rs = stmt.executeQuery();
            
            while(rs.next()){
                String sty = rs.getString("STY");
                String tui = rs.getString("TUI");
                
                if(! stys.contains(new SemanticType(sty, null)))
                    stys.add(new SemanticType(tui, sty));
            }
            
            stmt.close();
            rs.close();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return stys;
    }
    
    //returns the cui of the concept, if it exists, otherwise returns null
    @Override
    public String conceptExists(String concept){
        
        String cui = null;
        //Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;

        try {
            conn.setCatalog("umls_en");

            String query = "select * from MRCONSO WHERE STR = ?;";

            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, concept);
            
            ResultSet rs = stmt.executeQuery();
            
            rs.last();
            int total = rs.getRow();
            rs.beforeFirst();
            
            if (total > 0){
                while(rs.next()){
                    if (rs.getRow() == 1) {
                        //assign the first result at least, so it's not null
                        cui = rs.getString("CUI");
                    } else {
                        if ( (! rs.getString("CUI").equals(cui)) && ( rs.getString("TTY").equals("PT") || rs.getString("TTY").equals("SY"))) {
                            cui = rs.getString("CUI");
                        }
                    }
                }
            }
            
            stmt.close();
            rs.close();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return cui;
    }
}
