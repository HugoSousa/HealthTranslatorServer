/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.details;

import ht.concept.ConceptProcessor;
import ht.concept.Concept;
import ht.concept.EnglishProcessor;
import ht.concept.PortugueseProcessor;
import ht.concept.SemanticType;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
@Path("details")
public class ConceptDetails {
    
    @Context
    ServletContext servletContext;
    
    
    public ConceptDetails() {}
    
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public ConceptDetailsResult test(ConceptDetailsParams param) {
        
        //Connection connection = (Connection)servletContext.getAttribute("connectionDB");
        Connection connection;
        try {
            connection = ((DataSource)servletContext.getAttribute("connPool")).getConnection();
        } catch (SQLException ex) {
            Logger.getLogger(ConceptDetails.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        String cui = param.cui;
        String string = param.string;
        String language = param.language;
        
        Concept concept = new Concept();
        concept.CUI = cui;
        concept.string = string;
        
        ConceptProcessor processor;
        switch (language) {
            case "en":
                processor = new EnglishProcessor(connection);
                break;
            case "pt":
                processor = new PortugueseProcessor(connection);
                break;
            default:
                processor = new EnglishProcessor(connection);
                break;
        }
        
        ArrayList<ExternalReference> externalReferences = processor.getExternalReferences(concept);
        if(param.includeEnglishRefs){
            externalReferences.addAll(new ExternalReferencesExtractor(connection).getEnglishExternalReferences(concept));
        }
        
        String definition = processor.getDefinition(concept);
        //processor.getRelationships();
        
        ArrayList<SemanticType> stys = processor.getSemanticTypes(concept.CUI);
        
        HashMap<String, HashSet<Relationship>> rels = null;
        
        rels = processor.getRelationships(null, concept.CUI);
        /*
        if(stys.contains(new SemanticType("T047", null)))
            rels = processor.getRelationships("T047", concept.CUI);
        else if(stys.contains(new SemanticType("T121", null)))
            rels = processor.getRelationships("T121", concept.CUI);
        */
        boolean hasRating = processor.hasRating(param.tuid, concept.CUI);
        
        ConceptDetailsResult result = new ConceptDetailsResult(definition, externalReferences, stys, rels, hasRating);
        
        try {
            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(ConceptDetails.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }    
}
