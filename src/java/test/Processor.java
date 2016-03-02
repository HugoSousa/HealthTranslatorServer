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
    private final HashSet<String> semanticTypes = new HashSet<>(Arrays.asList("T005", "T007", "T023", "T029", "T030", "T033", "T034", "T037", "T046", "T047", "T048", "T058", "T059", "T060", "T061", "T116", "T121", "T125", "T126", "T127", "T129", "T130", "T131", "T184", "T192", "T195", "T200"));
    
    private Matcher punctuationMatcher;
    private Matcher numberMatcher;

    /**
     * Creates a new instance of TestResource
     */
    public Processor() {
        System.out.println("Hello there :)");
        Pattern punctuationPattern = Pattern.compile("\\p{Punct}", Pattern.CASE_INSENSITIVE);
        Pattern numberPattern = Pattern.compile("\\\\d+", Pattern.CASE_INSENSITIVE);
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
     * load stopwords into memory
     */
    @PostConstruct
    public void loadStopwords() {
        InputStream is = servletContext.getResourceAsStream("/WEB-INF/stopwords/stopwords_en.txt");

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
        
        long startTime = System.nanoTime();
        //processDocument("<h1>Things hypertension here though<span style=\"color: red\">Hypertension</span> and abdomen achlorhydria</h1>");
        
        /*
        String result = processDocument(
                "<br><br>"
                + "<h1>Things hypertension here though <span style=\"color: red\">Hypertension</span> and abdomen achlorhydria</h1>"
                + "<p>heart failure</p>"
                + "<div>"
                + "The European languages are members of the same family. Their separate existence is a myth. For science, music, sport, etc, Europe uses the same vocabulary. The languages only differ in their grammar, their pronunciation and their most common words.\n"
                + "\n"
                + "Everyone realizes why a new common language would be desirable: one could refuse to pay expensive translators. To achieve this, it would be necessary to have uniform grammar, pronunciation and more common words. If several languages coalesce, the grammar of the resulting language is more simple and regular than that of the individual languages.\n"
                + "\n"
                + "The new common language will be more simple and regular than the existing European languages. It will be as simple as Occidental; in fact, it will be Occidental. To an English person, it will seem like simplified English, as a skeptical Cambridge friend of mine told me what Occidental is. The European languages are members of the same family.\n"
                + "\n"
                + "Their separate existence is a myth. For science, music, sport, etc, Europe uses the same vocabulary. The languages only differ in their grammar, their pronunciation and their most common words. Everyone realizes why a new common language would be desirable: one could refuse to pay expensive translators.\n"
                + "\n"
                + "To achieve this, it would be necessary to have uniform grammar, pronunciation and more common words. If several languages coalesce, the grammar of the resulting language is more simple and regular than that of the individual languages. The new common language will be more simple and regular than the existing European languages. It will be as simple as Occidental; in fact, it will be Occidental. To an English person, it will seem like simplified English, as a skeptical Cambridge friend of mine told me what Occidental is. The European languages are members of the same family. Their separate existence is a"
                + "\n"
                + "</div>"
        );
        */
        
        String result = processDocument(param.getBody());

        JSONObject obj = new JSONObject();
        obj.put("result", result);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        System.out.println("DURATION: " + duration + " ms");

        return Response.status(200).entity(obj.toString()).build();
    }

    public String processDocument(String content) {
        Document doc = Jsoup.parseBodyFragment(content);

        Elements elements = doc.body().children().select("*");
        InputStream is = servletContext.getResourceAsStream("/WEB-INF/models/en-token.bin");
        TokenizerModel model = null;
        try {
            model = new TokenizerModel(is);
            is.close();
            
        } catch (IOException ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
        }
        //this can be even done on server startup, like stopwords, for better performance
        Tokenizer tokenizer = new TokenizerME(model);
        
        for (Element element : elements) {
            
            if(element.tagName().equals("script"))
                continue;
            
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
                            
                            if(token.length() <= 2)
                                continue;
                            
                            if (stopwordsEN.containsKey(queryToken)) {
                                continue;
                            }
                            
                            punctuationMatcher.reset(queryToken);
                            if ( punctuationMatcher.matches() ) {
                                continue;
                            }
                            
                            numberMatcher.reset(queryToken);
                            if(numberMatcher.matches())
                                continue;
                            

                            /*enStemmer.setCurrent(token);
                             String queryToken = null;
                             if (enStemmer.stem()) {
                             queryToken = enStemmer.getCurrent();
                             } else {
                             queryToken = token;
                             }*/
                                
                            Connection conn = ServletContextClass.conn;
                            Connection connMySQL = ServletContextClass.conn_MySQL;
                            PreparedStatement stmt;

                                //stmt = conn.prepareStatement("SELECT * FROM chvstring WHERE en LIKE ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                                //stmt.setString(1, queryToken + "%");
                                long startTime = System.nanoTime();
                                stmt = connMySQL.prepareStatement("SELECT * FROM MRCONSO mrc WHERE STR LIKE ?;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                                //stmt.setString(1, queryToken);
                                stmt.setString(1, queryToken + "%");
                                
                                ResultSet rs = stmt.executeQuery();
                                long endTime = System.nanoTime();
                                long duration = (endTime - startTime) / 1000000;
                                System.out.println("DURATION: " + duration + " ms for token " + queryToken);
                                
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
                                        if(rs.getString("STR").toLowerCase().equals(finalStringLowercase)){
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
                                            if(resultLowerCase.equals(finalStringLowercase)){
                                                //bestMatch = rs.getRow();
                                                //bestMatchString = finalString;
                                                bestMatch = new Concept(finalString, nextSpan, rs.getRow());
                                                i++;
                                                //lastFound = nextSpan;
                                                if (keepCycle) {
                                                    break;
                                                }
                                            //} else if (rs.getString("en").startsWith(finalStringLowercase)) {
                                            }else if(resultLowerCase.startsWith(finalStringLowercase)){
                                                keepCycle = true;
                                            }
                                        }
                                    } while (keepCycle);

                                    if (bestMatch != null) {
                                        
                                        if(bestMatch.string.equals("hypertension")){
                                            int n = 1;
                                        }
                                        rs.absolute(bestMatch.row);
                                        //get TUI from mrsty and check if accepted semantic type
                                        String CUI = rs.getString("CUI");
                                        stmt = connMySQL.prepareStatement("SELECT * FROM MRSTY mrc WHERE CUI = ?;");
                                        stmt.setString(1, CUI);
                                        rs = stmt.executeQuery();
                                        rs.next();
                                        
                                        if(acceptedSemanticType(rs.getString("TUI"))){
                                            
                                            if(bestMatch.string.equals("Secondary")){
                                                int a = 2;
                                            }
                                            //if (firstFound) { //lastFound == null ?
                                            if(lastFound == null){
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
                                    if(result.equals(queryToken)){
                                        //finalString = token;
                                        bestMatch = new Concept(finalString, span, rs.getRow());
                                    }
                                    else{
                                        int wordCount = result.split(" ").length;
                                        
                                        if(i + wordCount - 1 < spans.length){
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
                                    if(rs.getString("STR").equals(finalString)){
                                        //System.out.println("FOUND TERM: ID - " + rs.getString("id"));
                                        bestMatch = new Concept(finalString, span, rs.getRow());
                                        //if (firstFound) {
                                        if(lastFound == null){
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
            } finally {}
        }

        //System.out.println("DOCUMENT: " + doc);
        return doc.body().toString();
    }
    
    private boolean acceptedSemanticType(String sty){
        return semanticTypes.contains(sty);
    }
}
