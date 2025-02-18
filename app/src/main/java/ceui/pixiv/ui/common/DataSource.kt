package ceui.pixiv.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ceui.lisa.models.ModelObject
import ceui.loxia.Client
import ceui.loxia.KListShow
import ceui.loxia.RefreshHint
import ceui.loxia.RefreshState
import ceui.pixiv.ui.detail.ArtworksMap
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

open class DataSource<Item, T: KListShow<Item>>(
    private val dataFetcher: suspend () -> T,
    private val responseStore: ResponseStore<T>? = null,
    itemMapper: (Item) -> List<ListItemHolder>,
    private val filter: (Item) -> Boolean = { _ -> true }
) {
    private var _variableItemMapper: ((Item) -> List<ListItemHolder>)? = null

    init {
        this._variableItemMapper = itemMapper
    }

    private val currentProtoItems = mutableListOf<Item>()

    private val _itemHolders = MutableLiveData<List<ListItemHolder>>()
    val itemHoldersImpl: LiveData<List<ListItemHolder>> = _itemHolders

    private var _nextPageUrl: String? = null
    private val gson = Gson()

    private var responseClass: Class<T>? = null

    private val _refreshState = MutableLiveData<RefreshState>()
    val refreshStateImpl: LiveData<RefreshState> = _refreshState

    open suspend fun refreshImpl(hint: RefreshHint) {
        _refreshState.value = RefreshState.LOADING(refreshHint = hint)
        try {
            if (hint == RefreshHint.ErrorRetry) {
                delay(300L)
            }

            var isCacheRetrieved = false

            if (hint == RefreshHint.InitialLoad) {
                responseStore?.loadFromCache()?.let { storedResponse ->
                    applyResponse(storedResponse, false)
                    isCacheRetrieved = true
                }
            }

            if (hint == RefreshHint.PullToRefresh ||
                hint == RefreshHint.ErrorRetry ||
                responseStore == null ||
                responseStore.isCacheExpired()
             ) {
                if (hint == RefreshHint.InitialLoad && isCacheRetrieved) {
                    delay(600L)
                    _refreshState.value = RefreshState.FETCHING_LATEST()
                    delay(1000L)
                }

                val response = withContext(Dispatchers.IO) {
                    dataFetcher().also {
                        responseStore?.writeToCache(it)
                    }
                }
                applyResponse(response, false)
            }
        } catch (ex: Exception) {
            _refreshState.value = RefreshState.ERROR(ex)
            Timber.e(ex)
        }
    }

    private fun applyResponse(response: T, isLoadMore: Boolean) {
        if (!isLoadMore) {
            currentProtoItems.clear()
        }
        responseClass = response::class.java as Class<T>
        _nextPageUrl = response.nextPageUrl
        currentProtoItems.addAll(response.displayList)
        mapProtoItemsToHolders()
        _refreshState.value = RefreshState.LOADED(
            hasContent = _itemHolders.value?.isNotEmpty() == true,
            hasNext = hasNext()
        )
    }

    fun hasNext(): Boolean {
        return _nextPageUrl?.isNotEmpty() == true
    }

    open suspend fun loadMoreImpl() {
        val nextPageUrl = _nextPageUrl ?: return
        _refreshState.value = RefreshState.LOADING(refreshHint = RefreshHint.LoadMore)
        try {
            val response = withContext(Dispatchers.IO) {
                val responseBody = Client.appApi.generalGet(nextPageUrl)
                val responseJson = responseBody.string()
                gson.fromJson(responseJson, responseClass)
            }
            applyResponse(response, true)
        } catch (ex: Exception) {
            _refreshState.value = RefreshState.ERROR(ex)
            Timber.e(ex)
        }
    }

    fun prepareIdMapImpl(fragmentUniqueId: String) {
        val idList = mutableListOf<Long>()
        currentProtoItems.forEach { item ->
            if (item is ModelObject) {
                idList.add(item.objectUniqueId)
            }
        }
        ArtworksMap.store[fragmentUniqueId] = idList
    }

    private fun updateOffsetInUrl(url: String, newOffset: Int): String {
        val regex = """(offset=)(\d+)""".toRegex()
        return if (regex.containsMatchIn(url)) {
            // 替换现有的 offset 参数
            regex.replace(url) { matchResult ->
                "${matchResult.groupValues[1]}$newOffset"
            }
        } else {
            // 如果 URL 中没有 offset 参数，直接添加它
            if (url.contains("?")) {
                "$url&offset=$newOffset"
            } else {
                "$url?offset=$newOffset"
            }
        }
    }

    private fun mapProtoItemsToHolders() {
        val mapper = _variableItemMapper ?: return
        val holders = currentProtoItems
            .filter { item ->
                filter(item)
            }
            .flatMap(mapper)
        updateHolders(holders)
    }

    open fun updateHolders(holders: List<ListItemHolder>) {
        _itemHolders.value = holders
    }

    protected fun pickItemHolders(): MutableLiveData<List<ListItemHolder>> {
        return _itemHolders
    }

    open fun initialLoad(): Boolean {
        return true
    }
}
