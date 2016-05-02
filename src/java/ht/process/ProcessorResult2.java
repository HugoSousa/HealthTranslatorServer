/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.process;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hugo
 */
@XmlRootElement
public class ProcessorResult2 {
    
    @XmlElement private ArrayList<Change> changes;
    
    public ProcessorResult2(){}
    
    public ProcessorResult2(ArrayList<Change> changes){
        this.changes = changes;
    }
    
}
