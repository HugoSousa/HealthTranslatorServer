/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Hugo
 */
public class RelationshipExtractor {
    public static HashMap<String, HashSet<Relationship>> extract(HashMap<String, List<String>> relationshipRules, String cui, String langCode){
        HashMap<String, HashSet<Relationship>> result = new HashMap<>();
        
        Connection connMySQL = ServletContextClass.conn_MySQL;
        PreparedStatement stmt;
        
        String database = "umls_" + langCode;
        try {
            connMySQL.setCatalog(database);
            
            String[] relList = new String[relationshipRules.keySet().size()];
            relationshipRules.keySet().toArray(relList);
            
            StringBuilder sb = new StringBuilder();
            String query =  "SELECT rel.cui1, rel.aui1, con.sab, con.str str1, rel.rela, rel.cui2, rel.aui2, con2.sab, con2.str str2, rel.rg, s.tui, s.sty " +
                            "FROM mrrel rel " +
                            "JOIN mrconso con ON rel.aui1 = con.aui " +
                            "JOIN mrconso con2 ON rel.aui2 = con2.aui " +
                            "JOIN mrsty s ON con2.cui = s.cui " +
                            "WHERE rel.cui1 = ? " +
                            "AND rel.cui1 <> rel.cui2 " +
                            "AND rela IN (";
            sb = sb.append(query);
            for(String rel: relList){
                sb.append("?,");
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append(")");
            query = sb.toString();

            stmt = connMySQL.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            
            stmt.setString(1, cui);
            for(int i = 0; i < relList.length; i++){
                stmt.setString(i+2, relList[i]);
            }
            
            ResultSet rs = stmt.executeQuery();
            
            while(rs.next()){
                
                String str1 = rs.getString("str1");
                String str2 = rs.getString("str2");
                String rela = rs.getString("rela");
                String cui2 = rs.getString("cui2");
                
                String tui = rs.getString("tui");
                
                for(String relation: relList){
                    if(rela.equals(relation)){
                        if( ! str1.equals(str2)){
                            List<String> acceptedTuiList = relationshipRules.get(relation);
                            if(acceptedTuiList == null || (acceptedTuiList != null && acceptedTuiList.contains(tui))){
                                Relationship rel = new Relationship(str1, rela, str2, cui2);
                                addRelationshipToResult(result, rela, rel); 
                            }
                        }
                    }
                }
            }
            
        } catch (SQLException ex) {
            Logger logger = LoggerFactory.createLogger(RelationshipExtractor.class.getName());
            logger.log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    private static void addRelationshipToResult(HashMap<String, HashSet<Relationship>> result, String rela, Relationship rel) {
        if( ! result.containsKey(rela)){
            HashSet<Relationship> rels = new HashSet<>();
            rels.add(rel);
            result.put(rela, rels);
        }else if( ! result.get(rela).contains(rel)){
            HashSet<Relationship> rels = result.get(rela);
            rels.add(rel);
        }
    }
}
