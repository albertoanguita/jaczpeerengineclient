package jacz.peerengineclient.test.transfer;

/**
 * Created by IntelliJ IDEA.
 * User: Alberto
 * Date: 19/07/12
 * Time: 10:06
 * To change this template use File | Settings | File Templates.
 */
public class Test {

    public static void main(String args[]) throws Exception {

        TestDownload.main(new String[0]);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        TestUpload.main(new String[0]);
    }
}
