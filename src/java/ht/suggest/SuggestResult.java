/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.suggest;

/**
 *
 * @author Hugo
 */
public class SuggestResult {
    public boolean success;
    public String reason;
    
    public SuggestResult(){}
    
    public SuggestResult(boolean success, String reason){
        this.success = success;
        this.reason = reason;
    }
}
