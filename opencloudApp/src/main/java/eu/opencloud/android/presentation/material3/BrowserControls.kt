@file:Suppress("FunctionNaming", "LongMethod")

package eu.opencloud.android.presentation.material3

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.opencloud.android.R
import eu.opencloud.android.presentation.files.SortOptionsView.AdditionalView
import eu.opencloud.android.presentation.files.SortOrder
import eu.opencloud.android.presentation.files.SortType
import eu.opencloud.android.presentation.files.ViewType

@Composable
fun BrowserControls(
    sortType: SortType,
    sortOrder: SortOrder,
    viewType: ViewType,
    columns: Int,
    additionalView: AdditionalView,
    onSort: ((SortType, SortOrder) -> Unit)?,
    onLegacySort: () -> Unit,
    onViewType: () -> Unit,
    onDensity: ((Int) -> Unit)?,
    onCreateFolder: () -> Unit,
) {
    var sortExpanded by remember { mutableStateOf(false) }
    var densityExpanded by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                TextButton(
                    onClick = { if (onSort != null) sortExpanded = true else onLegacySort() },
                    modifier = Modifier.testTag("browser_sort"),
                ) {
                    Text(stringResource(sortType.toStringRes()))
                    Icon(
                        painterResource(sortOrder.toDrawableRes()),
                        contentDescription = stringResource(
                            if (sortOrder == SortOrder.SORT_ORDER_ASCENDING) R.string.m3_sort_ascending else R.string.m3_sort_descending
                        ),
                        modifier = Modifier.padding(start = 8.dp).size(18.dp),
                    )
                }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    SortType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(type.toStringRes())) },
                            leadingIcon = { RadioButton(selected = type == sortType, onClick = null) },
                            onClick = {
                                onSort?.invoke(type, if (type == sortType) sortOrder.getOppositeSortOrder() else sortOrder)
                                sortExpanded = false
                            },
                            modifier = Modifier.testTag("sort_${type.name}"),
                        )
                    }
                    HorizontalDivider()
                    SortOrder.values().forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(
                                    if (order == SortOrder.SORT_ORDER_ASCENDING) R.string.m3_sort_ascending else R.string.m3_sort_descending
                                ))
                            },
                            leadingIcon = { RadioButton(selected = order == sortOrder, onClick = null) },
                            onClick = { onSort?.invoke(sortType, order); sortExpanded = false },
                        )
                    }
                }
            }
            if (onDensity != null && additionalView == AdditionalView.VIEW_TYPE) {
                Box {
                    TextButton(onClick = { densityExpanded = true }, modifier = Modifier.testTag("browser_density")) {
                        Text(if (columns == 0) stringResource(R.string.grid_density_title)
                        else pluralStringResource(R.plurals.grid_density_columns, columns, columns))
                    }
                    DropdownMenu(expanded = densityExpanded, onDismissRequest = { densityExpanded = false }) {
                        listOf(0, 2, 3, 4, 5, 6).forEach { count ->
                            DropdownMenuItem(
                                text = { Text(if (count == 0) stringResource(R.string.grid_density_automatic)
                                else pluralStringResource(R.plurals.grid_density_columns, count, count)) },
                                leadingIcon = { RadioButton(selected = columns == count, onClick = null) },
                                onClick = { onDensity(count); densityExpanded = false },
                                modifier = Modifier.testTag("density_$count"),
                            )
                        }
                    }
                }
            }
            when (additionalView) {
                AdditionalView.VIEW_TYPE -> FilledTonalIconButton(onClick = onViewType, modifier = Modifier.testTag("browser_view")) {
                    Icon(
                        painterResource(viewType.getOppositeViewType().toDrawableRes()),
                        stringResource(if (viewType == ViewType.VIEW_TYPE_LIST) R.string.m3_show_grid else R.string.m3_show_list),
                    )
                }
                AdditionalView.CREATE_FOLDER -> FilledTonalIconButton(onClick = onCreateFolder) {
                    Icon(painterResource(R.drawable.ic_action_create_dir), stringResource(R.string.content_description_create_new_folder))
                }
                AdditionalView.HIDDEN -> Unit
            }
        }
    }
}
