/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author Hugo
 */
public class ServletContextClass implements ServletContextListener{
    //public static Connection conn;
    
    public static Connection conn_MySQL;
    
    private static Logger logger;

    @Override
    public void contextInitialized(ServletContextEvent arg0) 
    {
        System.out.println("CONTEXT INITIALIZED HERE!");
        
        logger = LoggerFactory.createLogger(ServletContextClass.class.getName());
        /*
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost/healthtranslator";
            conn = DriverManager.getConnection(url, "hugo", "healthtranslator");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(2);
        }
        */
        
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://localhost/umls_en";
            conn_MySQL = DriverManager.getConnection(url, "root", "");
           
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, null, e);
            System.exit(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            System.exit(2);
        }
        
        //System.out.println("RETURNING CONN");
        //System.out.println("conn: " + conn);
        //System.out.println("CONN MYSQL " + conn_MySQL);
    }


    @Override
    public void contextDestroyed(ServletContextEvent arg0) 
    {
        try {       
            if(conn_MySQL != null)
                conn_MySQL.close();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

}
