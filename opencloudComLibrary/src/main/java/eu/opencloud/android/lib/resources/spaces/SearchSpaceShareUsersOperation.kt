package eu.opencloud.android.lib.resources.spaces

import eu.opencloud.android.lib.common.OpenCloudClient
import eu.opencloud.android.lib.common.http.methods.nonwebdav.GetMethod
import eu.opencloud.android.lib.common.operations.RemoteOperation
import eu.opencloud.android.lib.common.operations.RemoteOperationResult
import java.net.URL

/** Search the Graph directory so invitation objectId is the actual directory ID. */
class SearchSpaceShareUsersOperation(private val query: String) : RemoteOperation<String>() {
    override fun run(client: OpenCloudClient): RemoteOperationResult<String> = try {
        val uri = client.baseUri.buildUpon().appendEncodedPath("graph/v1.0/users")
            .appendQueryParameter("\$search", query)
            .appendQueryParameter("\$filter", "userType eq 'Member'")
            .build()
        val method = GetMethod(URL(uri.toString()))
        val status = client.executeHttpMethod(method)
        if (status in 200..299) {
            RemoteOperationResult<String>(RemoteOperationResult.ResultCode.OK).apply {
                data = method.getResponseBodyAsString()
            }
        } else {
            RemoteOperationResult(method)
        }
    } catch (e: Exception) {
        RemoteOperationResult(e)
    }
}
