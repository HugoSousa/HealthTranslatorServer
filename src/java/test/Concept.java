/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import opennlp.tools.util.Span;

/**
 *
 * @author Hugo
 */
public class Concept {
    public String string;
    public Span span;
    public int row;
    public String CUI;
    
    public String definition;
    public String CHVPreferred;
   
    
    public Concept(){}
    
    public Concept(String string, Span span, int row){
        this.string = string;
        this.span = span;
        this.row = row;
    }
    
    public boolean isNull(){
        return string == null;
    }
}
