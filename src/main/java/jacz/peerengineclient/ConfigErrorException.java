package jacz.peerengineclient;

/**
 * Exceptions due to errors when reading config files in their content
 */
public class ConfigErrorException extends RuntimeException {

    public ConfigErrorException(String message) {
        super(message);
    }
}
