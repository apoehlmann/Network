package de.apoehlmann.network

import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.json.JSONObject
import java.io.File
import java.io.InvalidClassException

/**
 * Created by alex on 10.02.18.
 */

interface NetworkManager {

    fun start()
    fun <T> add(query: Query<T>): Single<T>
    fun stop()

    companion object Factory {
        fun create(): NetworkManager {
            val cacheDir = File("")
            val cache = DiskBasedCache(cacheDir)
            val network = BasicNetwork(HurlStack())
            val requestQueue = RequestQueue(cache, network)
            return NetworkManagerImpl(requestQueue)
        }
    }
}

class NetworkManagerImpl(val requestQueue: RequestQueue) : NetworkManager {

    override fun start() {
        requestQueue.start()
    }

    override fun <T> add(query: Query<T>): Single<T> {
        val callBack = PublishSubject.create<T>()
        var request: JsonObjectRequest? = null
        when(query) {
            is Query.GetQuery -> request = createGetRequest(query, callBack)
            is Query.PostQuery -> request = createPostRequest(query, callBack)
        }

        if(request != null) {
            requestQueue.add(request)
        } else {
            callBack.onError(InvalidClassException("This kind of Query is not exist"))
        }
        return callBack.firstOrError()
    }

    fun <T> createPostRequest(query: Query.PostQuery<T>, callBack: PublishSubject<T>):JsonObjectRequest {
        val json = JSONObject(Gson().toJson(query))
        return JsonObjectRequest(Request.Method.GET, query.uri.path, json,
                CallBackListener(callBack, query.response), ErrorCallBackListener(callBack))    }

    fun <T> createGetRequest(query: Query.GetQuery<T>, callBack: PublishSubject<T>): JsonObjectRequest {
        return JsonObjectRequest(Request.Method.GET, query.uri.path, null,
                CallBackListener(callBack, query.response), ErrorCallBackListener(callBack))
    }

    override fun stop() {
        requestQueue.stop()
    }

    class CallBackListener<T>(val callBack: PublishSubject<T>, val responseType: Class<T>) : com.android.volley.Response.Listener<JSONObject> {

        override fun onResponse(response: JSONObject?) {
            if(response != null) {
                val gson = Gson()
                callBack.onNext(gson.fromJson<T>(response.toString(), responseType))
            } else {
                callBack.onError(NullPointerException("response is null"))
            }
        }
    }

    class ErrorCallBackListener<T>(val callBack: PublishSubject<T>) : com.android.volley.Response.ErrorListener {

        override fun onErrorResponse(error: VolleyError?) {
            if(error != null) {
                callBack.onError(error)
            } else {
                callBack.onError(NullPointerException("error is null"))
            }
        }
    }
}
