package ceui.loxia

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ceui.lisa.models.IllustsBean
import ceui.lisa.models.ModelObject
import ceui.lisa.models.ObjectSpec
import ceui.lisa.models.UserBean
import java.io.Serializable
import kotlin.reflect.KClass


data class ObjectKey(
    val id: Long,
    val type: Int
) : Serializable

object ObjectPool {

    val store = mutableMapOf<ObjectKey, MutableLiveData<Any>>()

    fun putUserPreview(preview: UserPreview) {
        preview.user?.let { user ->
            update(user)
        }

        preview.illusts?.forEach { illust ->
            update(illust)
        }
    }

    fun updateIllust(illust: IllustsBean) {
        update(illust)
        illust.user?.let { user ->
            update(user)
        }
    }

    /**
     * @param illustId The id of specified illustration
     * @return
     * */
    fun getIllust(illustId: Long): LiveData<IllustsBean> {
        return get(illustId)
    }

    fun updateUser(userBean: UserBean) {
        update(userBean)
    }

    fun followUser(userId: Long) {
        val exist = get<UserBean>(userId).value ?: return
        exist.isIs_followed = true
        update(exist)
    }

    fun unFollowUser(userId: Long) {
        val exist = get<UserBean>(userId).value ?: return
        exist.isIs_followed = false
        update(exist)
    }

    /**
     * @param id The id of the illustration
     * @return
     * */
    inline fun <reified ObjectT : ModelObject> get(id: Long): LiveData<ObjectT> {
        return getFromMap(ObjectT::class, id)
    }

    /**
     * @param objClass The data source
     * @param id The id of the illustration
     * @return
     * */
    fun <ObjectT : ModelObject> getFromMap(objClass: KClass<ObjectT>, id: Long): LiveData<ObjectT> {
        val key = ObjectKey(id, findObjectSpec(objClass))
        val storedLiveData = store[key]
        return (if (storedLiveData == null) {
            val newly = MutableLiveData<Any>()
            store[key] = newly
            newly
        } else {
            storedLiveData
        }) as LiveData<ObjectT>
    }

    inline fun <reified ObjectT : ModelObject> update(obj: ObjectT, isFullVersion: Boolean = false) {
        return updateObjectPool(obj, isFullVersion)
    }

    inline fun <reified ObjectT : ModelObject> updateObjectPool(obj: ObjectT, isFullVersion: Boolean) {
        val key = ObjectKey(obj.objectUniqueId, obj.objectType)
        val storedObject = store[key]
        if (storedObject == null) {
            store[key] = MutableLiveData(obj)
        } else {
            try {
                if (isFullVersion) {
                    storedObject.value = obj
                } else {
//                    val lastValue = storedObject.value
//                    if (lastValue != null) {
//                        storedObject.value = merge(ObjectT::class, lastValue as ObjectT, obj)
//                    } else {
                        storedObject.value = obj
//                    }
                }
            } catch (ex: Exception) {
                storedObject.postValue(obj)
            }
        }
        Log.d("updateObjectPool", "对象池大小：${store.size}")
    }

    private fun <ObjectT : ModelObject> findObjectSpec(objClass: KClass<ObjectT>): Int {
        val classSimpleName = objClass.simpleName ?: return ObjectSpec.UNKNOWN
        return when (classSimpleName) {
            "IllustsBean", "Novel" -> {
                ObjectSpec.POST
            }
            "UserBean" -> {
                ObjectSpec.USER
            }
            "Article" -> {
                ObjectSpec.ARTICLE
            }
            "GifInfoResponse" -> {
                ObjectSpec.GIF_INFO
            }
            else -> {
                ObjectSpec.UNKNOWN
            }
        }
    }
}