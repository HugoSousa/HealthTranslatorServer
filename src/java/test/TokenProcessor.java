/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

/**
 *
 * @author Hugo
 */
public abstract class TokenProcessor {
    
    protected ConcurrentHashMap<String, String> stopwords;
    protected Tokenizer tokenizer; 
    protected HashSet<String> semanticTypes;
    protected Matcher punctuationMatcher;
    protected Matcher numberMatcher;
    
    /**
     *
     */
    public TokenProcessor(){
        Pattern punctuationPattern = Pattern.compile("\\p{Punct}", Pattern.CASE_INSENSITIVE);
        Pattern numberPattern = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
        punctuationMatcher = punctuationPattern.matcher("");
        numberMatcher = numberPattern.matcher("");
    }

    
    protected void setSemanticTypes(HashSet<String> semanticTypes){
        this.semanticTypes = semanticTypes;
    }
    
    protected void setTokenizer(Tokenizer tokenizer){
        this.tokenizer = tokenizer;
    };
    
    protected void setStopwords(ConcurrentHashMap<String, String> stopwords){
        this.stopwords = stopwords;
    }
    
    protected boolean acceptedSemanticType(String sty) {
        return semanticTypes.contains(sty);
    }
    
    protected Concept processToken(Span[] spans, int i, String text, int forward_threshold){
        return null;
    };
    
}
