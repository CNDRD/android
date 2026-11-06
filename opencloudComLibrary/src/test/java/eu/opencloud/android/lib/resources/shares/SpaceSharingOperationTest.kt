package eu.opencloud.android.lib.resources.shares

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.opencloud.android.lib.common.OpenCloudClient
import eu.opencloud.android.lib.common.authentication.OpenCloudCredentialsFactory
import eu.opencloud.android.lib.resources.spaces.SpaceSharingOperation
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class SpaceSharingOperationTest {
    private val server = MockWebServer()
    @Before fun start() { server.start() }
    @After fun stop() { server.shutdown() }
    private fun client() = OpenCloudClient(
        Uri.parse(server.url("/cloud").toString()), null, true, null,
        ApplicationProvider.getApplicationContext()
    ).apply { credentials = OpenCloudCredentialsFactory.newBearerCredentials("user", "TOKEN") }

    @Test fun `space root permissions retain server subpath and opaque id`() {
        server.enqueue(MockResponse().setBody("{\"value\":[]}"))
        assertTrue(SpaceSharingOperation("storage\$drive", null).execute(client()).isSuccess)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals(listOf("cloud", "graph", "v1beta1", "drives", "storage\$drive", "root", "permissions"), request.requestUrl!!.pathSegments)
        assertEquals("Bearer TOKEN", request.getHeader("Authorization"))
    }

    @Test fun `folder invitation uses item id and internal user body`() {
        server.enqueue(MockResponse().setBody("{\"value\":[{\"id\":\"grant\"}]}"))
        val body = JSONObject().put("recipients", JSONArray().put(
            JSONObject().put("objectId", "user-id").put("@libre.graph.recipient.type", "user")
        )).put("roles", JSONArray().put("server-role-id"))
        assertTrue(SpaceSharingOperation("drive", "drive!folder", body).execute(client()).isSuccess)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals(listOf("cloud", "graph", "v1beta1", "drives", "drive", "items", "drive!folder", "invite"), request.requestUrl!!.pathSegments)
        val sent = JSONObject(request.body.readUtf8())
        assertEquals("user-id", sent.getJSONArray("recipients").getJSONObject(0).getString("objectId"))
        assertEquals("server-role-id", sent.getJSONArray("roles").getString(0))
        assertFalse(sent.has("link"))
    }

    @Test fun `forbidden invitation is not reported as success`() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))
        assertFalse(SpaceSharingOperation("drive", null, JSONObject()).execute(client()).isSuccess)
    }
}
