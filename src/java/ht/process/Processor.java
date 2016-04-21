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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.servlet.ServletContext;
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
@Singleton
public class Processor {

    @Context
    ServletContext servletContext;

    //private final ConcurrentHashMap<String, String> stopwordsEN = new ConcurrentHashMap<>();
    private final HashSet<String> semanticTypes = new HashSet<>(Arrays.asList("T005", "T007", "T023", "T029", "T030", "T034", "T037", "T040", "T046", "T047", "T048", "T059", "T060", "T061", "T116", "T121", "T125", "T126", "T127", "T129", "T130", "T131", "T184", "T192", "T195", "T200"));

    //private Matcher punctuationMatcher;
    //private Matcher numberMatcher;
    //private TokenizerModel modelEN;
    private final int FORWARD_THRESHOLD = 5;

    //number of text chunks bigger than THRESHOLD_TEXT_SIZE that are extracted for language detection
    private int THRESHOLD_TEXT_DETECTION = 3;

    //size of the text that is considered as a chunk to be used in language detection
    private int THRESHOLD_TEXT_SIZE = 100;
    
    private final int MAX_RETRIES = 3;
    
    private static Logger logger; 
    
    private Tokenizer englishTokenizer;
    private Tokenizer portugueseTokenizer;
    
    private ConcurrentHashMap<String, String> englishStopwords;
    private ConcurrentHashMap<String, String> portugueseStopwords;
    
    //private final EnglishProcessor englishProcessor = new EnglishProcessor();
    //private final PortugueseProcessor portugueseProcessor = new PortugueseProcessor();
    
    private ResourceBundle englishMessages;
    private ResourceBundle portugueseMessages;

    /**
     * Creates a new instance of Processor
     */
    public Processor() {
        System.out.println("Processor constructor");
    }
    

    /*
     * load stopwords, models, semantic types and logger into memory
     */
    @PostConstruct
    public void preLoad() {

        System.out.println("Processor preload");
        
        //englishProcessor.setAcceptedSemanticTypes(semanticTypes);
        //portugueseProcessor.setAcceptedSemanticTypes(semanticTypes);
        
        logger = LoggerFactory.createLogger(Processor.class.getName());
        
        InputStream is = null;
        try {
            is = servletContext.getResourceAsStream("/WEB-INF/models/en-token.bin");
            TokenizerModel modelEN = new TokenizerModel(is);
            englishTokenizer = new TokenizerME(modelEN);
            //englishProcessor.setTokenizer(tokenizerEN);
            is.close();

            is = servletContext.getResourceAsStream("/WEB-INF/models/pt-token.bin");
            TokenizerModel modelPT = new TokenizerModel(is);
            portugueseTokenizer = new TokenizerME(modelPT);
            //portugueseProcessor.setTokenizer(tokenizerPT);
            is.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        is = servletContext.getResourceAsStream("/WEB-INF/stopwords/stopwords_en.txt");
        ConcurrentHashMap<String, String> stopwordsEN = new ConcurrentHashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println("line: " + line);
                if (!line.isEmpty() && Character.isLetter(line.charAt(0))) {
                    int index = line.indexOf(' ');

                    if (index == -1) {
                        index = line.length();
                    }

                    //System.out.println("index: " + index);
                    stopwordsEN.put(line.substring(0, index), "");
                }
            }
            englishStopwords = stopwordsEN;
            //englishProcessor.setStopwords(stopwordsEN);
        } catch (Exception e) {
            System.out.println(e);
        }

        is = servletContext.getResourceAsStream("/WEB-INF/stopwords/stopwords_pt.txt");
        ConcurrentHashMap<String, String> stopwordsPT = new ConcurrentHashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println("line: " + line);
                if (!line.isEmpty() && Character.isLetter(line.charAt(0))) {
                    int index = line.indexOf(' ');

                    if (index == -1) {
                        index = line.length();
                    }

                    //System.out.println("index: " + index);
                    stopwordsPT.put(line.substring(0, index), "");
                }
            }

            portugueseStopwords = stopwordsPT;
            //portugueseProcessor.setStopwords(stopwordsEN);
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            long startTime = System.nanoTime();
            String directory = servletContext.getRealPath("/") + "/WEB-INF/language-profiles/";
            DetectorFactory.loadProfile(directory);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000;
            System.out.println("LOAD PROFILES: " + duration + " ms");
        } catch (LangDetectException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        try{
            
            Locale localeEN = new Locale("en");
            Locale localePT = new Locale("pt");
            URL[] urls = {servletContext.getResource("/WEB-INF/i18n/")};
            ClassLoader loader = new URLClassLoader(urls);
            
            englishMessages = ResourceBundle.getBundle("MessagesBundle", localeEN, loader);
            portugueseMessages = ResourceBundle.getBundle("MessagesBundle", localePT, loader);
            
        }catch(Exception e){
            logger.log(Level.SEVERE, null, e);
        }
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
    public ProcessorResult test(ProcessorParams param) {
        
        System.out.println(param);
        if(param.supportedLanguages.isEmpty())
            param.setDefaultSupportedLanguages();
        
        
        System.out.println("Starting Processing");
        SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS");
        Date date = new Date();
        System.out.println(SDF.format(date));

        long startTimeX = System.nanoTime();
        String decompressed = LZString.decompressFromUTF16(param.body);
        long endTimeX = System.nanoTime();
        long durationX = (endTimeX - startTimeX) / 1000000;
        System.out.println("DURATION TO DECOMPRESS: " + durationX + " ms");
        //System.out.println("DECOMPRESSED: " + decompressed);

        long startTime = System.nanoTime();
        param.body = decompressed;
        ProcessorResult result = processDocumentV2(param);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        System.out.println("DURATION: " + duration + " ms");
        
        return result;
    }

    public ProcessorResult processDocumentV2(ProcessorParams param) {

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
        
        if(! param.supportedLanguages.contains(language))
            return new ProcessorResult("This language is not supported");
        
        switch (language) {
            case "en":
                processor = new EnglishProcessor(englishStopwords, englishTokenizer, semanticTypes);
                //messages = englishMessages;
                break;
            case "pt":
                processor = new PortugueseProcessor(portugueseStopwords, portugueseTokenizer, semanticTypes);
                //messages = portugueseMessages;
                break;
            default:
                processor = new EnglishProcessor(englishStopwords, englishTokenizer, semanticTypes);
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
            if (element.tagName().equals("script") /*|| element.tagName().equals("noscript")*/) {
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

                        Span spans[] = processor.tokenizer.tokenizePos(text);

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

                            /*
                             * with the current splitting method:
                             * even chunks are text nodes
                             * odd chunks are data nodes (new html created)
                             * this way, as we are replacing by a single datanode, we need to escape the text parts
                             */
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
        return result;
    }

    private String replaceConcept(Concept bestMatch, String language, ResourceBundle messages) {
        
        boolean hasDifferentCHV = bestMatch.CHVPreferred != null && ! bestMatch.string.equalsIgnoreCase(bestMatch.CHVPreferred);
        boolean hasDifferentUMLS = bestMatch.UMLSPreferred != null && ! bestMatch.string.equalsIgnoreCase(bestMatch.UMLSPreferred);
        String preferred = "";
        String definition = "";
        
        if(hasDifferentCHV && hasDifferentUMLS)
        {
            preferred = "<p>" + messages.getString("aka") + " \"" + bestMatch.CHVPreferred + "\" " + messages.getString("lay_terminology") + " " + messages.getString("or") + " \"" + " " + bestMatch.UMLSPreferred + "\" " + messages.getString("medical_terminology") + "</p>";
        }else if(hasDifferentCHV){
            preferred = "<p>" + messages.getString("aka") + " \"" + bestMatch.CHVPreferred + "\" " + messages.getString("lay_terminology") + "</p>";
        }else if(hasDifferentUMLS){
            preferred = "<p>" + messages.getString("aka") + " \"" + bestMatch.UMLSPreferred + "\" " + messages.getString("medical_terminology") + "</p>";
        }
        
        
            
        if(bestMatch.definition != null){
            definition = "<p class=\"definition\">" + bestMatch.definition + "</p>";
        }else{
            definition = "<p>" + messages.getString("sorry") + "<br>" +  messages.getString("no_definition") + "</p>"; 
        }
        String tooltip = preferred + definition + "<a href=\"#\" data-toggle=\"modal\" data-target=\"#health-translator-modal\">" + messages.getString("more_info") + "</a>";
        String newString = "<x-health-translator style='display:inline' class='health-translator'><x-health-translator class='medical-term-translate' data-toggle='tooltip' title='" + tooltip + "' data-html='true' data-lang=\"" + language + "\" data-cui=\"" + bestMatch.CUI + "\" data-term=\"" + bestMatch.string + "\">" + bestMatch.string + "</x-health-translator></x-health-translator>";

        return newString;
    }

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
    
    protected boolean acceptedSemanticType(String sty) {
        return semanticTypes.contains(sty);
    }
 }
