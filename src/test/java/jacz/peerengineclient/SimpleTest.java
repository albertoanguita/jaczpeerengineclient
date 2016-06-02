package jacz.peerengineclient;

import jacz.util.concurrency.ThreadUtil;
import jacz.util.concurrency.task_executor.ThreadExecutor;
import jacz.util.lists.tuple.Duple;

import java.io.IOException;
import java.util.List;

/**
 * Created by alberto on 6/2/16.
 */
public class SimpleTest {

    public static void main(String[] args) {
        try {
            PeerEngineClient client;
            Duple<PeerEngineClient, List<String>> duple = SessionManager.load(
                    "./etc/user_0",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
            client = duple.element1;

            ThreadUtil.safeSleep(5000);
            client.stop();
            System.out.println("client stopped!");

            while (true) {
                System.out.println(ThreadExecutor.getRegisteredClients());
                ThreadUtil.safeSleep(1000);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
