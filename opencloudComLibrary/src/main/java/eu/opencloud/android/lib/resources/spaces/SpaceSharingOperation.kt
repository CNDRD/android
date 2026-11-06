package eu.opencloud.android.lib.resources.spaces

import eu.opencloud.android.lib.common.OpenCloudClient
import eu.opencloud.android.lib.common.http.methods.nonwebdav.GetMethod
import eu.opencloud.android.lib.common.http.methods.nonwebdav.PostMethod
import eu.opencloud.android.lib.common.operations.RemoteOperation
import eu.opencloud.android.lib.common.operations.RemoteOperationResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URL

/** Internal Graph sharing. IDs are opaque path segments; no public link is created. */
class SpaceSharingOperation(
    private val driveId: String,
    private val itemId: String?,
    private val invitation: JSONObject? = null,
) : RemoteOperation<String>() {
    override fun run(client: OpenCloudClient): RemoteOperationResult<String> = try {
        val uri = client.baseUri.buildUpon().appendEncodedPath("graph/v1beta1/drives")
            .appendPath(driveId)
        if (itemId == null) uri.appendPath("root") else uri.appendPath("items").appendPath(itemId)
        uri.appendPath(if (invitation == null) "permissions" else "invite")
        val url = URL(uri.build().toString())
        val method = if (invitation == null) GetMethod(url) else
            PostMethod(url, invitation.toString().toRequestBody("application/json".toMediaType()))
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
