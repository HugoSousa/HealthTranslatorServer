/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.concept;

import ht.utils.LoggerFactory;
import ht.details.ExternalReference;
import ht.details.ExternalReferencesExtractor;
import ht.utils.ServletContextClass;
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
import opennlp.tools.tokenize.Tokenizer;

/**
 *
 * @author Hugo
 */
public class PortugueseProcessor extends ConceptProcessor {
    
    private static Logger logger;
    
    public PortugueseProcessor() {
        super();
        code = "pt";
        
        logger = LoggerFactory.createLogger(PortugueseProcessor.class.getName());
    }
    
    public PortugueseProcessor(ConcurrentHashMap<String, String> stopwords, Tokenizer tokenizer, HashSet<String> acceptedSemanticTypes){
        super(stopwords, tokenizer, acceptedSemanticTypes);
        code = "pt";
        
        logger = LoggerFactory.createLogger(PortugueseProcessor.class.getName());
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
                singularQueryToken = Inflector.singularize(queryToken, "pt");
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
                connMySQL.setCatalog("umls_pt");

                String query = "select c.CUI, STR, SAB, TTY, null as chv_pref_pt, null AS umls_pref_pt, tui "
                        + "from MRCONSO c, MRSTY s "
                        + "WHERE STR = ? "
                        + "AND c.cui = s.cui "
                        + "UNION ALL "
                        + "select s.cui, pt, 'CHV' AS sab, null as tty, chv_pref_pt, umls_pref_pt, tui "
                        + "from chvstring s join chvconcept c on s.cui = c.cui "
                        + "WHERE pt = ?;";
                
                stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

                stmt.setString(1, singularQueryToken);
                stmt.setString(2, singularQueryToken);

                ResultSet rs = stmt.executeQuery();

                rs.last();
                int total = rs.getRow();
                rs.beforeFirst();
                //rs.next();

                //System.out.print("TOKEN " + singularQueryToken + ": " + total + " results");

                if (total >= 1) {
                    
                    String CUI = null;
                    String CHVPreferred = null;
                    String UMLSPreferred = null;
                    ArrayList<String> tuis = new ArrayList<>();
                    int TUIPreferred = -1;
                    
                    if(allResultsFromCHV(rs)){
                        
                        if(! recognizeOnlyCHV)
                            return null;
                        //if there's only many CHV results, it's probably a bad translation (example: "elevado") 
                        if(total < 4){
                            
                            //get the CUIS on a list
                            //if the preferred term is in the results, that's the best CUI
                            while (rs.next()) {
                                if (rs.getRow() == 1) {
                                    //assign the first result at least, so it's not null
                                    CUI = rs.getString("CUI");
                                    tuis.add(rs.getString("tui"));
                                    CHVPreferred = rs.getString("chv_pref_pt");
                                    UMLSPreferred = rs.getString("umls_pref_pt");
                                }  
                            }                         
                        }
                    }else{                  
                        while (rs.next()) {
                            if (rs.getRow() == 1) {
                                //assign the first result at least, so it's not null
                                CUI = rs.getString("CUI");
                            } else {
                                if( ! rs.getString("CUI").equals(CUI) && ! rs.getString("SAB").equals("CHV")){
                                    CUI = rs.getString("CUI");
                                }
                            }

                            if (rs.getString("SAB").equals("CHV") && rs.getString("CUI").equals(CUI)) {
                                CHVPreferred = rs.getString("chv_pref_pt");
                            }
                            
                            tuis.add(rs.getString("tui"));
                        }
                        
                        stmt = connMySQL.prepareStatement("select * from MRCONSO c WHERE c.ts = 'P' AND c.stt = 'PF' AND c.ispref = 'Y' AND c.lat = 'POR' AND c.cui = ?;");
                        stmt.setString(1, CUI);
                        rs = stmt.executeQuery();
                        if (rs.next()) {
                            UMLSPreferred = rs.getString("STR");
                        }
                    }
                    
                    ArrayList<String> tuisCopy = new ArrayList<>(tuis);
                    
                    for(String tui: tuisCopy){
                        String[] tuiList = tui.split(";");
                        for(String tuiSplit: tuiList){
                            if(! tuis.contains(tuiSplit))
                                tuis.add(tuiSplit);
                        }
                    }
                    
                    if( tuis.size() > 0 &&
                        (TUIPreferred == -1 && isAcceptedSemanticType(tuis))){
                        bestMatch = new Concept(originalString, new Span(initialSpan.getStart(), span.getEnd()), j+1);
                        bestMatch.CUI = CUI;
                        bestMatch.setCHVPreferred(CHVPreferred);
                        bestMatch.setUMLSPreferred(UMLSPreferred);
                        
                        /*
                        if (CHVPreferred == null) {
                            //System.out.println("The concept " + CUI + " (" + singularQueryToken + ") is not in CHV.");
                        }
                        */
                    }
                }
                
                stmt.close();
                rs.close();

            } catch (SQLException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        return bestMatch;
    };
    
    @Override
    public String getDefinition(Concept concept) {
        
        long startTime = System.nanoTime();
        
        String definition = null;
        
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
                
        try {
            connMySQL.setCatalog("umls_pt");
            
            String query = "SELECT DEF FROM wikidef WHERE CUI = ?;";
            stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, concept.CUI);

            ResultSet rs = stmt.executeQuery();
            
            if(rs.next()){
                definition = rs.getString("DEF");
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(ConceptProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        System.out.println("PROCESSING FOR TOKEN " + concept.string + " (" + concept.CUI + ")" +": " + duration + " ms");
        
        return definition;
    }
    
    @Override
    public ArrayList<ExternalReference> getExternalReferences(Concept concept) {
        return ExternalReferencesExtractor.getPortugueseExternalReferences(concept);   
    }
    
    @Override
    public ArrayList<SemanticType> getSemanticTypes(String cui){
        
        ArrayList<SemanticType> stys = new ArrayList<>();
        
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
                String str = rs.getString("STY");
                String tui = rs.getString("TUI");
                
                if(! stys.contains(new SemanticType(str, null)))
                    stys.add(new SemanticType(tui, str));
            }
            
            //only in CHV
            query = "SELECT * FROM chvconcept c WHERE c.CUI = ?;";
            stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, cui);
            rs = stmt.executeQuery();
            
            while(rs.next()){
                
                String tui = rs.getString("TUI");
                
                if(tui.contains(";")){
                    String[] stySplit = tui.split(";");
                    for(String _sty : stySplit){       
                        if(! stys.contains(new SemanticType(_sty, null))){
                            String str = getSemanticTypeString(_sty);
                            stys.add(new SemanticType(tui, str));
                        }
                    }
                }else{
                    if(! stys.contains(new SemanticType(tui, null))){
                        String str = getSemanticTypeString(tui);
                        stys.add(new SemanticType(tui, str));
                    }
                }
            }
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return stys;
    }
    
    private String getSemanticTypeString(String tui){
        
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        String str = null;
        
        String database = "umls_" + code;
        try {
            connMySQL.setCatalog(database);
        
            String query = "SELECT sty FROM mrsty WHERE tui = ? LIMIT 1";
            stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1, tui);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            str = rs.getString("sty");
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return str;
    }
    
    @Override
    public String conceptExists(String concept){
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;

        try {
            connMySQL.setCatalog("umls_pt");

            String query = "select STR, CUI from MRCONSO WHERE STR = ? UNION ALL select pt, cui from chvstring WHERE pt = ?;";

            stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, concept);
            stmt.setString(2, concept);
            
            ResultSet rs = stmt.executeQuery();

            rs.last();
            int total = rs.getRow();
            
            if (total > 0){
                rs.next();
                return rs.getString("CUI");
            }
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
}
