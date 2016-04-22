/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.suggest;

import ht.concept.ConceptProcessor;
import ht.concept.EnglishProcessor;
import ht.concept.PortugueseProcessor;
import ht.concept.SemanticType;
import ht.utils.Inflector;
import ht.utils.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

/**
 *
 * @author Hugo
 */
@Path("suggest")
public class Suggest {
    
    @Context
    ServletContext servletContext;
    
    private static Logger logger;
    
    public Suggest(){
        logger = LoggerFactory.createLogger(Suggest.class.getName());
    }
    
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public SuggestResult test(SuggestParams param) {
        
        ConceptProcessor processor; 
        //Connection conn = (Connection)servletContext.getAttribute("connectionDB");
        Connection connection;
        try {
            connection = ((DataSource)servletContext.getAttribute("connPool")).getConnection();
        } catch (SQLException ex) {
            Logger.getLogger(Suggest.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        String suggestion = null;
        String cui;
        
        try {
            suggestion = Inflector.singularize(param.suggestion, param.language);
        } catch (Exception ex) {
            Logger.getLogger(Suggest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        switch(param.language){
            case "en":
                processor = new EnglishProcessor(connection);
                break;
            case "pt":
                processor = new PortugueseProcessor(connection);
                break;
            default:
                return new SuggestResult(false, "Language not supported");
        }
        
        cui = processor.conceptExists(suggestion);
        
        SuggestResult result;
        if(cui == null){
            //check if this tuid has suggested this before
            if( ! hasSuggestedSame(connection, param.tuid, suggestion, param.language)){
                if(insertSuggestion(connection, param.tuid, suggestion, param.language))
                    result = new SuggestResult(true, null);
                else
                    result = new SuggestResult(false, "There was an error inserting your suggestion in the database.");
            }else{
                result = new SuggestResult(false, "You have suggested this concept before.");
            }
        }else{
            ArrayList<SemanticType> semanticTypes = processor.getSemanticTypes(cui);
            String reason = "The concept '" + suggestion + "' already exists. It belongs to the following semantic types:<br>";
            for(int i = 0; i < semanticTypes.size(); i++){
                reason += semanticTypes.get(i).str;
                if(i < semanticTypes.size() - 1)
                    reason += "<br>";
            }
            result = new SuggestResult(false, reason);
        }
        
        try {
            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(Suggest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    private boolean hasSuggestedSame(Connection conn, String tuid, String suggestion, String language){
        //Connection conn = (Connection)servletContext.getAttribute("connectionDB");
        PreparedStatement stmt;

        try {
            conn.setCatalog("umls_" + language);

            String query = "select STR from suggestion WHERE str = ? AND tuid = ?;";

            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, suggestion);
            stmt.setString(2, tuid);
            
            ResultSet rs = stmt.executeQuery();

            if(rs.next())
                return true;
            
            stmt.close();
            rs.close();
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    private boolean insertSuggestion(Connection conn, String tuid, String suggestion, String language){
        //Connection conn = (Connection)servletContext.getAttribute("connectionDB");
        PreparedStatement stmt;

        try {
            conn.setCatalog("umls_" + language);

            String query = "INSERT INTO suggestion (tuid, str) VALUES (?, ?);";

            stmt = conn.prepareStatement(query);

            stmt.setString(1, tuid);
            stmt.setString(2, suggestion);
            
            stmt.execute();
            
            stmt.close();
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;        
    }
}
