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
public class ProcessorResult {
    
    @XmlElement private String body;
    @XmlElement private int conceptCounter;
    @XmlElement private long processingTime;
    
    public ProcessorResult(){}
    
    public ProcessorResult(String body, int conceptCounter, long processingTime){
        this.body = body;
        this.conceptCounter = conceptCounter;
        this.processingTime = processingTime;
    }

}
