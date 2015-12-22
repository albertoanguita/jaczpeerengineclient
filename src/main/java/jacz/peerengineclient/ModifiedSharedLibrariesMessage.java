package jacz.peerengineclient;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A message indicating that one or more shared databases have been modified
 */
class ModifiedSharedLibrariesMessage implements Serializable {

    final Map<String, List<Integer>> modifiedLibraries;

    public ModifiedSharedLibrariesMessage(Map<String, List<Integer>> modifiedLibraries) {
        this.modifiedLibraries = modifiedLibraries;
    }
}
