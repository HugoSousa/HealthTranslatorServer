/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.suggest;

import ht.concept.ConceptProcessor;
import ht.concept.EnglishProcessor;
import ht.concept.PortugueseProcessor;
import ht.utils.Inflector;
import ht.utils.LoggerFactory;
import ht.utils.ServletContextClass;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 *
 * @author Hugo
 */
@Path("suggest")
public class Suggest {
    
    private static Logger logger;
    
    public Suggest(){
        logger = LoggerFactory.createLogger(Suggest.class.getName());
    }
    
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public SuggestResult test(SuggestParams param) {
        
        ConceptProcessor processor; 
        
        String suggestion = null;
        String cui;
        
        try {
            suggestion = Inflector.singularize(param.suggestion, param.language);
        } catch (Exception ex) {
            Logger.getLogger(Suggest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        switch(param.language){
            case "en":
                processor = new EnglishProcessor();
                break;
            case "pt":
                processor = new PortugueseProcessor();
                break;
            default:
                return new SuggestResult(false, "Language not supported");
        }
        
        cui = processor.conceptExists(suggestion);
        
        if(cui == null){
            //check if this tuid has suggested this before
            if( ! hasSuggestedSame(param.tuid, suggestion, param.language)){
                if(insertSuggestion(param.tuid, suggestion, param.language))
                    return new SuggestResult(true, null);
                else
                    return new SuggestResult(false, "There was an error inserting your suggestion in the database.");
            }else{
                return new SuggestResult(false, "You have suggested this concept before.");
            }
        }else{
            ArrayList<String> semanticTypes = processor.getSemanticTypes(cui);
            String reason = "The concept '" + suggestion + "' already exists. It belongs to the following semantic types:<br>";
            for(int i = 0; i < semanticTypes.size(); i++){
                reason += semanticTypes.get(i);
                if(i < semanticTypes.size() - 1)
                    reason += "<br>";
            }
            return new SuggestResult(false, reason);
        }
    }
    
    private boolean hasSuggestedSame(String tuid, String suggestion, String language){
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;

        try {
            connMySQL.setCatalog("umls_" + language);

            String query = "select STR from suggestion WHERE str = ? AND tuid = ?;";

            stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, suggestion);
            stmt.setString(2, tuid);
            
            ResultSet rs = stmt.executeQuery();

            if(rs.next())
                return true;
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    private boolean insertSuggestion(String tuid, String suggestion, String language){
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;

        try {
            connMySQL.setCatalog("umls_" + language);

            String query = "INSERT INTO suggestion (tuid, str) VALUES (?, ?);";

            stmt = connMySQL.prepareStatement(query);

            stmt.setString(1, tuid);
            stmt.setString(2, suggestion);
            
            stmt.execute();
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;        
    }
}
