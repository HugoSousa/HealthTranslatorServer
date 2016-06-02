/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.rating;

import ht.utils.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
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
@Path("rating")
public class Rating {
    
    @Context
    ServletContext servletContext;
    
    @Resource
    private static Logger logger;
    
    public Rating(){
        logger = LoggerFactory.createLogger(Rating.class.getName());
    }
    
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public RatingResult test(RatingParams param) {
        
        Connection connection;
        try {
            connection = ((DataSource)servletContext.getAttribute("connPool")).getConnection();
        } catch (SQLException ex) {
            Logger.getLogger(Rating.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        boolean success = insertResult(connection, param.cui, param.tuid, param.language, param.definition, param.externalReferences, param.relationships, param.general);
        
        try {
            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(Rating.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(success)
            return new RatingResult(true, null);
        else
            return new RatingResult(false, "An error occurred submitting your evaluation.");
    }
    
    public boolean insertResult(Connection conn, String cui, String tuid, String language, int definition, int externalReferences, int relationships, int general){
        
        //Connection connMySQL = ServletContextClass.conn_MySQL;
        //Connection conn = (Connection)servletContext.getAttribute("connectionDB");
        PreparedStatement stmt;
        
        String database = "umls_" + language;
        try {
            conn.setCatalog(database);
            
            String query = "INSERT INTO rating (cui, tuid, definition, ext_refs, relationships, general) VALUES(?,?,?,?,?,?);";
            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            stmt.setString(1, cui);
            stmt.setString(2, tuid);
            stmt.setInt(3, definition);
            stmt.setInt(4, externalReferences);
            stmt.setInt(5, relationships);
            stmt.setInt(6, general);

            stmt.execute();
            
            stmt.close();
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;
    }
}
