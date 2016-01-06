package jacz.peerengineclient;

import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;

/**
 * This interface contains methods that are invoked upon events during a download
 * <p>
 * todo cuantos stores pongo, como gestiono el foreign share?
 * <p>
 * un store general para descargas de grupo, con un foreign share asociado
 * los permisos especificos a cada fichero se tendran que gestionar a un nivel superior
 * un store para ficheros compartidos con usuarios especificos
 * <p>
 * y un unico store y el se apagna?
 * <p>
 * mejor dar aqui tb la funcionalidad de stores, y que el decida como lo usa
 * <p>
 * poder agnadir y eliminar stores locales y foraneos, y especificar el store en la descarga
 * <p>
 * A lo mejor podria dar hecho uno general, y si al descargar no especifica store, tira de ese. SI
 * <p>
 * Al descargar, dejar que especifica path final (si null, lo escojo yo y se lo doy en el informe final)
 * <p>
 * <p>
 * lo guay seria poder compartir ficheros con todos, con grupos, o con uno
 */
public interface DownloadEvents {

    /**
     * The download just started. This is invoked during the invocation of the download method itself
     *
     * @param downloadInfo    info about this download
     * @param downloadManager download manager associated to this download
     */
    void started(DownloadInfo downloadInfo, DownloadManager downloadManager);

    /**
     * The download was paused (the user issued it)
     *
     * @param downloadInfo    info about this download
     * @param downloadManager download manager associated to this download
     */
    void paused(DownloadInfo downloadInfo, DownloadManager downloadManager);

    /**
     * The download was resumed (the user issued it)
     *
     * @param downloadInfo    info about this download
     * @param downloadManager download manager associated to this download
     */
    void resumed(DownloadInfo downloadInfo, DownloadManager downloadManager);

    /**
     * This is invoked when the download process was cancelled (no possibility of resuming). This can be caused by
     * the user of by an error
     *
     * @param downloadInfo    info about this download
     * @param downloadManager download manager associated to this download
     * @param reason          reason of this cancellation
     */
    void cancelled(DownloadInfo downloadInfo, DownloadManager downloadManager, DownloadProgressNotificationHandler.CancellationReason reason);

    /**
     * This is invoked when the download process was stopped by the user (with the intention of backing up the
     * download for later use). No resuming is allowed. A new download process must be initiated.
     *
     * @param downloadInfo    info about this download
     * @param downloadManager download manager associated to this download
     */
    void stopped(DownloadInfo downloadInfo, DownloadManager downloadManager);

    /**
     * The download was completed. The corresponding resource writer already completed its own "complete" operation,
     * so the resource is ready for full use
     *
     * @param downloadInfo    info about this download
     * @param path            path to the downloaded resource
     * @param downloadManager download manager associated to this download
     */
    void completed(DownloadInfo downloadInfo, String path, DownloadManager downloadManager);
}
