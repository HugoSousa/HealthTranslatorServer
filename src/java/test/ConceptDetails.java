/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.ArrayList;
import javax.ejb.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 *
 * @author Hugo
 */
@Path("details")
@Singleton
public class ConceptDetails {
    
    private final EnglishProcessor englishProcessor = new EnglishProcessor();
    private final PortugueseProcessor portugueseProcessor = new PortugueseProcessor();
    
    public ConceptDetails() {}
    
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public ConceptDetailsResult test(ConceptDetailsParams param) {
        
        String cui = param.cui;
        String string = param.string;
        String language = param.language;
        
        ConceptProcessor processor = null;
        switch (language) {
            case "en":
                processor = englishProcessor;
                break;
            case "pt":
                processor = portugueseProcessor;
                break;
            default:
                processor = englishProcessor;
                break;
        }
        
        //processor.getDefinition();
        Concept concept = new Concept();
        concept.CUI = cui;
        concept.string = string;
        ArrayList<ExternalReference> externalReferences = processor.getExternalReferences(concept);
        //processor.getRelationships();
        
        //send semantic type
        //send the definition (probably bigger...?)
        //send external references
        //send relationships
        
        
        ConceptDetailsResult result = new ConceptDetailsResult(externalReferences);
        return result;
    }
}
