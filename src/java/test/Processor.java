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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.jactiveresource.Inflector;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
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
    private final HashSet<String> semanticTypes = new HashSet<>(Arrays.asList("T005", "T007", "T023", "T029", "T030", "T033", "T034", "T037", "T046", "T047", "T048", "T059", "T060", "T061", "T116", "T121", "T125", "T126", "T127", "T129", "T130", "T131", "T184", "T192", "T195", "T200"));

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
        //System.out.println("cenas: " + param.getBody());
        /*
         long startTime = System.nanoTime();
         String result = processDocumentV1(param.getBody());
         long endTime = System.nanoTime();
         long duration = (endTime - startTime) / 1000000;
         System.out.println("V1 - DURATION: " + duration + " ms");
         */
        long startTime = System.nanoTime();
        ProcessResult result = processDocumentV2(param.getBody());
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        System.out.println("DURATION: " + duration + " ms");
        /*
         JSONObject obj = new JSONObject();
         obj.put("result", result);
         //long endTime = System.nanoTime();
         //long duration = (endTime - startTime) / 1000000;
         //System.out.println("DURATION: " + duration + " ms");

         return Response.status(200).entity(obj.toString()).build();
         */
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
                //continue;
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
                        //System.out.println("TEXT: " + text);
                        if(text.contains("$")){
                            System.out.println("HERE");
                            if(text.startsWith("\n"))
                                System.out.println("HERE 2");
                        }

                        Span spans[] = processor.tokenizer.tokenizePos(text);

                        ArrayList<String> splitText = new ArrayList<>();
                        //boolean firstFound = true;
                        Concept lastFound = null;
                        //EnglishStemmer enStemmer = new EnglishStemmer();

                        for (int i = 0; i < spans.length; i++) {
                            //for (Span span : spans) {
                            /*
                            String[] tokens = new String[FORWARD_THRESHOLD];
                            Span initialSpan = spans[i];
                            Concept bestMatch = null;

                            int initialIndex = i;

                            for (int j = 0; j < FORWARD_THRESHOLD; j++) {

                                if (initialIndex + j >= spans.length) {
                                    break;
                                }

                                Span span = spans[initialIndex + j];
                                String token = text.substring(span.getStart(), span.getEnd());
                                tokens[j] = token;

                                punctuationMatcher.reset(token);
                                numberMatcher.reset(token);
                                if (j == 0 && (token.length() <= 2 || stopwordsEN.containsKey(token))) {
                                    break;
                                } else if (punctuationMatcher.matches() || numberMatcher.matches()) {
                                    break;
                                }

                                String finalToken = "";
                                if (j == 0) {
                                    finalToken = token;
                                } else if (j > 0) {
                                    for (int k = 0; k <= j; k++) {
                                        finalToken += tokens[k];
                                        if (k < j) {
                                            finalToken += " ";
                                        }
                                    }
                                }

                                String queryToken = finalToken.toLowerCase();
                                String originalString = finalToken;
                                String singularQueryToken = Inflector.singularize(queryToken, "en");

                                Connection connMySQL = ServletContextClass.conn_MySQL;
                                PreparedStatement stmt;

                                //long startTime = System.nanoTime();
                                stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO mrc WHERE STR = ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                                stmt.setString(1, singularQueryToken);

                                ResultSet rs = stmt.executeQuery();
                                //long endTime = System.nanoTime();
                                //long duration = (endTime - startTime) / 1000000;
                                //System.out.println("DURATION: " + duration + " ms for token " + queryToken);

                                rs.last();
                                int total = rs.getRow();
                                rs.beforeFirst();
                                //rs.next();

                                if (total >= 1) {
                                    //iterate result set, check if it's CHV preferred or synonym
                                    //if they map to different CUIs, check the one with TTY = PT

                                    String CUI = null;
                                    String CHVPreferred = null;
                                    while (rs.next()) {
                                        if (rs.getRow() == 1) {
                                            //assign the first result at least, so it's not null
                                            CUI = rs.getString("CUI");
                                        } else {
                                            if (rs.getString("CUI") != CUI && rs.getString("TTY").equals("PT")) {
                                                CUI = rs.getString("CUI");
                                            }
                                        }

                                        if (rs.getString("TTY").equals("PT") && rs.getString("SAB").equals("CHV")) {
                                            CHVPreferred = singularQueryToken;
                                        }
                                    }

                                    stmt = connMySQL.prepareStatement("SELECT * FROM MRSTY WHERE CUI = ?;");
                                    stmt.setString(1, CUI);
                                    rs = stmt.executeQuery();
                                    rs.next();

                                    if (acceptedSemanticType(rs.getString("TUI"))) {
                                        i += j;
                                        bestMatch = new Concept(originalString, new Span(initialSpan.getStart(), span.getEnd()), rs.getRow());
                                        bestMatch.CUI = CUI;

                                        if (CHVPreferred == null) {
                                            //check the CHV preferred term for this CUI
                                            //assign to CHVPreferred variable

                                            stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO WHERE CUI = ? AND SAB = 'CHV' AND TTY = 'PT';");
                                            stmt.setString(1, CUI);
                                            rs = stmt.executeQuery();
                                            if (rs.next()) {
                                                CHVPreferred = rs.getString("STR");
                                            } else {
                                                //the concept may not be in CHV
                                                System.out.println("The concept " + CUI + " (" + singularQueryToken + ") is not in CHV.");
                                            }
                                        }

                                        bestMatch.CHVPreferred = CHVPreferred;

                                    }
                                }

                                stmt.close();
                                rs.close();
                            }
                            */
                            Span initialSpan = spans[i];
                            Concept bestMatch = processor.processToken(spans, i, text, FORWARD_THRESHOLD);
                            
                            if (bestMatch != null) {
                                //how to know how many i to increment?
                                i += bestMatch.words - 1;
                                conceptCounter++;

                                if (lastFound == null) {
                                    splitText.add(text.substring(0, initialSpan.getStart()));
                                    //firstFound = false;
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

    private boolean acceptedSemanticType(String sty) {
        return semanticTypes.contains(sty);
    }
    
    /*
    private Concept processToken(Span[] spans, int i, String text) throws SQLException {
        
        String[] tokens = new String[FORWARD_THRESHOLD];
        Span initialSpan = spans[i];
        Concept bestMatch = null;

        int initialIndex = i;
        
        for (int j = 0; j < FORWARD_THRESHOLD; j++) {

            if (initialIndex + j >= spans.length) {
                break;
            }

            Span span = spans[initialIndex + j];
            String token = text.substring(span.getStart(), span.getEnd());
            tokens[j] = token;

            punctuationMatcher.reset(token);
            numberMatcher.reset(token);
            if (j == 0 && (token.length() <= 2 || englishProcessor.stopwords.containsKey(token))) {
                break;
            } else if (punctuationMatcher.matches() || numberMatcher.matches()) {
                break;
            }

            String finalToken = "";
            if (j == 0) {
                finalToken = token;
            } else if (j > 0) {
                for (int k = 0; k <= j; k++) {
                    finalToken += tokens[k];
                    if (k < j) {
                        finalToken += " ";
                    }
                }
            }

            String queryToken = finalToken.toLowerCase();
            String originalString = finalToken;
            String singularQueryToken = null;
            try {
                singularQueryToken = Inflector.singularize(queryToken, "en");
            } catch (Exception ex) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
            }

            Connection connMySQL = ServletContextClass.conn_MySQL;
            PreparedStatement stmt;

            //long startTime = System.nanoTime();
            stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO mrc WHERE STR = ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1, singularQueryToken);

            ResultSet rs = stmt.executeQuery();
            //long endTime = System.nanoTime();
            //long duration = (endTime - startTime) / 1000000;
            //System.out.println("DURATION: " + duration + " ms for token " + queryToken);

            rs.last();
            int total = rs.getRow();
            rs.beforeFirst();
            //rs.next();

            if (total >= 1) {
                                    //iterate result set, check if it's CHV preferred or synonym
                //if they map to different CUIs, check the one with TTY = PT

                String CUI = null;
                String CHVPreferred = null;
                while (rs.next()) {
                    if (rs.getRow() == 1) {
                        //assign the first result at least, so it's not null
                        CUI = rs.getString("CUI");
                    } else {
                        if (rs.getString("CUI") != CUI && rs.getString("TTY").equals("PT")) {
                            CUI = rs.getString("CUI");
                        }
                    }

                    if (rs.getString("TTY").equals("PT") && rs.getString("SAB").equals("CHV")) {
                        CHVPreferred = singularQueryToken;
                    }
                }

                stmt = connMySQL.prepareStatement("SELECT * FROM MRSTY WHERE CUI = ?;");
                stmt.setString(1, CUI);
                rs = stmt.executeQuery();
                rs.next();

                if (acceptedSemanticType(rs.getString("TUI"))) {
                    i += j;
                    bestMatch = new Concept(originalString, new Span(initialSpan.getStart(), span.getEnd()), rs.getRow());
                    bestMatch.CUI = CUI;

                    if (CHVPreferred == null) {
                                            //check the CHV preferred term for this CUI
                        //assign to CHVPreferred variable

                        stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO WHERE CUI = ? AND SAB = 'CHV' AND TTY = 'PT';");
                        stmt.setString(1, CUI);
                        rs = stmt.executeQuery();
                        if (rs.next()) {
                            CHVPreferred = rs.getString("STR");
                        } else {
                            //the concept may not be in CHV
                            System.out.println("The concept " + CUI + " (" + singularQueryToken + ") is not in CHV.");
                        }
                    }

                    bestMatch.CHVPreferred = CHVPreferred;

                }
            }

            stmt.close();
            rs.close();
        }
        
        return bestMatch;
    }
    */
}
