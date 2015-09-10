package jacz.peerengineclient;

import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.master.ProviderStatistics;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.util.numeric.LongRange;

import java.io.Serializable;
import java.util.Map;

/**
 * This interface contains methods that are invoked upon events during a download
 * <p/>
 * todo cuantos stores pongo, como gestiono el foreign share?
 * <p/>
 * un store general para descargas de grupo, con un foreign share asociado
 * los permisos especificos a cada fichero se tendran que gestionar a un nivel superior
 * un store para ficheros compartidos con usuarios especificos
 * <p/>
 * y un unico store y el se apagna?
 * <p/>
 * mejor dar aqui tb la funcionalidad de stores, y que el decida como lo usa
 * <p/>
 * poder agnadir y eliminar stores locales y foraneos, y especificar el store en la descarga
 * <p/>
 * A lo mejor podria dar hecho uno general, y si al descargar no especifica store, tira de ese. SI
 * <p/>
 * Al descargar, dejar que especifica path final (si null, lo escojo yo y se lo doy en el informe final)
 * <p/>
 * <p/>
 * lo guay seria poder compartir ficheros con todos, con grupos, o con uno
 */
public interface DownloadEvents {

    /**
     * Possible causes of the cancellation of a download
     */
    public enum CancellationReason {
        // the cancellation was issued by the user
        USER,
        // the cancellation was due to an IO failure in the resource writer
        IO_FAILURE;
        
        static CancellationReason generateCancellationReason(DownloadProgressNotificationHandler.CancellationReason cancellationReason) {
            if (cancellationReason == DownloadProgressNotificationHandler.CancellationReason.USER) {
                return USER;
            } else {
                return IO_FAILURE;
            }
        }
    }

    /**
     * The download just started. This is invoked during the invocation of the download method itself
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     * @param genericUserData the generic data map provided when the download was initiated
     */
    public void started(String resourceID, String storeName, DownloadManager downloadManager, Map<String, Serializable> genericUserData);

    /**
     * The download obtained the total resource size
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     * @param resourceSize    the reported resource size in bytes
     */
    public void resourceSize(String resourceID, String storeName, DownloadManager downloadManager, long resourceSize);

    /**
     * This is invoked every time a provider was added to the download
     *
     * @param resourceID         id of the corresponding downloaded resource
     * @param storeName          name of the resource store from which the resource is being downloaded
     * @param providerStatistics data about the added provider
     * @param downloadManager    download manager associated to this download
     */
    public void providerAdded(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, String providerId);

    /**
     * This is invoked every time a provider was removed from the download
     *
     * @param resourceID         id of the corresponding downloaded resource
     * @param storeName          name of the resource store from which the resource is being downloaded
     * @param providerStatistics data about the removed provider
     * @param downloadManager    download manager associated to this download
     */
    public void providerRemoved(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, String providerId);

    /**
     * One of the active providers reported the part of the resource that he is currently sharing
     *
     * @param resourceID         id of the corresponding downloaded resource
     * @param storeName          name of the resource store from which the resource is being downloaded
     * @param providerStatistics data about the provider
     * @param downloadManager    download manager associated to this download
     * @param sharedPart         part of the resource shared by the provider
     */
    public void providerReportedSharedPart(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, ResourcePart sharedPart);

    /**
     * One of the active providers received a new assignment (a segment of the resource) for transferring to us
     *
     * @param resourceID         id of the corresponding downloaded resource
     * @param storeName          name of the resource store from which the resource is being downloaded
     * @param providerStatistics data about the provider
     * @param downloadManager    download manager associated to this download
     * @param assignedSegment    the new assigned segment (the total assignation can be accessed through the included provider statistics)
     */
    public void providerWasAssignedSegment(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, LongRange assignedSegment);

    /**
     * The assignation of an active provider was cleared (this can happen in 3 situations: i) the download was paused so all provider's assignation
     * was cleared, ii) the provider slowed down too much, so its assignation is cleared to avoid him taking too long to transfer the assignation, and
     * iii) the provider's speed was below a minimum allowed speed). The cause is not provided here
     *
     * @param resourceID         id of the corresponding downloaded resource
     * @param storeName          name of the resource store from which the resource is being downloaded
     * @param providerStatistics data about the provider
     * @param downloadManager    download manager associated to this download
     */
    public void providerWasClearedAssignation(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager);

    /**
     * The download was paused (the user issued it)
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     */
    public void paused(String resourceID, String storeName, DownloadManager downloadManager);

    /**
     * The download was resumed (the user issued it)
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     */
    public void resumed(String resourceID, String storeName, DownloadManager downloadManager);

    /**
     * Successfully checked an intermediate hash
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param segment         downloaded segment
     * @param downloadManager download manager associated to this download
     */
    public void downloadedSegment(String resourceID, String storeName, LongRange segment, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager);

    /**
     * Successfully checked an intermediate hash
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param range           range of the checked intermediate hash
     * @param downloadManager download manager associated to this download
     */
    public void successIntermediateHash(String resourceID, String storeName, LongRange range, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager);

    /**
     * Failed when checking an intermediate hash. The part will be downloaded again
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param range           range of the checked intermediate hash
     * @param downloadManager download manager associated to this download
     */
    public void failedIntermediateHash(String resourceID, String storeName, LongRange range, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager);

    /**
     * The system cannot check an intermediate hash due to a not valid hash algorithm
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param range           range of the checked intermediate hash
     * @param hashAlgorithm   invalid hash algorithm
     * @param downloadManager download manager associated to this download
     */
    public void invalidIntermediateHashAlgorithm(String resourceID, String storeName, LongRange range, String hashAlgorithm, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager);

    /**
     * The total hash is currently being checked for the downloaded resource. Percentage values are not repeated
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param percentage      value between 0 and 100 with the percentage completed. Not repeated in different invocations. The first is always 0,
     *                        and the last is always 100
     * @param downloadManager download manager associated to this download
     */
    public void checkingTotalHash(String resourceID, String storeName, int percentage, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager);

    /**
     * The total hash was successfully checked on the downloaded resource. The completed method will be invoked next
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     */
    public void successTotalHash(String resourceID, String storeName, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager);

    /**
     * The total hash failed when checking. The download will not start again, since the resource writer was already completed. The user must
     * invoke the download again if he wishes so. The completed method will be invoked normally right after this method
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     */
    public void failedTotalHash(String resourceID, String storeName, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager);

    /**
     * The system cannot check the total hash due to a not valid hash algorithm
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param hashAlgorithm   invalid hash algorithm
     * @param downloadManager download manager associated to this download
     */
    public void invalidTotalHashAlgorithm(String resourceID, String storeName, String hashAlgorithm, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager);

    /**
     * This is invoked when the download process was cancelled (no possibility of resuming). This can be caused by
     * the user of by an error
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param reason          reason of this cancellation
     * @param downloadManager download manager associated to this download
     * @param genericUserData the generic data map provided when the download was initiated
     */
    public void cancelled(String resourceID, String storeName, CancellationReason reason, DownloadManager downloadManager, Map<String, Serializable> genericUserData);

    /**
     * This is invoked when the download process was stopped by the user (with the intention of backing up the
     * download for later use). No resuming is allowed. A new download process must be initiated.
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     * @param genericUserData the generic data map provided when the download was initiated
     */
    public void stopped(String resourceID, String storeName, DownloadManager downloadManager, Map<String, Serializable> genericUserData);

    /**
     * The download was completed. The corresponding resource writer already completed its own "complete" operation,
     * so the resource is ready for full use
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param path            path to the downloaded resource
     * @param downloadManager download manager associated to this download
     * @param genericUserData the generic data map provided when the download was initiated
     */
    public void completed(String resourceID, String storeName, String path, DownloadManager downloadManager, Map<String, Serializable> genericUserData);
}
