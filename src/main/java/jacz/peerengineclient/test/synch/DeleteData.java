package jacz.peerengineclient.test.synch;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 11/08/14
 * Time: 19:37
 * To change this template use File | Settings | File Templates.
 */
public class DeleteData {

    public static void main(String[] args) {
        File file = new File("./examples/configs");
        file.delete();
        file.mkdir();
    }
}
