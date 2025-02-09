package com.example.overlay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenAIService {
    companion object {
        private const val API_URL = "https://api.openai.com/v1/chat/completions"
        private const val API_KEY = "YOUR_API_KEY" // Replace with your API key
    }

    suspend fun sendQuery(text: String): String = withContext(Dispatchers.IO) {
        var retryCount = 0
        while (true) {
            try {
                return@withContext makeApiRequest(text)
            } catch (e: Exception) {
                e.printStackTrace()
                // Wait 1 second before retrying
                delay(1000)
                retryCount++
                // Continue retrying indefinitely
            }
        }
    }

    private suspend fun makeApiRequest(text: String): String {
        try {
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $API_KEY")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("model", "gpt-4")
                put("messages", arrayOf(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", text)
                    }
                ))
                put("temperature", 0.7)
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(response)
            return@withContext jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            throw e // Throw the error to trigger retry
        }
}