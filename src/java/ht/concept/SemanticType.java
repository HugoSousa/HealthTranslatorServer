/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.concept;

import java.util.Objects;

/**
 *
 * @author Hugo
 */
public class SemanticType {
    public String tui;
    public String str;
    
    public SemanticType(String tui, String str){
        this.tui = tui;
        this.str = str;
    }
    
    @Override
    public boolean equals(Object obj) {
       if (!(obj instanceof SemanticType))
            return false;
        if (obj == this)
            return true;

        SemanticType other = (SemanticType) obj;
        return other.tui.equals(this.tui);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.tui);
        return hash;
    }
    
}
