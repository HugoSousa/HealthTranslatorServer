/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hugo
 */
@XmlRootElement
public class ConceptDetailsResult {
    
    @XmlElement private ArrayList<ExternalReference> refs;
    
    public ConceptDetailsResult(){}
    
    public ConceptDetailsResult(ArrayList<ExternalReference> refs){
        this.refs = refs;
    }
}
