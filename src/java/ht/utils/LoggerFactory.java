/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author Hugo
 */
public abstract class LoggerFactory {
    
    public static Logger createLogger(String className){
        
        Logger logger = Logger.getLogger(className);
        
        //to find the log location
        //C:\Users\Hugo\AppData\Roaming\NetBeans\8.0.2\config\GF_4.1\domain1\config
        //String sRootPath = new File("").getAbsolutePath();
        /*
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("M-d_HHmmss");
            FileHandler handler = new FileHandler(dateFormat.format(Calendar.getInstance().getTime()) + ".log");
            
            SimpleFormatter format = new SimpleFormatter();
            handler.setFormatter(format);
            logger.addHandler(handler);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        */
        return logger;
    }
}
