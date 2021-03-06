/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.process;

import ht.concept.Concept;
import ht.concept.PortugueseProcessor;
import ht.concept.EnglishProcessor;
import ht.concept.ConceptProcessor;
import ht.utils.LoggerFactory;
import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;
import ht.utils.Inflector;
import ht.utils.InvalidLanguageException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import rufus.lzstring4java.LZString;


/**
 * REST Web Service
 *
 * @author Hugo
 */
@Path("process")
public class Processor {

    @Context
    ServletContext servletContext;

    //private final ConcurrentHashMap<String, String> stopwordsEN = new ConcurrentHashMap<>();
    @Resource
    private final HashSet<String> defaultSemanticTypes = new HashSet<>(Arrays.asList("T005", "T007", "T019", "T020", "T023", "T029", "T030", "T037", "T046", "T047", "T048", "T059", "T060", "T061", "T109", "T116", "T121", "T125", "T126", "T127", "T129", "T130", "T131", "T184", "T190", "T194", "T195", "T200", "T204"));

    //private Matcher punctuationMatcher;
    //private Matcher numberMatcher;
    //private TokenizerModel modelEN;
    @Resource
    private final int FORWARD_THRESHOLD = 5;
    
    /*
    //number of text chunks bigger than THRESHOLD_TEXT_SIZE that are extracted for language detection
    private int THRESHOLD_TEXT_DETECTION = 3;

    //size of the text that is considered as a chunk to be used in language detection
    private int THRESHOLD_TEXT_SIZE = 100;
    
    private final int MAX_RETRIES = 3;
    */
    @Resource
    private static Logger logger; 
    
    /**
     * Creates a new instance of Processor
     */
    public Processor() {
        //System.out.println("Processor constructor");
    }
   

    /*
     * load stopwords, models, semantic types and logger into memory
     */
    @PostConstruct
    public void preLoad() {

        //System.out.println("Processor preload");
        
        logger = LoggerFactory.createLogger(Processor.class.getName());
        
    }

    /**
     * Retrieves representation of an instance of test.TestResource
     *
     * @param param
     * @return an instance of java.lang.String
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public ProcessorResult2 process(ProcessorParams param) {

        //System.out.println("Starting Processing");
        SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS");
        Date date = new Date();
        //System.out.println(SDF.format(date));

        long startTimeX = System.nanoTime();
        //String decompressed = LZString.decompressFromUTF16(param.body);
        //long endTimeX = System.nanoTime();
        //long durationX = (endTimeX - startTimeX) / 1000000;
        //System.out.println("DURATION TO DECOMPRESS: " + durationX + " ms");
        //System.out.println("DECOMPRESSED: " + decompressed);

        long startTime = System.nanoTime();
        //param.body = decompressed;
        //ProcessorResult result = processDocumentV2(param);
        ProcessorResult2 result = processDocumentV3(param);
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        //System.out.println("DURATION: " + duration + " ms for " + param.body);
      
       return result;     
    }
    
    public ProcessorResult2 processDocumentV3(ProcessorParams param){
        
        TokenizerModel portugueseTokenizer  = (TokenizerModel)servletContext.getAttribute("portugueseTokenizer");
        TokenizerModel englishTokenizer = (TokenizerModel)servletContext.getAttribute("englishTokenizer");
    
        ConcurrentHashMap<String, String> portugueseStopwords = (ConcurrentHashMap<String, String>)servletContext.getAttribute("portugueseStopwords");
        ConcurrentHashMap<String, String> englishStopwords = (ConcurrentHashMap<String, String>)servletContext.getAttribute("englishStopwords");
        
        ResourceBundle portugueseMessages = (ResourceBundle)servletContext.getAttribute("portugueseMessages");
        ResourceBundle englishMessages = (ResourceBundle)servletContext.getAttribute("englishMessages");
        
        String text = param.body;
        //String text = LZString.decompressFromUTF16(param.body);
        
        Connection connection;
        try {
            connection = ((DataSource)servletContext.getAttribute("connPool")).getConnection();
        } catch (SQLException ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
                
        HashSet<String> semanticTypes;
        if(param.semanticTypes == null || param.semanticTypes.isEmpty())
            semanticTypes = defaultSemanticTypes;
        else
            semanticTypes = param.semanticTypes;    
        
        ConceptProcessor processor = null;
        ResourceBundle messages = null;
        
        switch (param.language) {
            case "en":
                processor = new EnglishProcessor(connection, englishStopwords, englishTokenizer, semanticTypes);
                //messages = englishMessages;
                break;
            case "pt":
                processor = new PortugueseProcessor(connection, portugueseStopwords, portugueseTokenizer, semanticTypes);
                //messages = portugueseMessages;
                break;
            default:
                processor = new EnglishProcessor(connection, englishStopwords, englishTokenizer, semanticTypes);
                //messages = englishMessages;
                break;
        }
        
        if(param.contentLanguage == null)
            param.contentLanguage = "detected";
        
        if((param.contentLanguage.equals("detected") && param.language.equals("en")) || param.contentLanguage.equals("en"))
            messages = englishMessages;
        else if((param.contentLanguage.equals("detected") && param.language.equals("pt")) || param.contentLanguage.equals("pt"))
            messages = portugueseMessages;
        else
            messages = englishMessages;
        
        if(param.recognizeOnlyCHV == null)
            param.recognizeOnlyCHV = true;
        
        processor.recognizeOnlyCHV = param.recognizeOnlyCHV;
        
        if(param.recognizeWithoutDefinition == null)
            param.recognizeWithoutDefinition = true;
        
        if(param.styFilter == null)
            param.styFilter = "all";
        
        if(param.styFilter.equals("all"))
            processor.allAccepted = true;
        else if(param.styFilter.equals("one"))
            processor.allAccepted = false;
        
        Tokenizer tokenizer = new TokenizerME(processor.tokenizerModel);
        
        Span spans[] = tokenizer.tokenizePos(text);
        //Span[] spansCopy = new Span[spans.length];
        //System.arraycopy( spans, 0, spansCopy, 0, spans.length );
        //System.out.println("TEXT: " + text);
        //System.out.println("SPANS: " + spans.length);

        ArrayList<Change> resultChanges = new ArrayList<>();
        
        for (int i = 0; i < spans.length; i++) {

            Span initialSpan = spans[i];
            
            Concept bestMatch = processor.processToken(spans, i, text, FORWARD_THRESHOLD);
            
            if(bestMatch != null){                
                //replace "'" so it doesn't break the tooltip html if the definition contains it
                String definition = processor.getDefinition(bestMatch);
                if(definition != null){
                    bestMatch.setDefinition(definition);
                }
            }

            if (bestMatch != null && ((! param.recognizeWithoutDefinition && bestMatch.definition != null) || param.recognizeWithoutDefinition) ) {
                i += bestMatch.words - 1;
                
                /*
                if (lastFound == null) {
                    splitText.add(text.substring(0, initialSpan.getStart()));
                } else {
                    splitText.add(text.substring(lastFound.span.getEnd(), bestMatch.span.getStart()));
                }
                */
                String definitionTooltip = replaceConcept(bestMatch, param.language, messages);
                resultChanges.add(new Change(bestMatch.span.getStart(), bestMatch.span.getEnd(), definitionTooltip));
                
                //lastFound = bestMatch;
                
            }   
        }
        
        try {
            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return new ProcessorResult2(resultChanges);
    }
/*
    public ProcessorResult processDocumentV2(ProcessorParams param){

        TokenizerModel englishTokenizer = (TokenizerModel)servletContext.getAttribute("englishTokenizer");
        TokenizerModel portugueseTokenizer  = (TokenizerModel)servletContext.getAttribute("portugueseTokenizer");
    
        ConcurrentHashMap<String, String> englishStopwords = (ConcurrentHashMap<String, String>)servletContext.getAttribute("englishStopwords");
        ConcurrentHashMap<String, String> portugueseStopwords = (ConcurrentHashMap<String, String>)servletContext.getAttribute("portugueseStopwords");

        ResourceBundle englishMessages = (ResourceBundle)servletContext.getAttribute("englishMessages");
        ResourceBundle portugueseMessages = (ResourceBundle)servletContext.getAttribute("portugueseMessages");
        
        //System.out.println("CONTENT: " + content);
        String content = param.body;
        
        long startTime = System.nanoTime();
        int conceptCounter = 0;

        Document doc = Jsoup.parseBodyFragment(content);
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));

        Elements elements = doc.body().children().select("*");
        
        String language = param.language;
        if(language == null) 
            language = detectLanguage(elements);
        
        ConceptProcessor processor;
        ResourceBundle messages;

        Connection connection;
        try {
            connection = ((DataSource)servletContext.getAttribute("connPool")).getConnection();
        } catch (SQLException ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
       
        if(! param.supportedLanguages.contains(language))
            return new ProcessorResult("This language is not supported");
        
        HashSet<String> semanticTypes;
        if(! param.semanticTypes.isEmpty())
            semanticTypes = param.semanticTypes;
        else
            semanticTypes = defaultSemanticTypes;
        
        switch (language) {
            case "en":
                processor = new EnglishProcessor(connection, englishStopwords, englishTokenizer, semanticTypes);
                //messages = englishMessages;
                break;
            case "pt":
                processor = new PortugueseProcessor(connection, portugueseStopwords, portugueseTokenizer, semanticTypes);
                //messages = portugueseMessages;
                break;
            default:
                processor = new EnglishProcessor(connection, englishStopwords, englishTokenizer, semanticTypes);
                //messages = englishMessages;
                break;
        }
        
        if((param.contentLanguage.equals("detected") && language.equals("en")) || param.contentLanguage.equals("en"))
            messages = englishMessages;
        else if((param.contentLanguage.equals("detected") && language.equals("pt")) || param.contentLanguage.equals("pt"))
            messages = portugueseMessages;
        else
            messages = englishMessages;
        
        processor.recognizeOnlyCHV = param.recognizeOnlyCHV;
        
        if(param.styFilter.equals("all"))
            processor.allAccepted = true;
        else if(param.styFilter.equals("one"))
            processor.allAccepted = false;
        
        processor.setAcceptedSemanticTypes(param.semanticTypes);

        for (Element element : elements) {

            //ignore scripts
            if (element.tagName().equals("script")) {
                element.remove();
                continue;
            }
            
            if(element.tagName().equals("x-health-translator")){
                if(element.hasClass("health-translator"))
                    conceptCounter++;
                continue;
            }

            //System.out.println("ELEMENT: " + element.tagName());
            try {

                //String text = element.ownText();
                List<Node> nodes = element.childNodes();
                //System.out.println("TEXT: " + text);


                for (Node node : nodes) {
                    //System.out.println("NODE: " + node);

                    if (node instanceof TextNode) {
                        //System.out.println("NODE: TEXT");

                        String text = ((TextNode) node).getWholeText();
                        
                        TokenizerME tokenizer = new TokenizerME(processor.tokenizerModel);
                        Span spans[] = tokenizer.tokenizePos(text);

                        ArrayList<String> splitText = new ArrayList<>();
                        Concept lastFound = null;
                        //EnglishStemmer enStemmer = new EnglishStemmer();

                        for (int i = 0; i < spans.length; i++) {

                            Span initialSpan = spans[i];
                            Concept bestMatch = processor.processToken(spans, i, text, FORWARD_THRESHOLD);
                            
                            if(bestMatch != null){
                                //replace "'" so it doesn't break the tooltip html if the definition contains it
                                String definition = processor.getDefinition(bestMatch);
                                if(definition != null){
                                    bestMatch.setDefinition(definition);
                                }
                            }

                            if (bestMatch != null && ((! param.recognizeWithoutDefinition && bestMatch.definition != null) || param.recognizeWithoutDefinition) ) {
                                i += bestMatch.words - 1;
                                conceptCounter++;

                                if (lastFound == null) {
                                    splitText.add(text.substring(0, initialSpan.getStart()));
                                } else {
                                    try {
                                        splitText.add(text.substring(lastFound.span.getEnd(), bestMatch.span.getStart()));
                                    } catch (StringIndexOutOfBoundsException e) {
                                        System.out.println("e: " + e);
                                    }
                                }

                                String replace = replaceConcept(bestMatch, language, messages);
                                splitText.add(replace);

                                lastFound = bestMatch;
                            }
                        }

                        if (lastFound != null) {
                            splitText.add(text.substring(lastFound.span.getEnd()));
                        }

                        if (splitText.size() > 0) {
                            //replace textnode by text-element-text

                            
                            // with the current splitting method:
                            // even chunks are text nodes
                            // odd chunks are data nodes (new html created)
                            // this way, as we are replacing by a single datanode, we need to escape the text parts
                            StringBuilder sb = new StringBuilder(text.length());
                            for (int i = 0; i < splitText.size(); i++) {
                                String chunk = splitText.get(i);
                                if (i % 2 == 0) {
                                    sb.append(escapeHtml4(chunk));
                                } else {
                                    sb.append(chunk);
                                }
                            }

                            DataNode dn = new DataNode(sb.toString(), "");
                            node.replaceWith(dn);

                            //System.out.println("node: " + node);
                            //System.out.println("new node: " + dn);
                        }

                    } else {
                        //System.out.println("NODE: NOT TEXT");
                    }
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, null, e);
            }
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000;// / 1000000;
        //System.out.println("BODY: " + doc.body().toString());
        ProcessorResult result = new ProcessorResult(doc.body().toString(), conceptCounter, duration, language);
        
        try {
            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //return result;
        return new ProcessorResult(param.body, 0, 1, "pt");
    }
    */

    private String replaceConcept(Concept bestMatch, String language, ResourceBundle messages) {
        
        boolean hasDifferentCHV = false;
        boolean hasDifferentUMLS = false;

        try {
            hasDifferentCHV = bestMatch.CHVPreferred != null && ! Inflector.singularize(bestMatch.string, language).equalsIgnoreCase(Inflector.singularize(bestMatch.CHVPreferred, language));
            hasDifferentUMLS = bestMatch.UMLSPreferred != null && ! Inflector.singularize(bestMatch.string, language).equalsIgnoreCase(Inflector.singularize(bestMatch.UMLSPreferred, language));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        String preferred = "";
        String definition = "";
        
        if(hasDifferentCHV && hasDifferentUMLS)
        {
            preferred = "<p>" + messages.getString("aka") + " \"" + bestMatch.CHVPreferred + "\" " + messages.getString("lay_terminology") + " " + messages.getString("or") + " \"" + bestMatch.UMLSPreferred + "\" " + messages.getString("medical_terminology") + "</p>";
        }else if(hasDifferentCHV){
            preferred = "<p>" + messages.getString("aka") + " \"" + bestMatch.CHVPreferred + "\" " + messages.getString("lay_terminology") + "</p>";
        }else if(hasDifferentUMLS){
            preferred = "<p>" + messages.getString("aka") + " \"" + bestMatch.UMLSPreferred + "\" " + messages.getString("medical_terminology") + "</p>";
        }
        
        if(bestMatch.definition != null && ! bestMatch.definition.isEmpty()){
            definition = "<p class=\"definition\">" + bestMatch.definition + "</p>";
        }else{
            definition = "<p>" + messages.getString("sorry") + "<br>" +  messages.getString("no_definition") + "</p>"; 
        }
        String tooltip = preferred + definition + "<a href=\"#\" data-toggle=\"modal\" data-target=\"#health-translator-modal\">" + messages.getString("more_info") + "</a>";
        //String newString = "<x-health-translator style='display:inline' class='health-translator'><x-health-translator class='medical-term-translate' data-toggle='tooltip' title='" + tooltip + "' data-html='true' data-lang=\"" + language + "\" data-cui=\"" + bestMatch.CUI + "\" data-term=\"" + bestMatch.string + "\">" + bestMatch.string + "</x-health-translator></x-health-translator>";
        String tooltipString = "<x-health-translator class='medical-term-translate' data-toggle='tooltip' title='" + tooltip + "' data-html='true' data-lang=\"" + language + "\" data-cui=\"" + bestMatch.CUI + "\" data-term=\"" + bestMatch.string + "\">" + bestMatch.string + "</x-health-translator>";
        
        return tooltipString;
    }

    /*
    private String detectLanguage(Elements elements) {
        //do a first iteration over elements (text) to detect language
        boolean successfulDetection;
        int tries = 0;
        do {   
            Detector detector = null;
            try {
                detector = DetectorFactory.create();
            } catch (LangDetectException ex) {
                logger.log(Level.SEVERE, null, ex);
            }

            successfulDetection = true;
            int counter = 0;

            for (Element element : elements) {
                //System.out.println("TEXT: " + element.text());
                if (counter >= THRESHOLD_TEXT_DETECTION) {
                    break;
                }

                if (element.text().length() > THRESHOLD_TEXT_SIZE) {
                    counter++;
                    detector.append(" ");
                    detector.append(element.text());
                }
            }
            try {
                ArrayList<Language> languageList = detector.getProbabilities();

                if (languageList.size() > 0) {
                    String bestLanguage = languageList.get(0).lang;

                    return bestLanguage;
                }
                //System.out.println("LANGUAGES: " + languageList);
            } catch (LangDetectException ex) {
                if (ex.getMessage().equals("no features in text")) {
                    successfulDetection = false;
                    THRESHOLD_TEXT_SIZE = 0;
                    THRESHOLD_TEXT_DETECTION = 99999;
                } else {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            
            tries++;
            
        } while (!successfulDetection && tries < MAX_RETRIES);
        
        return "en";
    }
    */
 }
