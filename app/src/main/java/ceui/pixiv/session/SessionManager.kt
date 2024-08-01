package ceui.pixiv.session

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ceui.lisa.fragments.FragmentLogin
import ceui.lisa.models.UserModel
import ceui.lisa.utils.Local
import ceui.loxia.AccountResponse
import ceui.loxia.Client
import ceui.loxia.User
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object SessionManager {

    private const val LoggedInUserJsonKey = "LoggedInUserJsonKey"

    private val _loggedInAccount = MutableLiveData<AccountResponse>()
    private val gson = Gson()

    private val prefStore: MMKV by lazy {
        MMKV.defaultMMKV()
    }

    val isLoggedIn: Boolean get() {
        return _loggedInAccount.value != null
    }

    val loggedInUid: Long get() {
        return _loggedInAccount.value?.user?.id ?: 0L
    }

    fun load() {
        val json = prefStore.getString(LoggedInUserJsonKey, "")
        if (json?.isNotEmpty() == true) {
            try {
                _loggedInAccount.value = gson.fromJson(json, AccountResponse::class.java)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun updateSession(userModel: UserModel) {
        val javaJson = gson.toJson(userModel)
        val accountResponse = gson.fromJson(javaJson, AccountResponse::class.java)
        prefStore.putString(LoggedInUserJsonKey, gson.toJson(accountResponse))
        _loggedInAccount.value = accountResponse
    }


    fun refreshAccessToken(): String? {
        return runBlocking(Dispatchers.IO) {
            try {
                val refreshToken = _loggedInAccount.value?.refresh_token ?: throw RuntimeException("refresh_token not exist")
                val userModel = Client.authApi.newRefreshToken(
                    FragmentLogin.CLIENT_ID,
                    FragmentLogin.CLIENT_SECRET,
                    FragmentLogin.REFRESH_TOKEN,
                    refreshToken,
                    true
                ).execute().body()
                if (userModel != null) {
                    updateSession(userModel)
                    getAccessToken()
                } else {
                    throw RuntimeException("newRefreshToken failed")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }
    }

    fun getAccessToken(): String {
        val account = _loggedInAccount.value ?: throw RuntimeException("account not found")
        return account.access_token ?: throw RuntimeException("access_token not exist")
    }



}