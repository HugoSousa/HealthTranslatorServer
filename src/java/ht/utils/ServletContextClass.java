/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.utils;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

/**
 *
 * @author Hugo
 */
public class ServletContextClass implements ServletContextListener{
    //public static Connection conn;
    
    //public static Connection conn_MySQL;
    
    private static Logger logger;

    @Override
    public void contextInitialized(ServletContextEvent sce) 
    {
        System.out.println("CONTEXT INITIALIZED HERE!");
        
        ServletContext context = sce.getServletContext();
           
        logger = LoggerFactory.createLogger(ServletContextClass.class.getName());

        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (javax.sql.DataSource)ctx.lookup("jdbc/MySQL");
            context.setAttribute("connPool", ds);
        } catch (NamingException ex) {
            Logger.getLogger(ServletContextClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /*
        try {
            
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://localhost/umls_en";
            //conn_MySQL = DriverManager.getConnection(url, "root", "");
            Connection conn = DriverManager.getConnection(url, "root", "");
            context.setAttribute("connectionDB", conn);

        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, null, e);
            System.exit(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            System.exit(2);
        }
        */
        
        InputStream is;
        try {
            is = sce.getServletContext().getResourceAsStream("/WEB-INF/models/en-token.bin");
            TokenizerModel modelEN = new TokenizerModel(is);
            context.setAttribute("englishTokenizer", new TokenizerME(modelEN));
            //englishProcessor.setTokenizer(tokenizerEN);
            is.close();

            is = sce.getServletContext().getResourceAsStream("/WEB-INF/models/pt-token.bin");
            TokenizerModel modelPT = new TokenizerModel(is);
            context.setAttribute("portugueseTokenizer", new TokenizerME(modelPT));
            //portugueseProcessor.setTokenizer(tokenizerPT);
            is.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        is = sce.getServletContext().getResourceAsStream("/WEB-INF/stopwords/stopwords_en.txt");
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
            context.setAttribute("englishStopwords", stopwordsEN);
            //englishProcessor.setStopwords(stopwordsEN);
        } catch (Exception e) {
            System.out.println(e);
        }

        is = sce.getServletContext().getResourceAsStream("/WEB-INF/stopwords/stopwords_pt.txt");
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

            context.setAttribute("portugueseStopwords", stopwordsPT);
            //portugueseProcessor.setStopwords(stopwordsEN);
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            long startTime = System.nanoTime();
            String directory = sce.getServletContext().getRealPath("/") + "/WEB-INF/language-profiles/";
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
            URL[] urls = {sce.getServletContext().getResource("/WEB-INF/i18n/")};
            ClassLoader loader = new URLClassLoader(urls);
            
            context.setAttribute("englishMessages", ResourceBundle.getBundle("MessagesBundle", localeEN, loader));
            context.setAttribute("portugueseMessages", ResourceBundle.getBundle("MessagesBundle", localePT, loader));
            
        }catch(Exception e){
            logger.log(Level.SEVERE, null, e);
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) 
    {
        //destroy resources;
        //close pool
        ServletContext context = sce.getServletContext();
        /*
        try {     
            Connection conn = (Connection)context.getAttribute("connectionDB");
            if(conn != null)
                conn.close();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        */
    }

}
