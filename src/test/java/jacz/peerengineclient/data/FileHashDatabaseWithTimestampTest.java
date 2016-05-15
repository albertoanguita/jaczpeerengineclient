package jacz.peerengineclient.data;

import jacz.util.files.FileReaderWriter;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.lists.tuple.Duple;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Alberto on 14/05/2016.
 */
public class FileHashDatabaseWithTimestampTest {

    private static final String dir = "./etc/test-fhdwt/";

    private static final String bd = dir + "fhdwt.bd";

    @Test
    public void test() throws IOException {

        FileUtils.forceMkdir(new File(dir));
        FileUtils.cleanDirectory(new File(dir));
        List<Duple<String, String>> pathAndHash = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            FileReaderWriter.writeTextFile(path(i), "file " + i);
            pathAndHash.add(new Duple<>(path(i), FileHashDatabase.getHash(new File(path(i)))));
        }

        FileHashDatabaseWithTimestamp fhd = new FileHashDatabaseWithTimestamp(bd, "id");
        for (int i = 0; i < 4; i++) {
            fhd.put(path((i)));
        }

        Assert.assertEquals("id", fhd.getId());
        List<SerializedHashItem> items = Arrays.asList(
                new SerializedHashItem(1L, pathAndHash.get(0).element2, true),
                new SerializedHashItem(2L, pathAndHash.get(1).element2, true),
                new SerializedHashItem(3L, pathAndHash.get(2).element2, true),
                new SerializedHashItem(4L, pathAndHash.get(3).element2, true)
        );
        Assert.assertEquals(items, fhd.getHashesFrom(1L));

        fhd.remove(pathAndHash.get(0).element2);
        fhd.removeValue(pathAndHash.get(1).element1);

        items = Arrays.asList(
                new SerializedHashItem(3L, pathAndHash.get(2).element2, true),
                new SerializedHashItem(4L, pathAndHash.get(3).element2, true),
                new SerializedHashItem(5L, pathAndHash.get(0).element2, false),
                new SerializedHashItem(6L, pathAndHash.get(1).element2, false)
        );
        Assert.assertEquals(items, fhd.getHashesFrom(1L));

        // load fhd again
        fhd = new FileHashDatabaseWithTimestamp(bd);
        Assert.assertEquals("id", fhd.getId());
        Assert.assertEquals(items, fhd.getHashesFrom(1L));

        fhd.put(path((4)));
        items = Arrays.asList(
                new SerializedHashItem(3L, pathAndHash.get(2).element2, true),
                new SerializedHashItem(4L, pathAndHash.get(3).element2, true),
                new SerializedHashItem(7L, pathAndHash.get(4).element2, true),
                new SerializedHashItem(5L, pathAndHash.get(0).element2, false),
                new SerializedHashItem(6L, pathAndHash.get(1).element2, false)
        );
        Assert.assertEquals(items, fhd.getHashesFrom(1L));

        FileUtils.cleanDirectory(new File(dir));
    }

    private String path(int index) {
        return dir + index;
    }
}