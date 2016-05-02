/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.process;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hugo
 */
@XmlRootElement
public class Change {
    
    @XmlElement private int start;
    @XmlElement private int end;
    @XmlElement private String tooltip;
    
    public Change(){}
    
    public Change(int start, int end, String tooltip){
        this.start = start;
        this.end = end;
        this.tooltip = tooltip;
    }
}
