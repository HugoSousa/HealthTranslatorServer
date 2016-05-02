/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ht.utils;

/**
 *
 * @author Hugo
 */
public class InvalidLanguageException extends Exception
{
      public InvalidLanguageException() {}

      public InvalidLanguageException(String message)
      {
         super(message);
      }
 }
