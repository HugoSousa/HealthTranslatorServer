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
public class BodyMessage {
    @XmlElement private String body;
    
    public BodyMessage(){}
    
    public String getBody(){
        return body;
    }
}
