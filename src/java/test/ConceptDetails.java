/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author Hugo
 */
@Path("details")
public class ConceptDetails {
    
    public ConceptDetails() {
    }
    
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public Response test(BodyMessage param) {
        
        String concept = param.getBody();
        
        return Response.status(200).entity(null).build();
    }
}
