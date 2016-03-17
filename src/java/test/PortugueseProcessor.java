/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.util.Span;
import org.jactiveresource.Inflector;

/**
 *
 * @author Hugo
 */
public class PortugueseProcessor extends ConceptProcessor {

    public PortugueseProcessor() {
        super();
        code = "pt";
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
                    ArrayList<String> tuis = new ArrayList<>();
                    int TUIPreferred = -1;
                    
                    if(allResultsFromCHV(rs)){
                        //if there's only many CHV results, it's probably a bad translation (example: "elevado") 
                        if(total < 4){
                            
                            //get the CUIS on a list
                            //if the preferred term is in the results, that's the best CUI
                            while (rs.next()) {
                                if (rs.getRow() == 1) {
                                    //assign the first result at least, so it's not null
                                    CUI = rs.getString("CUI");
                                }
                                
                                tuis.add(rs.getString("tui"));
                                
                                CHVPreferred = rs.getString("chv_pref_pt");
                                //TUIPreferred??
                                //CHVPreferred??
                            }
                            
                            if(tuis.size() > 1){
                                //there are multiple results, elements of tuis may be splitted by ";"
                            }
                        }
                    }else{                  
                        while (rs.next()) {
                            if (rs.getRow() == 1) {
                                //assign the first result at least, so it's not null
                                CUI = rs.getString("CUI");
                            } else {
                                if(rs.getString("CUI") != CUI){
                                    //what to do?
                                }
                            }

                            if (rs.getString("SAB").equals("CHV")) {
                                CHVPreferred = rs.getString("chv_pref_pt");
                            }else{
                                /*if(TUI == null){
                                    TUI = rs.getString("tui");
                                }*/
                                tuis.add(rs.getString("tui"));
                            }
                        }
                    }
                    
                    //TODO TUI may be null if only CHV results are returned!
                    if( tuis.size() > 0 &&
                        (TUIPreferred == -1 && acceptedSemanticType(tuis)) || 
                        (TUIPreferred != -1 && acceptedSemanticType(tuis.get(TUIPreferred)))){
                        bestMatch = new Concept(originalString, new Span(initialSpan.getStart(), span.getEnd()), j+1);
                        bestMatch.CUI = CUI;
                        bestMatch.setCHVPreferred(CHVPreferred);
                        
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
                Logger.getLogger(EnglishProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return bestMatch;
    };
    
    @Override
    protected String getDefinition(Concept concept) {
        
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
               
        return null;
    }
    
    private boolean allResultsFromCHV(ResultSet rs){
        
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
            Logger.getLogger(PortugueseProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }
    
    private boolean acceptedSemanticType(ArrayList<String> tuis) {
        
        for(String tuiList: tuis){
            String[] tuiSplit = tuiList.split(";");
            for(String tui: tuiSplit){
                if(! acceptedSemanticType(tui))
                    return false;
            }
        }
        
        return true;
    }
}
