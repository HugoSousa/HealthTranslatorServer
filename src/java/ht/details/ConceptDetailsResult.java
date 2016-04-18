/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.details;

import ht.concept.SemanticType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hugo
 */
@XmlRootElement
public class ConceptDetailsResult {
    
    @XmlElement private String definition;
    @XmlElement private ArrayList<ExternalReference> references;
    @XmlElement private ArrayList<SemanticType> semanticTypes;
    @XmlElement private HashMap<String, HashSet<Relationship>> relationships;
    @XmlElement private boolean hasRating;
    
    public ConceptDetailsResult(){}
    
    public ConceptDetailsResult(String definition, ArrayList<ExternalReference> refs, ArrayList<SemanticType> stys, HashMap<String, HashSet<Relationship>> rels, boolean hasRating){
        this.definition = definition;
        this.references = refs;
        this.semanticTypes = stys;
        this.relationships = rels;
        this.hasRating = hasRating;
    }
}
