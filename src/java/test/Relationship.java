/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.Objects;

/**
 *
 * @author Hugo
 */
public class Relationship {
    
    public String concept1;
    public String concept2;
    public String label;
    
    public Relationship(String concept1, String label, String concept2) {
        this.concept1 = concept1;
        this.label = label;
        this.concept2 = concept2; 
    }
    
    //only the label and concept2 matter for the comparison, as concept1 are just different terms for the same concept (CUI)
    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof Relationship))return false;
        Relationship otherRelationship = (Relationship)other;
        return this.concept2.equals(otherRelationship.concept2) && this.label.equals(otherRelationship.label);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.concept2);
        hash = 79 * hash + Objects.hashCode(this.label);
        return hash;
    }
}
