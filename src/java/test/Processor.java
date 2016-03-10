/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

    private final EnglishProcessor englishProcessor = new EnglishProcessor();
    private final PortugueseProcessor portugueseProcessor = new PortugueseProcessor();
        
    /**
     * Creates a new instance of Processor
     */
    public Processor() {
        System.out.println("Processor constructor");
        /*
        Pattern punctuationPattern = Pattern.compile("\\p{Punct}", Pattern.CASE_INSENSITIVE);
        Pattern numberPattern = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
        punctuationMatcher = punctuationPattern.matcher("");
        numberMatcher = numberPattern.matcher("");
        */
        
        englishProcessor.setSemanticTypes(semanticTypes);
        portugueseProcessor.setSemanticTypes(semanticTypes);
        //conn = connectToDatabaseOrDie();
    }
    /*
     private Connection connectToDatabaseOrDie() {
     Connection conn = null;
     try {
     Class.forName("org.postgresql.Driver");
     String url = "jdbc:postgresql://localhost/HealthTranslator";
     conn = DriverManager.getConnection(url, "hugo", "healthtranslator");
     } catch (ClassNotFoundException e) {
     e.printStackTrace();
     System.exit(1);
     } catch (SQLException e) {
     e.printStackTrace();
     System.exit(2);
     }
     System.out.println("RETURNING CONN");
     System.out.println("conn: " + conn);
     return conn;
     }
     */

    /*
     * load stopwords and models into memory
     */
    @PostConstruct
    public void preLoad() {

        System.out.println("Processor preload");
        InputStream is = null;
        try {
            is = servletContext.getResourceAsStream("/WEB-INF/models/en-token.bin");
            TokenizerModel modelEN = new TokenizerModel(is);
            Tokenizer tokenizerEN = new TokenizerME(modelEN);
            englishProcessor.setTokenizer(tokenizerEN);
            is.close();
            
            is = servletContext.getResourceAsStream("/WEB-INF/models/pt-token.bin");
            TokenizerModel modelPT = new TokenizerModel(is);
            Tokenizer tokenizerPT = new TokenizerME(modelPT);
            portugueseProcessor.setTokenizer(tokenizerPT);
            is.close();
        } catch (IOException ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
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
            
            englishProcessor.setStopwords(stopwordsEN);
            //set stopwords pt
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
            
            portugueseProcessor.setStopwords(stopwordsEN);
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
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
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
    public ProcessResult test(BodyMessage param) {
        System.out.println("Starting Processing");

        long startTime = System.nanoTime();
        ProcessResult result = processDocumentV2(param.getBody());
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        System.out.println("DURATION: " + duration + " ms");

        return result;
    }

    public ProcessResult processDocumentV2(String content) {
        
        System.out.println("CONTENT: " + content);
        long startTime = System.nanoTime();
        int conceptCounter = 0;

        Document doc = Jsoup.parseBodyFragment(content);      
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
        
        Elements elements = doc.body().children().select("*");

        String language = detectLanguage(elements);
        TokenProcessor processor = null;
        switch(language){
            case "en":
                processor = englishProcessor;
                break;
            case "pt":
                processor = portugueseProcessor;
                break;
            default:
                processor = englishProcessor;
                break;
        }

        for (Element element : elements) {

            //ignore scripts
            if (element.tagName().equals("script") /*|| element.tagName().equals("noscript")*/) {
                element.remove();
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

                        for (int i = 0; i < spans.length; i++) {
                            
                            Span initialSpan = spans[i];
                            Concept bestMatch = processor.processToken(spans, i, text, FORWARD_THRESHOLD);
                            
                            if (bestMatch != null) {
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

                                String replace = replaceConcept(bestMatch);
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
                            for(int i = 0; i < splitText.size(); i++){
                                String chunk = splitText.get(i);
                                if(i%2 == 0){
                                    sb.append(escapeHtml4(chunk));
                                }else{
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
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, e);
            }
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);// / 1000000;
        System.out.println("BODY: " + doc.body().toString());
        ProcessResult result = new ProcessResult(doc.body().toString(), conceptCounter, duration);
        return result;
    }

    private String replaceConcept(Concept bestMatch) {
        String newString = "<span style='display:inline' class='health-translator'><span class='medical-term-translate' data-toggle='tooltip' title='<p> CHV PREFERRED: " + bestMatch.CHVPreferred + "</p> <a href=\"#\" data-toggle=\"modal\" data-target=\"#myModal\">click here for more information</a>' data-html='true' data-term=\"" + bestMatch.string + "\">" + bestMatch.string + "</span></span>";
        
        return newString;
    }

    private String detectLanguage(Elements elements) {
        //do a first iteration over elements (text) to detect language
        boolean successfulDetection;
        do {

            Detector detector = null;
            try {
                detector = DetectorFactory.create();
            } catch (LangDetectException ex) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
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
                    Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } while (!successfulDetection);

        return "en";
    }

    protected boolean acceptedSemanticType(String sty) {
        return semanticTypes.contains(sty);
    }
}
