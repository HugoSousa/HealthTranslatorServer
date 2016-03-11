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
public class EnglishProcessor extends TokenProcessor {

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
                            if (rs.getString("CUI") != CUI && rs.getString("TTY").equals("PT")) {
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

                        bestMatch.CHVPreferred = CHVPreferred;

                    }
                }

                stmt.close();
                rs.close();
            } catch (SQLException ex) {
                Logger.getLogger(EnglishProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        /*
        if(bestMatch != null){
            bestMatch.definition = getDefinition(bestMatch.string).replace("'", "\\u0027");
        }
        */ 
        
        return bestMatch;
    }
    
    private boolean acceptedSemanticType(ArrayList<String> tuis) {
        
        for(String tui: tuis){
            if(! acceptedSemanticType(tui))
                return false;
        }
        
        return true;
    }

}
