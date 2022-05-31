package com.mycompany.app;
import java.lang.Exception;

public class NoTestSetAvailableException extends Exception {

    public NoTestSetAvailableException( String errorMessage, int versionNumber ){
        super( errorMessage + Integer.toString( versionNumber ) + "(or they are too few)" );
    }

    public NoTestSetAvailableException( String errorMessage ){
        super( errorMessage );
    }
    
}
