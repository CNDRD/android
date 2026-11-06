package eu.opencloud.android.presentation.sharing

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.opencloud.android.R
import eu.opencloud.android.data.ClientManager
import eu.opencloud.android.data.executeRemoteOperation
import eu.opencloud.android.lib.resources.spaces.SpaceShareUser
import eu.opencloud.android.lib.resources.spaces.SearchSpaceShareUsersOperation
import eu.opencloud.android.lib.common.operations.RemoteOperationResult
import eu.opencloud.android.lib.resources.spaces.SpaceSharingOperation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import org.koin.android.ext.android.inject

/** Same-server users only. Deliberately has no link creation or federation controls. */
class SpaceSharingDialogFragment : DialogFragment() {
    private val clients: ClientManager by inject()
    private lateinit var status: TextView
    private lateinit var members: TextView
    private lateinit var search: EditText
    private lateinit var find: Button
    private lateinit var users: Spinner
    private lateinit var roles: Spinner
    private lateinit var add: Button
    private var recipients = emptyList<SpaceShareUser>()
    private var roleIds = emptyList<String>()
    private val accountName get() = requireArguments().getString("account")!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, padding / 2)
        }
        status = TextView(context)
        members = TextView(context)
        search = EditText(context).apply {
            hint = getString(R.string.space_share_search)
            isSingleLine = true
        }
        find = Button(context).apply { setText(R.string.space_share_find) }
        users = Spinner(context).apply { contentDescription = getString(R.string.space_share_user) }
        roles = Spinner(context).apply { contentDescription = getString(R.string.space_share_role) }
        add = Button(context).apply { setText(R.string.space_share_add) }
        listOf(status, members, search, find, users, roles, add).forEach(body::addView)
        find.setOnClickListener { findUsers() }
        add.setOnClickListener { invite() }
        return MaterialAlertDialogBuilder(context)
            .setTitle(requireArguments().getString("name"))
            .setView(android.widget.ScrollView(context).apply { addView(body) })
            .setNegativeButton(R.string.space_share_close, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        loadPermissions()
    }

    private suspend fun graph(invitation: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val response = checkedResponse(
            SpaceSharingOperation(
                requireArguments().getString("drive")!!,
                requireArguments().getString("item"),
                invitation,
            ).execute(clients.getClientForSpaceSharing(accountName))
        )
        JSONObject(response)
    }

    private fun loadPermissions() = runRequest {
        val response = graph()
        val allowed = response.optJSONArray("@libre.graph.permissions.roles.allowedValues") ?: JSONArray()
        val definitions = (0 until allowed.length()).map { allowed.getJSONObject(it) }
            .filter { it.optString("id").isNotBlank() }
            .sortedBy { it.optInt("@libre.graph.weight", 0) }
        roleIds = definitions.map { it.getString("id") }
        roles.adapter = adapter(definitions.map { it.optString("displayName", it.getString("id")) })
        members.text = permissionLabels(response).joinToString("\n")
        status.setText(if (roleIds.isEmpty()) R.string.space_share_not_allowed else R.string.space_share_internal)
    }

    private fun findUsers() = runRequest {
        val query = search.text.toString().trim()
        if (query.isBlank()) {
            status.setText(R.string.space_share_search)
            return@runRequest
        }
        val response = withContext(Dispatchers.IO) {
            checkedResponse(SearchSpaceShareUsersOperation(query)
                .execute(clients.getClientForSpaceSharing(accountName)))
        }
        recipients = SpaceShareUser.fromResponse(JSONObject(response))
        users.adapter = adapter(recipients.map { user ->
            if (user.username.isBlank()) user.name else "${user.name} (${user.username})"
        })
        status.setText(if (recipients.isEmpty()) R.string.space_share_no_users else R.string.space_share_internal)
    }

    private fun invite() {
        val recipient = recipients.getOrNull(users.selectedItemPosition) ?: return
        val role = roleIds.getOrNull(roles.selectedItemPosition) ?: return
        runRequest {
            val response = graph(recipient.invitation(role))
            check(response.optJSONArray("value")?.length()?.let { it > 0 } == true)
            status.setText(R.string.space_share_success)
            try {
                members.text = permissionLabels(graph()).joinToString("\n")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Share granted, but refreshing recipients failed")
                status.setText(R.string.space_share_success_refresh_failed)
            }
        }
    }

    private fun permissionLabels(response: JSONObject): List<String> {
        val permissions = response.optJSONArray("value") ?: return emptyList()
        return (0 until permissions.length()).flatMap { index ->
            val permission = permissions.getJSONObject(index)
            val identities = permission.optJSONArray("grantedToIdentitiesV2")
                ?: permission.optJSONArray("grantedToIdentities")
                ?: permission.optJSONArray("grantedToV2")
                ?: JSONArray().apply {
                    permission.optJSONObject("grantedToV2")?.let { put(it) }
                }
            (0 until identities.length()).mapNotNull { i ->
                val identity = identities.getJSONObject(i)
                val user = identity.optJSONObject("user") ?: identity.optJSONObject("group")
                user?.optString("displayName")?.ifBlank { user.optString("id") }
            }
        }.distinct()
    }

    private fun adapter(labels: List<String>) = ArrayAdapter(
        requireContext(), android.R.layout.simple_spinner_item, labels
    ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

    private fun runRequest(block: suspend () -> Unit) {
        lifecycleScope.launch {
            setBusy(true)
            status.setText(R.string.common_loading)
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Internal Space sharing request failed")
                showRequestError(e)
            } finally {
                setBusy(false)
            }
        }
    }

    private class SharingHttpException(val statusCode: Int) : Exception("Sharing HTTP $statusCode")

    private fun checkedResponse(result: RemoteOperationResult<String>): String {
        if (!result.isSuccess && result.httpCode >= 400) throw SharingHttpException(result.httpCode)
        return executeRemoteOperation { result }
    }

    private fun showRequestError(error: Exception) {
        val message = when ((error as? SharingHttpException)?.statusCode) {
            400, 422 -> R.string.space_share_invalid
            401 -> R.string.space_share_unauthorized
            403 -> R.string.space_share_forbidden
            404, 405, 501 -> R.string.space_share_unavailable
            409 -> R.string.space_share_conflict
            null -> R.string.space_share_error
            else -> R.string.space_share_server_error
        }
        status.text = if (error is SharingHttpException) {
            getString(message) + " (HTTP ${error.statusCode})"
        } else getString(message)
    }

    private fun setBusy(busy: Boolean) {
        find.isEnabled = !busy
        search.isEnabled = !busy
        users.isEnabled = !busy
        roles.isEnabled = !busy
        add.isEnabled = !busy && recipients.isNotEmpty() && roleIds.isNotEmpty()
        isCancelable = !busy
        (dialog as? androidx.appcompat.app.AlertDialog)?.getButton(-2)?.isEnabled = !busy
    }

    companion object {
        @JvmStatic
        fun show(activity: FragmentActivity, account: String, drive: String, item: String?, name: String) {
            if (activity.supportFragmentManager.findFragmentByTag("space-sharing") != null) return
            SpaceSharingDialogFragment().apply {
                arguments = bundleOf("account" to account, "drive" to drive, "item" to item, "name" to name)
            }.show(activity.supportFragmentManager, "space-sharing")
        }
    }
}
