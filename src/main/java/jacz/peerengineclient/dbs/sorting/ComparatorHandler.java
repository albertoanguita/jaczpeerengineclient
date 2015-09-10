package jacz.peerengineclient.dbs.sorting;

import jacz.store.common.LibraryItem;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 28/06/14
 * Time: 16:47
 * To change this template use File | Settings | File Templates.
 */
public class ComparatorHandler {

    public enum Comparison {
        CREATION_DATE,
        TITLE,
    }

    private static final Map<Comparison, Comparator<LibraryItem>> comparators = defineComparators();

    private static Map<Comparison, Comparator<LibraryItem>> defineComparators() {
        Map<Comparison, Comparator<LibraryItem>> comparators = new HashMap<>();
        comparators.put(Comparison.CREATION_DATE, new CreationDateComparator());
        comparators.put(Comparison.TITLE, new TitleComparator());
        return comparators;
    }

    public static Comparator<LibraryItem> getComparator(Comparison comparison) {
        return comparators.get(comparison);
    }
}
