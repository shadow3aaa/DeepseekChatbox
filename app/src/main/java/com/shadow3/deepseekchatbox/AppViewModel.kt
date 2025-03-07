package com.shadow3.deepseekchatbox

import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class Message(
    var reasoningContent: String? = null,
    var content: String? = null,
    var error: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _apiKey = MutableStateFlow("")
    val apiKey = _apiKey.asStateFlow()

    private var _modelList = MutableStateFlow(emptyList<String>())
    val modelList = _modelList.asStateFlow()

    private val _currentModel: MutableStateFlow<String?> = MutableStateFlow(null)
    val currentModel = _currentModel.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<Pair<String, Message>>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _isWaiting = MutableStateFlow(false)
    val isWaiting: StateFlow<Boolean> = _isWaiting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val dataStore = application.dataStore

    init {
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                preferences[PreferencesKeys.API_KEY]?.let { key ->
                    _apiKey.value = key
                }

                preferences[PreferencesKeys.CURRENT_MODEL]?.let { model ->
                    _currentModel.value = model
                }
            }
        }
    }

    fun updateModelList() {
        val request = Request.Builder()
            .url("https://api.deepseek.com/models")
            .addHeader("Authorization", "Bearer ${_apiKey.value}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    return
                }

                val json = JSONObject(response.body!!.string())
                val models = json.getJSONArray("data")
                val modelList = mutableListOf<String>()
                for (i in 0 until models.length()) {
                    modelList.add(models.getJSONObject(i).getString("id"))
                }

                _modelList.value = modelList
            }
        })
    }

    fun updateCurrentModel(model: String) {
        _currentModel.value = model
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.CURRENT_MODEL] = model
            }
        }
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.API_KEY] = key
            }
        }
    }

    fun updateUserPromptInput(userMessage: String) {
        try {
            if ((_chatHistory.value.last().first == "user") or (_chatHistory.value.last().second.error != null)) {
                _chatHistory.value = _chatHistory.value.dropLast(1)
            }
        } catch (_: Exception) {
        }

        _chatHistory.value += ("user" to Message(content = userMessage))
    }

    fun regenerateResponse() {
        try {
            if (_chatHistory.value.last().first == "assistant") {
                _chatHistory.value = _chatHistory.value.dropLast(1)
            }
        } catch (_: Exception) {
        }

        sendRequest()
    }

    fun sendRequest() {
        if (_apiKey.value.isBlank()) {
            _errorMessage.value = "API Key is missing"
            return
        }
        _isWaiting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            fetchResponseStream()
        }
    }

    private fun fetchResponseStream() {
        try {
            val json = JSONObject().apply {
                put("model", _currentModel.value)
                put("messages", JSONArray().apply {
                    _chatHistory.value.forEach { (role, msg) ->
                        if (msg.content != null) {
                            put(JSONObject().apply {
                                put("role", role)
                                put("content", msg.content)
                            })
                        }
                    }
                })
                put("stream", true)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.deepseek.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer ${_apiKey.value}")
                .post(body)
                .build()

            val listener = object : EventSourceListener() {
                var responseContentBuffer = ""
                var reasoningContentResponseBuffer = ""
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    try {
                        if (data.trim() == "[DONE]") {
                            eventSource.cancel()
                            _isWaiting.value = false
                            return
                        }

                        val jsonObject = JSONObject(data)
                        val choices = jsonObject.getJSONArray("choices")

                        if (choices.length() > 0) {
                            val choice = choices.getJSONObject(0)

                            if (choice.optString("finish_reason") == "stop") {
                                eventSource.cancel()
                                _isWaiting.value = false
                                return
                            }

                            val delta = choice.getJSONObject("delta")

                            delta.optString("reasoning_content").let {
                                if (it != "null") {
                                    reasoningContentResponseBuffer += it
                                }
                            }

                            delta.optString("content").let {
                                if (it != "null") {
                                    responseContentBuffer += it
                                }
                            }

                            _chatHistory.value =
                                _chatHistory.value.dropLast(1) + ("assistant" to Message(
                                    content = responseContentBuffer,
                                    reasoningContent = reasoningContentResponseBuffer
                                ))
                        }
                    } catch (e: Exception) {
                        Log.e("AppViewModel", "JSON Parsing Error: ${e.message}")
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    if (!_isWaiting.value) {
                        return
                    }

                    eventSource.cancel()
                    val errorMsg = (response?.body?.string() ?: "") + (t?.message ?: "")
                    _chatHistory.value.last().second.error = "Error: $errorMsg"
                    _isWaiting.value = false
                }
            }

            EventSources.createFactory(client).newEventSource(request, listener)
            _chatHistory.value += ("assistant" to Message(content = ""))
        } catch (e: Exception) {
            _isWaiting.value = false
            _errorMessage.value = "Error: ${e.message}"
        }
    }
}
