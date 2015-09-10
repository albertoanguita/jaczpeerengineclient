package jacz.peerengineclient.dbs;

import jacz.util.files.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Reserves file names and generates empty files for the transmission of files in synch processes
 */
public class FileGenerator {

    private static final String PRE_INDEX = "_";
    private static final String POST_INDEX = "";

    public static synchronized String generateEmptyFile(String dir, String suggestedFilename) throws IOException {
        String filenameWithoutExtension = FileUtil.getFileNameWithoutExtension(suggestedFilename);
        filenameWithoutExtension = removeIndex(filenameWithoutExtension);
        String extension = FileUtil.getFileExtension(suggestedFilename);
        String fileName = FileUtil.createNonExistingFileNameWithIndex(dir, filenameWithoutExtension, extension, PRE_INDEX, POST_INDEX, true);
        File file = new File(FileUtil.joinPaths(dir, fileName));
        file.createNewFile();
        return file.getPath();
    }

    private static String removeIndex(String filename) {
        // if this filename ends with an index (presumably added by this class) we first remove it so we don't chain indexes
        int preIndex = filename.lastIndexOf(PRE_INDEX);
        if (preIndex > 0) {
            // check if there is a number after the pre index
            String rest = filename.substring(preIndex + PRE_INDEX.length());
            boolean restIsNumber;
            try {
                Integer.parseInt(rest);
                restIsNumber = true;
            } catch (NumberFormatException e) {
                restIsNumber = false;
            }
            if (restIsNumber) {
                // we have to remove the index
                return filename.substring(0, preIndex);
            }
        }
        // there is no index
        return filename;
    }
}
