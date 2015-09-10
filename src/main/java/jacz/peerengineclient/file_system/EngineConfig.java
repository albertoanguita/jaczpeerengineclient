package jacz.peerengineclient.file_system;

/**
 * Configuration for the peer engine
 */
public class EngineConfig {

    public final String tempDownloads;

    public final int maxUploadSpeed;

    public final int maxDownloadSpeed;

    public final double precision;

    public EngineConfig(String tempDownloads, int maxUploadSpeed, int maxDownloadSpeed, double precision) {
        this.tempDownloads = tempDownloads;
        this.maxUploadSpeed = maxUploadSpeed;
        this.maxDownloadSpeed = maxDownloadSpeed;
        this.precision = precision;
    }
}
