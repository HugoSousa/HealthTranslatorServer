/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.utils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;


/**
 *
 * @author Hugo
 */
@Path("ping")
public class Ping {

    @GET
    public Response ping() {
        return Response.status(200).build();
    }
}
