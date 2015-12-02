package jacz.peerengineclient.stores;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Alberto on 28/11/2015.
 */
public class SerializedItem implements Serializable {

    private String type;

    private String id;

    private Integer timestamp;

    private boolean alive;

    private HashMap<String, String> stringFields;
    private HashMap<String, Integer> integerFields;
    private HashMap<String, Long> longFields;
    private HashMap<String, Date> dateFields;

    public SerializedItem(String type, String id, Integer timestamp, boolean alive) {
        this.type = type;
        this.id = id;
        this.timestamp = timestamp;
        this.alive = alive;
        stringFields = null;
        integerFields = null;
        longFields = null;
        dateFields = null;
    }

    public SerializedItem addString(String field, String value) {
        if (stringFields == null) {
            stringFields = new HashMap<>();
        }
        stringFields.put(field, value);
        return this;
    }

    public SerializedItem addInteger(String field, Integer value) {
        if (integerFields == null) {
            integerFields = new HashMap<>();
        }
        integerFields.put(field, value);
        return this;
    }

    public SerializedItem addLong(String field, Long value) {
        if (longFields == null) {
            longFields = new HashMap<>();
        }
        longFields.put(field, value);
        return this;
    }

    public SerializedItem addDate(String field, Date value) {
        if (dateFields == null) {
            dateFields = new HashMap<>();
        }
        dateFields.put(field, value);
        return this;
    }
}
