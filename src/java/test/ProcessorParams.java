/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hugo
 */
@XmlRootElement
public class ProcessorParams {
    //used in Processor & ConceptDetails
    //in ConceptDetails, it represents the concept
    @XmlElement private String body;
    
    // used in ConceptDetails
    @XmlElement private String language;
    
    public ProcessorParams(){}
    
    public String getBody(){
        return body;
    }
    
    public String getLanguage(){
        return language;
    }
}
