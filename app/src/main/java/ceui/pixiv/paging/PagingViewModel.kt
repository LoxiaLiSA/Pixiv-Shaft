package ceui.pixiv.paging

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.flatMap
import ceui.lisa.database.AppDatabase
import ceui.lisa.models.ModelObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class PagingViewModel<ObjectT : ModelObject>(
    private val db: AppDatabase,
    private val repository: PagingAPIRepository<ObjectT>,
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
    val pager = refreshTrigger.flatMapLatest {
        Pager(
            config = PagingConfig(
                pageSize = 30,
                initialLoadSize = 30,
                prefetchDistance = 0
            ),
            remoteMediator = PagingRemoteMediator(db, repository, repository.recordType),
            pagingSourceFactory = { db.generalDao().pagingSource(repository.recordType) }
        ).flow.map { pagingData ->
            pagingData.flatMap(repository::mapper)
        }
    }.cachedIn(viewModelScope)

    fun refresh() {
        refreshTrigger.value++
    }

    val recordType: Int get() = repository.recordType
}

inline fun <ObjectT : ModelObject> Fragment.pagingViewModel(
    noinline repositoryProducer: () -> PagingAPIRepository<ObjectT>,
): Lazy<PagingViewModel<ObjectT>> {
    return this.viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getAppDatabase(requireContext())
                val repository = repositoryProducer()
                return PagingViewModel(database, repository) as T
            }
        }
    }
}


inline fun <ArgsT, ObjectT : ModelObject> Fragment.pagingViewModel(
    noinline argsProducer: () -> ArgsT,
    noinline repositoryProducer: (ArgsT) -> PagingAPIRepository<ObjectT>,
): Lazy<PagingViewModel<ObjectT>> {
    return this.viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getAppDatabase(requireContext())
                val args = argsProducer()
                val repository = repositoryProducer(args)
                return PagingViewModel(database, repository) as T
            }
        }
    }
}
