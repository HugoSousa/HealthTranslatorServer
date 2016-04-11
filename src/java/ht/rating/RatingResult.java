/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.rating;

/**
 *
 * @author Hugo
 */
public class RatingResult {
    
    public boolean success;
    public String reason;
    
    public RatingResult(){};
    
    public RatingResult(boolean success, String reason){
        this.success = success;
        this.reason = reason;
    }
}
