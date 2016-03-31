/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.concept;

import opennlp.tools.util.Span;

/**
 *
 * @author Hugo
 */
public class Concept {
    public String string;
    public Span span;
    public int words;
    public String CUI;
    
    public String definition;
    public String CHVPreferred;
   
    
    public Concept(){}
    
    public Concept(String string, Span span, int words){
        this.string = string;
        this.span = span;
        this.words = words;
    }
    
    public void setDefinition(String definition){
        this.definition = definition.replace("'", "&#39;");
    }
    
    public void setCHVPreferred(String CHVPreferred){
        if(CHVPreferred != null){
            this.CHVPreferred = CHVPreferred.replace("'", "&#39;");
        }
    }
}
