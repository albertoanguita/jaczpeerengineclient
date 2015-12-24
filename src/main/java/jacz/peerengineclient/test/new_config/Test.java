package jacz.peerengineclient.test.new_config;

import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.test.TestUtil;

import java.io.IOException;

/**
 * Created by Alberto on 24/12/2015.
 */
public class Test {

    public static void main(String[] args) throws IOException {

        String dir = SessionManager.createUserConfig("./etc", TestUtil.randomBytes(), "alb");

        System.out.println("User config created at " + dir);

        System.out.println("END");
    }
}
