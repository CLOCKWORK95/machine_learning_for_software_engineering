package com.mycompany.app;
import java.lang.Exception;

public class NoTestSetAvailableException extends Exception {

    public NoTestSetAvailableException( String errorMessage, int versionNumber ){
        super(" There are no entrypoints for version " + Integer.toString( versionNumber ) );
    }
    
}
