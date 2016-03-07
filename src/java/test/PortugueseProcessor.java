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
public class PortugueseProcessor extends TokenProcessor{
    
    public PortugueseProcessor(){
        super();
    }

    /**
     *
     * @param spans
     * @param i
     * @param text
     * @param forward_threshold
     * @return
     */
    @Override
    protected Concept processToken(Span[] spans, int i, String text, int forward_threshold){
        return null;
    };
}
