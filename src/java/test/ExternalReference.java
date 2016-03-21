/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

/**
 *
 * @author Hugo
 */
public class ExternalReference {
    public String url;
    public String label;
    public String source;
    
    public ExternalReference(String url, String label, String source){
        this.url = url;
        this.label = label;
        this.source = source;
    }
}
