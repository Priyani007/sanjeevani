package com.sanjeevani.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.sanjeevani.model.GuidanceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class LlmState {
    object Missing   : LlmState()
    object Loading   : LlmState()
    object Ready     : LlmState()
    object Generating: LlmState()
    data class Error(val msg: String) : LlmState()
}

class SanjeevaniLLM(private val context: Context) {

    private val _state = MutableStateFlow<LlmState>(LlmState.Missing)
    val state: StateFlow<LlmState> = _state

    private var llm: LlmInference? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Callbacks for the current in-flight request; updated on each generate() call.
    @Volatile private var onSentenceCb: ((String) -> Unit)? = null
    @Volatile private var onDoneCb: (() -> Unit)? = null
    private val streamBuf = StringBuilder()

    companion object {
        fun modelFile(context: Context) = File(context.filesDir, "llm/model.bin")
    }

    fun initialize() {
        val file = modelFile(context)
        if (!file.exists()) { _state.value = LlmState.Missing; return }
        scope.launch { loadModel(file) }
    }

    private fun loadModel(file: File) {
        _state.value = LlmState.Loading
        try {
            val opts = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(file.absolutePath)
                .setMaxTokens(90)
                .setResultListener { chunk: String, done: Boolean ->
                    if (chunk.isNotEmpty()) {
                        streamBuf.append(chunk)
                        flushSentences(streamBuf) { s -> onSentenceCb?.invoke(s) }
                    }
                    if (done) {
                        val tail = streamBuf.toString().trim()
                        if (tail.isNotBlank()) onSentenceCb?.invoke(tail)
                        streamBuf.clear()
                        _state.value = LlmState.Ready
                        onDoneCb?.invoke()
                    }
                }
                .build()
            llm = LlmInference.createFromOptions(context, opts)
            _state.value = LlmState.Ready
        } catch (e: Exception) {
            _state.value = LlmState.Error(e.message ?: "Load failed")
        }
    }

    fun generate(
        question: String,
        guidance: GuidanceState,
        lastInstruction: String?,
        onSentence: (String) -> Unit,
        onDone: () -> Unit
    ) {
        val model = llm
        if (model == null || _state.value !is LlmState.Ready) {
            onSentence("I didn't catch that. Ask: what next, are my hands right, or how fast?")
            onDone()
            return
        }
        onSentenceCb = onSentence
        onDoneCb = onDone
        streamBuf.clear()
        _state.value = LlmState.Generating
        val prompt = buildPrompt(question, guidance, lastInstruction)
        try {
            model.generateResponseAsync(prompt)
        } catch (e: Exception) {
            _state.value = LlmState.Ready
            onSentence("Sorry, I couldn't process that right now.")
            onDone()
        }
    }

    private fun flushSentences(buf: StringBuilder, emit: (String) -> Unit) {
        val text = buf.toString()
        val pattern = Regex("""(?<=[.!?])\s+""")
        val parts = pattern.split(text)
        if (parts.size > 1) {
            for (i in 0 until parts.size - 1) {
                val s = parts[i].trim()
                if (s.isNotBlank()) emit(s)
            }
            buf.clear()
            buf.append(parts.last())
        }
    }

    private fun buildPrompt(
        question: String,
        guidance: GuidanceState,
        lastInstruction: String?
    ): String {
        val step = guidance.overlay.stepCardTitle.ifBlank { guidance.fsmState.name }
        val bpm = guidance.temporal.compressionRateBPM.toInt()
        val ctx = buildString {
            append("Step: $step.")
            if (bpm > 0) append(" BPM: $bpm.")
            if (lastInstruction != null) append(" Last told: $lastInstruction.")
        }
        return """<start_of_turn>user
You are Sanjeevani, an emergency CPR AI guide on Android. $ctx
Reply in 1–2 short, calm sentences. First-aid scope only. No disclaimers.
Rescuer: $question<end_of_turn>
<start_of_turn>model
"""
    }

    fun isReady() = _state.value is LlmState.Ready
}
