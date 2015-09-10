package jacz.peerengineclient.dbs.sorting;

import jacz.store.common.Creation;
import jacz.store.common.LibraryItem;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 28/06/14
 * Time: 16:49
 * To change this template use File | Settings | File Templates.
 */
public class TitleComparator implements Comparator<LibraryItem> {

    @Override
    public int compare(LibraryItem o1, LibraryItem o2) {
        Creation creation1 = (Creation) o1;
        Creation creation2 = (Creation) o2;
        try {
            return creation1.getTitle().compareTo(creation2.getTitle());
        } catch (Exception e) {
            return 0;
        }
    }
}
