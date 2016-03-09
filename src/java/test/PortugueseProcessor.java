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
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.util.Span;
import org.jactiveresource.Inflector;

/**
 *
 * @author Hugo
 */
public class PortugueseProcessor extends TokenProcessor {

    public PortugueseProcessor() {
        super();
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
            if (j == 0 && (singularQueryToken.length() <= 2 || stopwords.containsKey(singularQueryToken))) {
                break;
            } else if (punctuationMatcher.matches() || numberMatcher.matches()) {
                break;
            }

            Connection connMySQL = ServletContextClass.conn_MySQL;
            PreparedStatement stmt;

            try {
                connMySQL.setCatalog("umls_pt");

                String query = "select c.CUI, STR, SAB, TTY, null as chv_pref_pt, null AS umls_pref_pt, tui\n"
                        + "from MRCONSO c, MRSTY s "
                        + "WHERE STR = ? "
                        + "AND c.cui = s.cui "
                        + "UNION ALL "
                        + "select s.cui, pt, 'CHV' AS sab, null as tty, chv_pref_pt, umls_pref_pt, null as tui "
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
                if(singularQueryToken.equals("na")){
                    int a = 2;
                }
                //System.out.print("TOKEN " + singularQueryToken + ": " + total + " results");

                if (total >= 1) {
                    //se tiver só resultados do CHV e forem muitos, provavelmente é uma má tradução! (exemplo "elevado")
                    String CUI = null;
                    String CHVPreferred = null;
                    String TUI = null;
                    while (rs.next()) {
                        if (rs.getRow() == 1) {
                            //assign the first result at least, so it's not null
                            CUI = rs.getString("CUI");
                        } else {
                            //TODO os TTY são diferentes da SNOMED!!
                            //desambiguação de diferentes CUI, como?
                            /*
                            if (rs.getString("CUI") != CUI && !rs.getString("SAB").equals("CHV") && rs.getString("TTY").equals("PT")) {
                                CUI = rs.getString("CUI");
                            }
                            */
                        }

                        //Collator ptCollator = Collator.getInstance(Locale.forLanguageTag("pt"));
                        //ptCollator.setStrength(Collator.PRIMARY);
                        if (rs.getString("SAB").equals("CHV")) {
                            CHVPreferred = rs.getString("chv_pref_pt");
                        }else{
                            if(TUI == null){
                                TUI = rs.getString("tui");
                            }
                        }
                    }
                    
                    //TODO TUI may be null if only CHV results are returned!
                    if(TUI == null || acceptedSemanticType(TUI)){
                        bestMatch = new Concept(originalString, new Span(initialSpan.getStart(), span.getEnd()), j+1);
                        bestMatch.CUI = CUI;
                        bestMatch.CHVPreferred = CHVPreferred;

                        if (CHVPreferred == null) {
                            //System.out.println("The concept " + CUI + " (" + singularQueryToken + ") is not in CHV.");
                        }
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
;
}
