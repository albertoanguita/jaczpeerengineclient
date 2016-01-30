package jacz.peerengineclient.databases.synch;


import com.neovisionaries.i18n.CountryCode;
import com.neovisionaries.i18n.LanguageCode;
import jacz.database.DatabaseMediator;
import jacz.database.util.GenreCode;
import jacz.database.util.ImageHash;
import jacz.database.util.QualityCode;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * The serialization of a library item
 */
public class SerializedItem implements Serializable, Comparable<SerializedItem> {

    private DatabaseMediator.ItemType type;

    private Integer id;

    private long timestamp;

    private boolean alive;

    private HashMap<DatabaseMediator.Field, String> stringFields;
    private HashMap<DatabaseMediator.Field, Integer> integerFields;
    private HashMap<DatabaseMediator.Field, Long> longFields;
    private HashMap<DatabaseMediator.Field, Date> dateFields;
    private HashMap<DatabaseMediator.Field, QualityCode> qualityFields;
    private HashMap<DatabaseMediator.Field, List<String>> stringListFields;
    private HashMap<DatabaseMediator.Field, List<Integer>> integerListFields;
    private HashMap<DatabaseMediator.Field, List<CountryCode>> countryListFields;
    private HashMap<DatabaseMediator.Field, List<GenreCode>> genreListFields;
    private HashMap<DatabaseMediator.Field, List<LanguageCode>> languageListFields;
    private HashMap<DatabaseMediator.Field, ImageHash> imageHashFields;

    public SerializedItem(DatabaseMediator.ItemType type, Integer id, long timestamp, boolean alive) {
        this.type = type;
        this.id = id;
        this.timestamp = timestamp;
        this.alive = alive;
        stringFields = null;
        integerFields = null;
        longFields = null;
        dateFields = null;
        qualityFields = null;
        stringListFields = null;
        countryListFields = null;
        genreListFields = null;
        languageListFields = null;
        imageHashFields = null;
    }

    public DatabaseMediator.ItemType getType() {
        return type;
    }

    public Integer getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isAlive() {
        return alive;
    }

    public void addString(DatabaseMediator.Field field, String value) {
        if (stringFields == null) {
            stringFields = new HashMap<>();
        }
        stringFields.put(field, value);
    }

    public void addInteger(DatabaseMediator.Field field, Integer value) {
        if (integerFields == null) {
            integerFields = new HashMap<>();
        }
        integerFields.put(field, value);
    }

    public void addLong(DatabaseMediator.Field field, Long value) {
        if (longFields == null) {
            longFields = new HashMap<>();
        }
        longFields.put(field, value);
    }

    public void addDate(DatabaseMediator.Field field, Date value) {
        if (dateFields == null) {
            dateFields = new HashMap<>();
        }
        dateFields.put(field, value);
    }

    public void addQuality(DatabaseMediator.Field field, QualityCode quality) {
        if (qualityFields == null) {
            qualityFields = new HashMap<>();
        }
        qualityFields.put(field, quality);
    }

    public void addStringList(DatabaseMediator.Field field, List<String> idList) {
        if (stringListFields == null) {
            stringListFields = new HashMap<>();
        }
        stringListFields.put(field, idList);
    }

    public void addIntegerList(DatabaseMediator.Field field, List<Integer> idList) {
        if (integerListFields == null) {
            integerListFields = new HashMap<>();
        }
        integerListFields.put(field, idList);
    }

    public void addCountryList(DatabaseMediator.Field field, List<CountryCode> countryList) {
        if (countryListFields == null) {
            countryListFields = new HashMap<>();
        }
        countryListFields.put(field, countryList);
    }

    public void addGenreList(DatabaseMediator.Field field, List<GenreCode> genreList) {
        if (genreListFields == null) {
            genreListFields = new HashMap<>();
        }
        genreListFields.put(field, genreList);
    }

    public void addLanguageList(DatabaseMediator.Field field, List<LanguageCode> languageList) {
        if (languageListFields == null) {
            languageListFields = new HashMap<>();
        }
        languageListFields.put(field, languageList);
    }

    public void addImageHash(DatabaseMediator.Field field, ImageHash imageHash) {
        if (imageHashFields == null) {
            imageHashFields = new HashMap<>();
        }
        imageHashFields.put(field, imageHash);
    }

    public String getString(DatabaseMediator.Field field) {
        return stringFields.get(field);
    }

    public Integer getInteger(DatabaseMediator.Field field) {
        return integerFields.get(field);
    }

    public Long getLong(DatabaseMediator.Field field) {
        return longFields.get(field);
    }

    public Date getDate(DatabaseMediator.Field field) {
        return dateFields.get(field);
    }
    
    public QualityCode getQuality(DatabaseMediator.Field field) {
        return qualityFields.get(field);
    }

    public List<String> getStringList(DatabaseMediator.Field field) {
        return stringListFields.get(field);
    }

    public List<Integer> getIntegerList(DatabaseMediator.Field field) {
        return integerListFields.get(field);
    }

    public List<CountryCode> getCountryList(DatabaseMediator.Field field) {
        return countryListFields.get(field);
    }

    public List<GenreCode> getGenreList(DatabaseMediator.Field field) {
        return genreListFields.get(field);
    }

    public List<LanguageCode> getLanguageList(DatabaseMediator.Field field) {
        return languageListFields.get(field);
    }

    public ImageHash getImageHash(DatabaseMediator.Field field) {
        return imageHashFields.get(field);
    }


    @Override
    public int compareTo(@NotNull SerializedItem o) {
        if (timestamp < o.timestamp) {
            return -1;
        } else if (timestamp > o.timestamp) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "SerializedItem{" +
                "type=" + type +
                ", id=" + id +
                ", timestamp=" + timestamp +
                ", alive=" + alive +
                ", stringFields=" + stringFields +
                ", integerFields=" + integerFields +
                ", longFields=" + longFields +
                ", dateFields=" + dateFields +
                ", qualityFields=" + qualityFields +
                ", stringListFields=" + stringListFields +
                ", countryListFields=" + countryListFields +
                ", genreListFields=" + genreListFields +
                ", languageListFields=" + languageListFields +
                '}';
    }
}
