package jacz.peerengineclient;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 14/07/12
 * Time: 13:42
 * To change this template use File | Settings | File Templates.
 */
public class ResourceRequestResult {

    public enum Response {
        // the client did not find the requested file
        RESOURCE_NOT_FOUND,
        // the client denied the requested file for this peer
        REQUEST_DENIED,
        // the client accepted the request, the file is provided
        REQUEST_APPROVED
    }

    private final Response response;

    private final String resourcePath;

    public static ResourceRequestResult resourceNotFound() {
        return new ResourceRequestResult(Response.RESOURCE_NOT_FOUND, null);
    }
    public static ResourceRequestResult requestDenied() {
        return new ResourceRequestResult(Response.REQUEST_DENIED, null);
    }

    public static ResourceRequestResult requestApproved(String resourcePath) {
        return new ResourceRequestResult(Response.REQUEST_APPROVED, resourcePath);
    }

    private ResourceRequestResult(Response response, String resourcePath) {
        this.response = response;
        this.resourcePath = resourcePath;
    }

    public Response getResponse() {
        return response;
    }

    public String getResourcePath() {
        return resourcePath;
    }
}
