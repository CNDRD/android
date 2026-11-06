package eu.opencloud.android.lib.resources.spaces

import org.json.JSONArray
import org.json.JSONObject

data class SpaceShareUser(val id: String, val name: String, val username: String) {
    fun invitation(roleId: String): JSONObject = JSONObject()
        .put("recipients", JSONArray().put(JSONObject()
            .put("objectId", id).put("@libre.graph.recipient.type", "user")))
        .put("roles", JSONArray().put(roleId))

    companion object {
        fun fromResponse(response: JSONObject): List<SpaceShareUser> {
            val users = response.optJSONArray("value") ?: return emptyList()
            return (0 until users.length()).map { users.getJSONObject(it) }
                .filter { it.optString("id").isNotBlank() && it.optString("userType", "Member") == "Member" }
                .map { user ->
                    SpaceShareUser(
                        user.getString("id"),
                        user.optString("displayName").ifBlank { user.getString("id") },
                        user.optString("onPremisesSamAccountName").ifBlank { user.optString("userPrincipalName") },
                    )
                }.distinctBy { it.id }
        }
    }
}
