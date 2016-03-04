/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

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
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.jactiveresource.Inflector;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * REST Web Service
 *
 * @author Hugo
 */
@Path("process")
@Singleton
public class Processor {

    @Context
    private UriInfo context;

    @Context
    ServletContext servletContext;

    private final ConcurrentHashMap<String, String> stopwordsEN = new ConcurrentHashMap<>();
    private final HashSet<String> semanticTypes = new HashSet<>(Arrays.asList("T005", "T007", "T023", "T029", "T030", "T033", "T034", "T037", "T046", "T047", "T048", "T059", "T060", "T061", "T116", "T121", "T125", "T126", "T127", "T129", "T130", "T131", "T184", "T192", "T195", "T200"));

    private Matcher punctuationMatcher;
    private Matcher numberMatcher;

    private TokenizerModel modelEN;

    private final int FORWARD_THRESHOLD = 5;

    /**
     * Creates a new instance of TestResource
     */
    public Processor() {
        System.out.println("Hello there :)");
        Pattern punctuationPattern = Pattern.compile("\\p{Punct}", Pattern.CASE_INSENSITIVE);
        Pattern numberPattern = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
        punctuationMatcher = punctuationPattern.matcher("");
        numberMatcher = numberPattern.matcher("");
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

        InputStream is = servletContext.getResourceAsStream("/WEB-INF/models/en-token.bin");
        TokenizerModel model = null;
        try {
            modelEN = new TokenizerModel(is);
            is.close();
        } catch (IOException ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
        }

        is = servletContext.getResourceAsStream("/WEB-INF/stopwords/stopwords_en.txt");

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
        } catch (Exception e) {
            System.out.println(e);
        }

        //System.out.println("STOPWORDS: " + stopwordsEN);
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
    public Response test(BodyMessage param) {

        //System.out.println("cenas: " + param.getBody());
        /*
         long startTime = System.nanoTime();
         String result = processDocumentV1(param.getBody());
         long endTime = System.nanoTime();
         long duration = (endTime - startTime) / 1000000;
         System.out.println("V1 - DURATION: " + duration + " ms");
         */
        long startTime = System.nanoTime();
        String result = processDocumentV2(param.getBody());
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        System.out.println("V2 - DURATION: " + duration + " ms");

        JSONObject obj = new JSONObject();
        obj.put("result", result);
        //long endTime = System.nanoTime();
        //long duration = (endTime - startTime) / 1000000;
        //System.out.println("DURATION: " + duration + " ms");

        return Response.status(200).entity(obj.toString()).build();
    }

    public String processDocumentV1(String content) {
        Document doc = Jsoup.parseBodyFragment(content);

        Elements elements = doc.body().children().select("*");

        //this can be even done on server startup, like stopwords, for better performance
        Tokenizer tokenizer = new TokenizerME(modelEN);

        for (Element element : elements) {

            if (element.tagName().equals("script")) {
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
                        String text = ((TextNode) node).text();
                        //System.out.println("TEXT: " + text);

                        Span spans[] = tokenizer.tokenizePos(text);

                        ArrayList<String> splitText = new ArrayList<>();
                        //boolean firstFound = true;
                        Concept lastFound = null;
                        //EnglishStemmer enStemmer = new EnglishStemmer();

                        for (int i = 0; i < spans.length; i++) {
                            //for (Span span : spans) {
                            Span span = spans[i];
                            String token = text.substring(span.getStart(), span.getEnd());
                            //System.out.println("token:" + token);

                            String queryToken = token.toLowerCase();
                            String originalString = token;

                            //System.out.println("TOKEN: " + queryToken);
                            if (token.length() <= 2) {
                                //System.out.println("Less than 2");
                                continue;
                            }

                            if (stopwordsEN.containsKey(queryToken)) {
                                //System.out.println("stopword: " + queryToken);
                                continue;
                            }

                            punctuationMatcher.reset(queryToken);
                            if (punctuationMatcher.matches()) {
                                //System.out.println("punctuation: " + queryToken);
                                continue;
                            }

                            numberMatcher.reset(queryToken);
                            if (numberMatcher.matches()) {
                                //System.out.println("number: " + queryToken);
                                continue;
                            }

                            /*enStemmer.setCurrent(token);
                             String queryToken = null;
                             if (enStemmer.stem()) {
                             queryToken = enStemmer.getCurrent();
                             } else {
                             queryToken = token;
                             }*/
                            //Connection conn = ServletContextClass.conn;
                            Connection connMySQL = ServletContextClass.conn_MySQL;
                            PreparedStatement stmt;

                            //stmt = conn.prepareStatement("SELECT * FROM chvstring WHERE en LIKE ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                            //stmt.setString(1, queryToken + "%");
                            long startTime = System.nanoTime();
                            stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO WHERE STR = ? OR STR LIKE ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                            stmt.setString(1, queryToken);
                            stmt.setString(2, queryToken + " %");

                            ResultSet rs = stmt.executeQuery();
                            long endTime = System.nanoTime();
                            long duration = (endTime - startTime) / 1000000;
                            //System.out.println("DURATION: " + duration + " ms for token " + queryToken);

                            rs.last();
                            int total = rs.getRow();
                            rs.beforeFirst();

                            Concept bestMatch = null;
                            String finalString = originalString;
                            String finalStringLowercase = queryToken.toLowerCase();

                            if (total > 1) {
                                /*
                                 //SOLUTION A
                                 //Get the list of counts
                                 //Starting with the biggest counts, get the forward terms and check for a exact match 
                                 //System.out.println("more than 1");
                                 */

                                //SOLUTION B
                                //keep adding a token until no more results are returned and check for exact-matching
                                //this solution should execute less processing than the previous one
                                //check if there is a exact match. If there is save in a variable
                                //String bestMatchString = null;
                                //int bestMatch = -1;
                                while (rs.next()) {
                                    if (rs.getString("STR").toLowerCase().equals(finalStringLowercase)) {
                                        //if (rs.getInt("en_count") == 1 && rs.getString("en").equals(finalStringLowercase)) {
                                        //bestMatchString = finalString;
                                        bestMatch = new Concept(originalString, span, rs.getRow());
                                        //bestMatch = rs.getRow();
                                        //lastFound = span;
                                        //it breaks after the first match
                                        break;
                                    }
                                }

                                boolean keepCycle;
                                int nextIndex = i;
                                do {
                                    //add a token, check if has better results
                                    //if no more results, stop and check if there is a bestMatch

                                    keepCycle = false;

                                    nextIndex++;
                                    if (nextIndex > spans.length - 1) {
                                        break;
                                    }

                                    Span nextSpan = spans[nextIndex];
                                    String nextToken = text.substring(nextSpan.getStart(), nextSpan.getEnd());

                                    finalString += " ";
                                    finalString += nextToken;
                                    finalStringLowercase = finalString.toLowerCase();

                                    rs.beforeFirst();
                                    while (rs.next()) {

                                        //if (rs.getString("en").equals(finalStringLowercase)) {
                                        String resultLowerCase = rs.getString("STR").toLowerCase();
                                        if (resultLowerCase.equals(finalStringLowercase)) {
                                            //bestMatch = rs.getRow();
                                            //bestMatchString = finalString;
                                            bestMatch = new Concept(finalString, nextSpan, rs.getRow());
                                            i++;
                                            //lastFound = nextSpan;
                                            if (keepCycle) {
                                                break;
                                            }
                                            //} else if (rs.getString("en").startsWith(finalStringLowercase)) {
                                        } else if (resultLowerCase.startsWith(finalStringLowercase)) {
                                            keepCycle = true;
                                        }
                                    }
                                } while (keepCycle);

                                if (bestMatch != null) {

                                    if (bestMatch.string.equals("hypertension")) {
                                        int n = 1;
                                    }
                                    rs.absolute(bestMatch.row);
                                    //get TUI from mrsty and check if accepted semantic type
                                    String CUI = rs.getString("CUI");
                                    stmt = connMySQL.prepareStatement("SELECT * FROM MRSTY mrc WHERE CUI = ?;");
                                    stmt.setString(1, CUI);
                                    rs = stmt.executeQuery();
                                    rs.next();

                                    if (acceptedSemanticType(rs.getString("TUI"))) {

                                        if (bestMatch.string.equals("Secondary")) {
                                            int a = 2;
                                        }
                                        //if (firstFound) { //lastFound == null ?
                                        if (lastFound == null) {
                                            splitText.add(text.substring(0, span.getStart()));
                                            //firstFound = false;
                                        } else {
                                            splitText.add(text.substring(lastFound.span.getEnd(), span.getStart()));
                                        }

                                        String newString = "<span style='display:inline' class='health-translator'><span class='medical-term-translate' data-toggle='tooltip' data-placement='right' title='<a href=\"#\" data-toggle=\"modal\" data-target=\"#myModal\">click here for more information</a>' data-html='true' data-term=\"" + bestMatch.string + "\">" + bestMatch.string + "</span></span>";

                                        splitText.add(newString);
                                        lastFound = bestMatch;

                                    }
                                }
                            } else if (total == 1) {

                                //if the first token is not a match already, stop the forward processing?
                                //need to get the count first and check the forward tokens
                                //System.out.println("just 1 result");
                                rs.next();
                                //int count = rs.getInt("en_count");
                                //String finalString = null;
                                String result = rs.getString("STR");
                                //if (count == 1) {
                                if (result.equals(queryToken)) {
                                    //finalString = token;
                                    bestMatch = new Concept(finalString, span, rs.getRow());
                                } else {
                                    int wordCount = result.split(" ").length;

                                    if (i + wordCount - 1 < spans.length) {
                                        StringBuilder sb = new StringBuilder();
                                        sb.append(token);

                                        int nextIndex = i + 1;
                                        while (nextIndex < i + wordCount) {
                                            Span nextSpan = spans[nextIndex];
                                            String nextToken = text.substring(nextSpan.getStart(), nextSpan.getEnd());
                                            sb.append(" ");
                                            sb.append(nextToken);
                                            nextIndex++;
                                        }
                                        finalString = sb.toString();
                                    }
                                }

                                //originalString = finalString;
                                //finalString = finalString.toLowerCase();
                                //if (rs.getString("en").equals(finalString)) {
                                if (rs.getString("STR").equals(finalString)) {
                                    //System.out.println("FOUND TERM: ID - " + rs.getString("id"));
                                    bestMatch = new Concept(finalString, span, rs.getRow());
                                    //if (firstFound) {
                                    if (lastFound == null) {
                                        splitText.add(text.substring(0, span.getStart()));
                                        //firstFound = false;
                                    } else {
                                        splitText.add(text.substring(lastFound.span.getEnd(), span.getStart()));
                                    }
                                    String newString = "<span style='display:inline' class='health-translator'><span class='medical-term-translate' data-toggle='tooltip' data-placement='right' title='<a href=\"#\" data-toggle=\"modal\" data-target=\"#myModal\">click here for more information</a>' data-html='true' data-term=\"" + bestMatch.string + "\">" + bestMatch.string + "</span></span>";

                                    splitText.add(newString);
                                    lastFound = bestMatch;

                                }
                            }

                            //se retornar termos
                            bestMatch = null;
                            rs.close();
                            stmt.close();

                        }

                        if (lastFound != null) {
                            splitText.add(text.substring(lastFound.span.getEnd()));
                        }

                        if (splitText.size() > 0) {
                            //replace textnode by text-element-text

                            StringBuilder sb = new StringBuilder(text.length());
                            for (String chunk : splitText) {
                                sb.append(chunk);
                            }

                            DataNode dn = new DataNode(sb.toString(), "");
                            node.replaceWith(dn);

                            //System.out.println("node: " + node);
                            //System.out.println("new node: " + dn);
                        }

                        splitText = null;

                    } else {
                        //System.out.println("NODE: NOT TEXT");
                    }
                }

            } catch (SQLException ex) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception e) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, e);
            } finally {
            }
        }

        //System.out.println("DOCUMENT: " + doc);
        return doc.body().toString();
    }

    public String processDocumentV2(String content) {
        Document doc = Jsoup.parseBodyFragment(content);

        Elements elements = doc.body().children().select("*");

        //this can be even done on server startup, like stopwords, for better performance
        Tokenizer tokenizer = new TokenizerME(modelEN);

        for (Element element : elements) {

            if (element.tagName().equals("script")) {
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
                        String text = ((TextNode) node).text();
                        //System.out.println("TEXT: " + text);

                        Span spans[] = tokenizer.tokenizePos(text);

                        ArrayList<String> splitText = new ArrayList<>();
                        //boolean firstFound = true;
                        Concept lastFound = null;
                        //EnglishStemmer enStemmer = new EnglishStemmer();

                        for (int i = 0; i < spans.length; i++) {
                            //for (Span span : spans) {

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

                                //String singularToken = Inflector.singularize("eyes", "en"); //TODO TOMORROW use this is the query 
                                //System.out.println("TOKEN: " + token + " / SINGULAR: " + singularToken);
                                tokens[j] = token;

                                if (j == 0) {
                                    if (token.length() <= 2) {
                                        //System.out.println("Less than 2");
                                        break;
                                    }

                                    if (stopwordsEN.containsKey(token)) {
                                        //System.out.println("stopword: " + token);
                                        break;
                                    }
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

                                //ignore only if is first token
                                /*
                                 System.out.println("TOKEN: " + queryToken);
                                 if (token.length() <= 2) {
                                 System.out.println("Less than 2");
                                 break;
                                 }
                                

                                 if (stopwordsEN.containsKey(queryToken)) {
                                 System.out.println("stopword: " + queryToken);
                                 break;
                                 }
                                 */
                                punctuationMatcher.reset(queryToken);
                                if (punctuationMatcher.matches()) {
                                    //System.out.println("punctuation: " + queryToken);
                                    break;
                                }

                                numberMatcher.reset(queryToken);
                                if (numberMatcher.matches()) {
                                    //System.out.println("number: " + queryToken);
                                    break;
                                }

                                //Connection conn = ServletContextClass.conn;
                                Connection connMySQL = ServletContextClass.conn_MySQL;
                                PreparedStatement stmt;

                                //stmt = conn.prepareStatement("SELECT * FROM chvstring WHERE en LIKE ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                                //stmt.setString(1, queryToken + "%");
                                long startTime = System.nanoTime();
                                stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO mrc WHERE STR = ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                                stmt.setString(1, singularQueryToken);

                                ResultSet rs = stmt.executeQuery();
                                long endTime = System.nanoTime();
                                long duration = (endTime - startTime) / 1000000;
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
                                        if(rs.getRow() == 1){
                                            //assign the first result at least, so it's not null
                                            CUI = rs.getString("CUI");
                                        }else{
                                            if (rs.getString("CUI") != CUI && rs.getString("TTY").equals("PT")) {
                                                CUI = rs.getString("CUI");
                                            }
                                        }
                                        
                                        if (rs.getString("TTY").equals("PT") && rs.getString("SAB").equals("CHV")) {
                                            CHVPreferred = singularQueryToken;
                                        }
                                    }

                                    //String CUI = rs.getString("CUI");
                                    if(CUI == null){
                                        System.out.println("Okay, this shouldn't happen...!");
                                    }
                                    stmt = connMySQL.prepareStatement("SELECT * FROM MRSTY mrc WHERE CUI = ?;");
                                    stmt.setString(1, CUI);
                                    rs = stmt.executeQuery();
                                    rs.next();

                                    if (acceptedSemanticType(rs.getString("TUI"))) {
                                        i += j;
                                        bestMatch = new Concept(originalString, new Span(initialSpan.getStart(), span.getEnd()), rs.getRow());
                                        bestMatch.CUI = CUI;
                                        
                                        if( CHVPreferred == null ){
                                            //check the CHV preferred term for this CUI
                                            //assign to CHVPreferred variable
                                        }
                                        
                                        bestMatch.CHVPreferred = CHVPreferred;
                                        
                                    }
                                }

                                stmt.close();
                                rs.close();
                            }

                            if (bestMatch != null) {
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
                                String newString = "<span style='display:inline' class='health-translator'><span class='medical-term-translate' data-toggle='tooltip' data-placement='right' title='<a href=\"#\" data-toggle=\"modal\" data-target=\"#myModal\">" + bestMatch.CHVPreferred + "click here for more information</a>' data-html='true' data-term=\"" + bestMatch.string + "\">" + bestMatch.string + "</span></span>";

                                splitText.add(newString);
                                lastFound = bestMatch;
                            }

                            //System.out.println("token:" + token);
                            /*enStemmer.setCurrent(token);
                             String queryToken = null;
                             if (enStemmer.stem()) {
                             queryToken = enStemmer.getCurrent();
                             } else {
                             queryToken = token;
                             }*/
                        }

                        if (lastFound != null) {
                            splitText.add(text.substring(lastFound.span.getEnd()));
                        }

                        if (splitText.size() > 0) {
                            //replace textnode by text-element-text

                            StringBuilder sb = new StringBuilder(text.length());
                            for (String chunk : splitText) {
                                sb.append(chunk);
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

            } catch (SQLException ex) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception e) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, e);
            } finally {
            }
        }

        //System.out.println("DOCUMENT: " + doc);
        return doc.body().toString();
    }

    private boolean acceptedSemanticType(String sty) {
        return semanticTypes.contains(sty);
    }
}
