/*

Copyright (c) 2008, Jared Crapo All rights reserved. 

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met: 

- Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer. 

- Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution. 

- Neither the name of jactiveresource.org nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission. 

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

 */

package ht.utils;


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import ht.utils.LoggerFactory;

/**
 * a port of the excellent Inflector class in ruby's ActiveSupport library
 * 
 * @version $LastChangedRevision: 5 $ <br>
 *          $LastChangedDate: 2008-05-03 13:44:24 -0600 (Sat, 03 May 2008) $
 * @author $LastChangedBy: jared $
 *
 * Updated by Hugo Sousa @ 3th March 2016 in order to add Portuguese inflection rules, taken from https://gist.github.com/mateusg/924574
 */
public class Inflector {

    public static String camelize( String word, boolean firstLetterInUppercase ) {

        return null;
    }

    private static Pattern underscorePattern = Pattern.compile( "_" );

    /**
     * replace underscores with dashes in a string
     * 
     * @param word
     * @return
     */
    public static String dasherize( String word ) {
        Matcher m = underscorePattern.matcher( word );
        return m.replaceAll( "-" );
    }

    private static Pattern dashPattern = Pattern.compile( "-" );

    /**
     * replace dashes with underscores in a string
     * 
     * @param word
     * @return
     */
    public static String underscorize( String word ) {
        Matcher m = dashPattern.matcher( word );
        return m.replaceAll( "_" );
    }

    private static Pattern doubleColonPattern = Pattern.compile( "::" );
    private static Pattern underscore1Pattern = Pattern
        .compile( "([A-Z]+)([A-Z][a-z])" );
    private static Pattern underscore2Pattern = Pattern
        .compile( "([a-z\\d])([A-Z])" );

    /**
     * The reverse of camelize. Makes an underscored form from the expression in
     * the string.
     * 
     * Changes '::' to '/' to convert namespaces to paths.
     * 
     * @param word
     * @return
     */
    public static String underscore( String word ) {

        String out;
        Matcher m;

        m = doubleColonPattern.matcher( word );
        out = m.replaceAll( "/" );

        m = underscore1Pattern.matcher( out );
        out = m.replaceAll( "$1_$2" );

        m = underscore2Pattern.matcher( out );
        out = m.replaceAll( "$1_$2" );

        out = underscorize( out );

        return out.toLowerCase();
    }

    /**
     * return the plural form of word
     * 
     * @param word
     * @param language
     * @return
     * @throws ht.utils.InvalidLanguageException
     */
    public static String pluralize( String word, String language ) throws InvalidLanguageException {

        ArrayList<ReplacementRule> plurals = null;
        ArrayList<String> uncountables = null;
        
        if(! languages.contains(language))
            throw new InvalidLanguageException("Invalid Language: Only 'en' and 'pt' accepted.");
        else
        {
            switch(language){
                case "en":
                    plurals = pluralsEN;
                    uncountables = uncountablesEN;
                    break;
                case "pt":
                    plurals = pluralsPT;
                    uncountables = uncountablesPT;
                    break;
            }
        }


        String out = new String( word );
        if ( ( out.length() == 0 )
            || ( !uncountables.contains( word.toLowerCase() ) ) ) {
            for ( ReplacementRule r : plurals ) {
                if ( r.find( word ) ) {
                    out = r.replace( word );
                    break;
                }
            }
        }
        return out;
    }

    /**
     * return the singular form of word
     * 
     * @param word
     * @param language
     * @return
     * @throws ht.utils.InvalidLanguageException
     */
    public static String singularize( String word, String language ) throws InvalidLanguageException {

        ArrayList<ReplacementRule> singulars = null;
        ArrayList<String> uncountables = null;
        ArrayList<String> irregularSingulars = null;

        if(! languages.contains(language))
            throw new InvalidLanguageException("Invalid Language: Only 'en' and 'pt' accepted.");
        else
        {
            switch(language){
                case "en":
                    singulars = singularsEN;
                    uncountables = uncountablesEN;
                    irregularSingulars = irregularSingularsEN;
                    break;
                case "pt":
                    singulars = singularsPT;
                    uncountables = uncountablesPT;
                    irregularSingulars = irregularSingularsPT;
                    break;
            }

        }

        String out = new String( word );
        if ( ( out.length() != 0 )
            && ( ! uncountables.contains( out.toLowerCase() ))
            && ( ! irregularSingulars.contains(out.toLowerCase() ))
           ) {

            for ( ReplacementRule r : singulars ) {
                if ( r.find( word ) ) {
                    out = r.replace( word );
                    break;
                }
            }
        }
        return out;
    }
    
    /*
    public static boolean isSingular( String word, String language ) throws InvalidLanguageException{
        
        ArrayList<ReplacementRule> plurals = null;
        
        if(! languages.contains(language))
            throw new InvalidLanguageException("Invalid Language: Only 'en' and 'pt' accepted.");
        else
        {
            switch(language){
                case "en":
                    plurals = pluralsEN;
                    break;
                case "pt":
                    plurals = pluralsPT;
                    break;
            }

        }
                
        for ( ReplacementRule r : plurals ) {
            if ( r.find( word ) ) {
                return true;
            }
        }
        return false;
    };
    */
    
    public static void irregular( String singular, String plural, String language) throws InvalidLanguageException {
        String regexp, repl;
        ArrayList<ReplacementRule> singulars = null;
        ArrayList<ReplacementRule> plurals = null;
        ArrayList<String> irregularSingulars = null;
        ArrayList<String> irregularPlurals = null;

        if(! languages.contains(language))
            throw new InvalidLanguageException("Invalid Language: Only 'en' and 'pt' accepted.");
        else
        {
            switch(language){
                case "en":
                    singulars = singularsEN;
                    plurals = pluralsEN;
                    irregularSingulars = irregularSingularsEN;
                    irregularPlurals = irregularPluralsEN;
                    break;
                case "pt":
                    singulars = singularsPT;
                    plurals = pluralsPT;
                    irregularSingulars = irregularSingularsPT;
                    irregularPlurals = irregularPluralsPT;
                    break;
            }
        }
        
        irregularSingulars.add(singular);
        irregularPlurals.add(plural);
        
        if ( singular.substring( 0, 1 ).toUpperCase().equals(
            plural.substring( 0, 1 ).toUpperCase() ) ) {
            // singular and plural start with the same letter
            regexp = "(?i)(" + singular.substring( 0, 1 ) + ")"
                + singular.substring( 1 ) + "$";
            repl = "$1" + plural.substring( 1 );
            plurals.add( 0, new ReplacementRule( regexp, repl ) );

            regexp = "(?i)(" + plural.substring( 0, 1 ) + ")"
                + plural.substring( 1 ) + "$";
            repl = "$1" + singular.substring( 1 );
            singulars.add( 0, new ReplacementRule( regexp, repl ) );
        } else {
            // singular and plural don't start with the same letter
            regexp = singular.substring( 0, 1 ).toUpperCase() + "(?i)"
                + singular.substring( 1 ) + "$";
            repl = plural.substring( 0, 1 ).toUpperCase()
                + plural.substring( 1 );
            plurals.add( 0, new ReplacementRule( regexp, repl ) );

            regexp = singular.substring( 0, 1 ).toLowerCase() + "(?i)"
                + singular.substring( 1 ) + "$";
            repl = plural.substring( 0, 1 ).toLowerCase()
                + plural.substring( 1 );
            plurals.add( 0, new ReplacementRule( regexp, repl ) );

            regexp = plural.substring( 0, 1 ).toUpperCase() + "(?i)"
                + plural.substring( 1 ) + "$";
            repl = singular.substring( 0, 1 ).toUpperCase()
                + singular.substring( 1 );
            singulars.add( 0, new ReplacementRule( regexp, repl ) );

            regexp = plural.substring( 0, 1 ).toLowerCase() + "(?i)"
                + plural.substring( 1 ) + "$";
            repl = singular.substring( 0, 1 ).toLowerCase()
                + singular.substring( 1 );
            singulars.add( 0, new ReplacementRule( regexp, repl ) );
        }
    }

    private static final ArrayList<ReplacementRule> pluralsEN;
    private static final ArrayList<ReplacementRule> singularsEN;
    private static final ArrayList<String> uncountablesEN;
    private static final ArrayList<String> irregularSingularsEN;
    private static final ArrayList<String> irregularPluralsEN;

    private static final ArrayList<ReplacementRule> pluralsPT;
    private static final ArrayList<ReplacementRule> singularsPT;
    private static final ArrayList<String> uncountablesPT;
    private static final ArrayList<String> irregularSingularsPT;
    private static final ArrayList<String> irregularPluralsPT;

    private static ArrayList<String> languages;

    static {
            languages = new ArrayList<>( 2 );
            languages.add( "en" );
            languages.add( "pt" );
            
            
            pluralsEN = new ArrayList<>( 17 );
            pluralsEN.add( 0, new ReplacementRule( "$", "s" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)s$", "s" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)(ax|test)is$", "$1es" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)(octop|vir)us$", "$1i" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)(alias|status)$", "$1es" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)(bu)s$", "$1es" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)(buffal|tomat)o$", "$1oes" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)([ti])um$", "$1a" ) );
            pluralsEN.add( 0, new ReplacementRule( "sis$", "ses" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)(?:([^f])fe|([lr])f)$", "$1$2ves" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)(hive)$", "$1s" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)([^aeiouy]|qu)y$", "$1ies" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)(x|ch|ss|sh)$", "$1es" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)(matr|vert|ind)(?:ix|ex)$", "$1ices" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)([m|l])ouse$", "$1ice" ) );
            pluralsEN.add( 0, new ReplacementRule( "^(?i)(ox)$", "$1en" ) );
            pluralsEN.add( 0, new ReplacementRule( "(?i)(quiz)$", "$1zes" ) );
            
            
            singularsEN = new ArrayList<>( 24 );
            singularsEN.add( 0, new ReplacementRule( "s$", "" ) );
            singularsEN.add( 0, new ReplacementRule( "(n)ews$", "$1ews" ) );
            singularsEN.add( 0, new ReplacementRule( "([ti])a$", "$1um" ) );
            singularsEN.add( 0, new ReplacementRule( "((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$", "$1$2sis" ) );
            singularsEN.add( 0, new ReplacementRule( "(^analy)ses$", "$1sis" ) );
            singularsEN.add( 0, new ReplacementRule( "([^f])ves$", "$1fe" ) );
            singularsEN.add( 0, new ReplacementRule( "(hive)s$", "$1" ) );
            singularsEN.add( 0, new ReplacementRule( "(tive)s$", "$1" ) );
            singularsEN.add( 0, new ReplacementRule( "([lr])ves$", "$1f" ) );
            singularsEN.add( 0, new ReplacementRule( "([^aeiouy]|qu)ies$", "$1y" ) );
            singularsEN.add( 0, new ReplacementRule( "(s)eries$", "$1eries" ) );
            singularsEN.add( 0, new ReplacementRule( "(m)ovies$", "$1ovie" ) );
            singularsEN.add( 0, new ReplacementRule( "(x|ch|ss|sh)es$", "$1" ) );
            singularsEN.add( 0, new ReplacementRule( "([m|l])ice$", "$1ouse" ) );
            singularsEN.add( 0, new ReplacementRule( "(bus)es$", "$1" ) );
            singularsEN.add( 0, new ReplacementRule( "(o)es$", "$1" ) );
            singularsEN.add( 0, new ReplacementRule( "(shoe)s$", "$1" ) );
            singularsEN.add( 0, new ReplacementRule( "(cris|ax|test)es$", "$1is" ) );
            singularsEN.add( 0, new ReplacementRule( "(octop|vir)i$", "$1us" ) );
            singularsEN.add( 0, new ReplacementRule( "(alias|status)es$", "$1" ) );
            singularsEN.add( 0, new ReplacementRule( "(ox)en$", "$1" ) );
            singularsEN.add( 0, new ReplacementRule( "(virt|ind)ices$", "$1ex" ) );
            singularsEN.add( 0, new ReplacementRule( "(matr)ices$", "$1ix" ) );
            singularsEN.add( 0, new ReplacementRule( "(quiz)zes$", "$1" ) );
            singularsEN.add( 0, new ReplacementRule( "is$", "is" ) );
            
            
            uncountablesEN = new ArrayList<>( 11 );
            uncountablesEN.add( "equipment" );
            uncountablesEN.add( "information" );
            uncountablesEN.add( "rice" );
            uncountablesEN.add( "money" );
            uncountablesEN.add( "species" );
            uncountablesEN.add( "series" );
            uncountablesEN.add( "fish" );
            uncountablesEN.add( "sheep" );
            uncountablesEN.add( "diabetes" );
            uncountablesEN.add( "status" );
            uncountablesEN.add( "abdomen" );
            //uncountablesEN.add( "endophthalmitis" );
            //uncountablesEN.add( "epistaxis" );
            
            
            pluralsPT = new ArrayList<>( 20 );
            pluralsPT.add( 0, new ReplacementRule( "$",  "s") );
            pluralsPT.add( 0, new ReplacementRule( "(s)$", "$1" ) );
            pluralsPT.add( 0, new ReplacementRule( "^(paí)s$", "$1ses") );
            pluralsPT.add( 0, new ReplacementRule( "$",  "s") );
            pluralsPT.add( 0, new ReplacementRule( "(z|r)$",  "$1es") );
            pluralsPT.add( 0, new ReplacementRule( "al$",  "ais") );
            pluralsPT.add( 0, new ReplacementRule( "el$",  "eis") );
            pluralsPT.add( 0, new ReplacementRule( "ul$",  "uis") );
            pluralsPT.add( 0, new ReplacementRule( "ol$",  "ois") );
            pluralsPT.add( 0, new ReplacementRule( "ul$",  "uis") );
            pluralsPT.add( 0, new ReplacementRule( "([^aeou])il$",  "$1is") );
            pluralsPT.add( 0, new ReplacementRule( "m$",  "ns") );
            pluralsPT.add( 0, new ReplacementRule( "^(japon|escoc|ingl|dinamarqu|fregu|portugu)ês$",  "$1eses") );
            pluralsPT.add( 0, new ReplacementRule( "^(|g)ás$",  "$1ases") );
            pluralsPT.add( 0, new ReplacementRule( "ão$",  "ões") );
            pluralsPT.add( 0, new ReplacementRule( "^(irm|m)ão$",  "$1ãos") );
            pluralsPT.add( 0, new ReplacementRule( "^(alem|c|p)ão$",  "$1ães") );
            //sem acentos
            pluralsPT.add( 0, new ReplacementRule( "ao$",  "oes") );
            pluralsPT.add( 0, new ReplacementRule( "^(irm|m)ao$",  "$1aos") );
            pluralsPT.add( 0, new ReplacementRule( "^(alem|c|p)ao$",  "$1aes") );
            
            singularsPT = new ArrayList<>( 19 );
            singularsPT.add( 0, new ReplacementRule( "([^ê])s$", "$1" ) );
            singularsPT.add( 0, new ReplacementRule( "^(á|gá|paí)s$", "$1s" ) );
            singularsPT.add( 0, new ReplacementRule( "(r|z)es$", "$1" ) );
            singularsPT.add( 0, new ReplacementRule( "([^p])ais$", "$1al" ) );
            singularsPT.add( 0, new ReplacementRule( "eis$", "el" ) );
            singularsPT.add( 0, new ReplacementRule( "ois$", "ol" ) );
            singularsPT.add( 0, new ReplacementRule( "uis$", "ul" ) );
            singularsPT.add( 0, new ReplacementRule( "(r|t|f|v)is$", "$1il" ) );
            singularsPT.add( 0, new ReplacementRule( "ns$", "m" ) );
            singularsPT.add( 0, new ReplacementRule( "sses$", "sse" ) );
            singularsPT.add( 0, new ReplacementRule( "^(.*[^s]s)es$", "$1" ) );
            singularsPT.add( 0, new ReplacementRule( "ães$", "ão" ) );
            singularsPT.add( 0, new ReplacementRule( "aes$", "ao" ) );
            singularsPT.add( 0, new ReplacementRule( "ãos$", "ão" ) );
            singularsPT.add( 0, new ReplacementRule( "aos$", "ao" ) );
            singularsPT.add( 0, new ReplacementRule( "ões$", "ão" ) );
            singularsPT.add( 0, new ReplacementRule( "oes$", "ao" ) );
            singularsPT.add( 0, new ReplacementRule( "(japon|escoc|ingl|dinamarqu|fregu|portugu)eses$", "$1ês" ) );
            singularsPT.add( 0, new ReplacementRule( "^(g|)ases$", "$1ás" ) );
            
            uncountablesPT = new ArrayList<>( 6 );
            uncountablesPT.add( "tórax" );
            uncountablesPT.add( "tênis" );
            uncountablesPT.add( "ônibus" );
            uncountablesPT.add( "lápis" );
            uncountablesPT.add( "fênix" );
            uncountablesPT.add( "diabetes" );
            
            
            irregularSingularsEN = new ArrayList<>();
            irregularPluralsEN = new ArrayList<>();
            irregularSingularsPT = new ArrayList<>();
            irregularPluralsPT = new ArrayList<>();
        try {
            irregular( "person", "people", "en" );
            irregular( "man", "men", "en" );
            irregular( "child", "children", "en" );
            irregular( "sex", "sexes", "en" );
            irregular( "move", "moves", "en" );
            irregular( "diagnosis", "diagnoses", "en");
            
            irregular( "país", "países", "pt");
        } catch (InvalidLanguageException ex) {
            
            Logger logger = LoggerFactory.createLogger(Inflector.class.getName());
            
            logger.log(Level.SEVERE, null, ex);
        }
    }
}