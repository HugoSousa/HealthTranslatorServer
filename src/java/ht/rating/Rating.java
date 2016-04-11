/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.rating;

import ht.suggest.Suggest;
import ht.suggest.SuggestParams;
import ht.suggest.SuggestResult;
import ht.utils.LoggerFactory;
import ht.utils.ServletContextClass;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
@Path("rating")
public class Rating {
    
    private static Logger logger;
    
    public Rating(){
        logger = LoggerFactory.createLogger(Rating.class.getName());
    }
    
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public RatingResult test(RatingParams param) {
        
        RatingResult result;
        boolean success = insertResult(param.cui, param.tuid, param.language, param.definition, param.externalReferences, param.relationships, param.general);
        
        if(success)
            return new RatingResult(true, null);
        else
            return new RatingResult(false, "An error occurred submitting your evaluation.");
    }
    
    public boolean insertResult(String cui, String tuid, String language, int definition, int externalReferences, int relationships, int general){
        
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        String database = "umls_" + language;
        try {
            connMySQL.setCatalog(database);
            
            String query = "INSERT INTO rating (cui, tuid, definition, ext_refs, relationships, general) VALUES(?,?,?,?,?,?);";
            stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, cui);
            stmt.setString(2, tuid);
            stmt.setInt(3, definition);
            stmt.setInt(4, externalReferences);
            stmt.setInt(5, relationships);
            stmt.setInt(6, general);

            stmt.execute();
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;
    }
}
