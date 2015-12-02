package jacz.peerengineclient.dbs_old.sorting;

import jacz.store.common.LibraryItem;

import java.util.Comparator;

/**
 * Compares creation dates of items
 */
public class CreationDateComparator implements Comparator<LibraryItem> {

    @Override
    public int compare(LibraryItem o1, LibraryItem o2) {
        try {
            return o1.getCreationDate().compareTo(o2.getCreationDate());
        } catch (Exception e) {
            return 0;
        }
    }
}
