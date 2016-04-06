/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hugo
 */
@XmlRootElement
public class ProcessorParams {
    @XmlElement public String body;
    
    @XmlElement public String styFilter = "one";
    @XmlElement public boolean recognizeOnlyCHV = true;
    @XmlElement public ArrayList<String> supportedLanguages = new ArrayList<>();
    @XmlElement public String contentLanguage = "detected";
    @XmlElement public HashSet<String> semanticTypes = new HashSet<>(); //Arrays.asList("T005", "T007", "T023", "T029", "T030", "T034", "T037", "T040", "T046", "T047", "T048", "T059", "T060", "T061", "T116", "T121", "T125", "T126", "T127", "T129", "T130", "T131", "T184", "T192", "T195", "T200"));
    
    public ProcessorParams(){
    }
    
    public void setDefaultSupportedLanguages(){
        supportedLanguages.clear();
        supportedLanguages.add("en");
        supportedLanguages.add("pt");
    }

}
