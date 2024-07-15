package com.example.basiscodelab.phi
import android.util.Log

class GenAIWrapper(modelPath: String) : AutoCloseable {
    // Load the GenAI library on application startup.
    companion object {
        init {
            System.loadLibrary("genai") // JNI layer
            System.loadLibrary("onnxruntime-genai")
            System.loadLibrary("onnxruntime")
        }
    }

    private var nativeModel: Long = 0
    private var nativeTokenizer: Long = 0
    private var listener: TokenUpdateListener? = null



    interface TokenUpdateListener {
        fun onTokenUpdate(token: String)

    }

    init {
        nativeModel = loadModel(modelPath)
        nativeTokenizer = createTokenizer(nativeModel)
    }


    fun setTokenUpdateListener(listener: TokenUpdateListener) {
        this.listener = listener
    }

    fun run(prompt: String) {
        run(nativeModel, nativeTokenizer, prompt, true)
    }

    override fun close() {
        if (nativeTokenizer != 0L) {
            releaseTokenizer(nativeTokenizer)
        }

        if (nativeModel != 0L) {
            releaseModel(nativeModel)
        }

        nativeTokenizer = 0
        nativeModel = 0
    }

    fun gotNextToken(token: String) {
        Log.i("GenAI", "gotNextToken: $token")
        listener?.onTokenUpdate(token)
    }

    private external fun loadModel(modelPath: String): Long

    private external fun releaseModel(nativeModel: Long)

    private external fun createTokenizer(nativeModel: Long): Long

    private external fun releaseTokenizer(nativeTokenizer: Long)

    private external fun run(nativeModel: Long, nativeTokenizer: Long, prompt: String, useCallback: Boolean): String
}