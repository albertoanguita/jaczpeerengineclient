package jacz.peerengineclient.test;

import jacz.util.log.ErrorHandler;

/**
 * Created by Alberto on 02/01/2016.
 */
public class ErrorHandlerImpl implements ErrorHandler {

    @Override
    public void errorRaised(String errorMessage) {
        System.err.println(errorMessage);
    }
}
