/**
 * openCloud Android client application
 *
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
@file:Suppress("FunctionNaming")

package eu.opencloud.android.presentation.files

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import eu.opencloud.android.data.providers.implementation.OCSharedPreferencesProvider
import eu.opencloud.android.presentation.material3.BrowserControls
import eu.opencloud.android.presentation.material3.OpenCloudMaterialTheme

/** Material 3 controls with the existing Fragment/Java callback API. */
class SortOptionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {
    var onSortOptionsListener: SortOptionsListener? = null
    var onCreateFolderListener: CreateFolderListener? = null
    var onSortSelected by mutableStateOf<((SortType, SortOrder) -> Unit)?>(null)
    var onGridColumnsSelected by mutableStateOf<((Int) -> Unit)?>(null)
    var preferredGridColumns by mutableStateOf(0)
    var viewTypeSelected by mutableStateOf(ViewType.VIEW_TYPE_LIST)
    private var additionalView by mutableStateOf(AdditionalView.VIEW_TYPE)
    private var selectedSortType by mutableStateOf(SortType.SORT_TYPE_BY_NAME)
    var sortOrderSelected by mutableStateOf(SortOrder.SORT_ORDER_ASCENDING)
    var sortTypeSelected: SortType
        get() = selectedSortType
        set(value) {
            if (value == selectedSortType) sortOrderSelected = sortOrderSelected.getOppositeSortOrder()
            selectedSortType = value
        }

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val preferences = OCSharedPreferencesProvider(context)
        sortTypeSelected = SortType.values().getOrElse(
            preferences.getInt(SortType.PREF_FILE_LIST_SORT_TYPE, SortType.SORT_TYPE_BY_NAME.ordinal)
        ) { SortType.SORT_TYPE_BY_NAME }
        sortOrderSelected = SortOrder.fromPreference(
            preferences.getInt(SortOrder.PREF_FILE_LIST_SORT_ORDER, SortOrder.SORT_ORDER_ASCENDING.ordinal)
        )
    }

    @Composable
    override fun Content() {
        OpenCloudMaterialTheme {
            BrowserControls(
                sortType = sortTypeSelected,
                sortOrder = sortOrderSelected,
                viewType = viewTypeSelected,
                columns = preferredGridColumns,
                additionalView = additionalView,
                onSort = onSortSelected,
                onLegacySort = { onSortOptionsListener?.onSortTypeListener(sortTypeSelected, sortOrderSelected) },
                onViewType = { onSortOptionsListener?.onViewTypeListener(viewTypeSelected.getOppositeViewType()) },
                onDensity = onGridColumnsSelected,
                onCreateFolder = { onCreateFolderListener?.onCreateFolderListener() },
            )
        }
    }

    fun selectAdditionalView(view: AdditionalView) {
        additionalView = view
    }

    interface SortOptionsListener {
        fun onSortTypeListener(sortType: SortType, sortOrder: SortOrder)
        fun onViewTypeListener(viewType: ViewType)
    }

    interface CreateFolderListener {
        fun onCreateFolderListener()
    }

    enum class AdditionalView { CREATE_FOLDER, VIEW_TYPE, HIDDEN }
}
