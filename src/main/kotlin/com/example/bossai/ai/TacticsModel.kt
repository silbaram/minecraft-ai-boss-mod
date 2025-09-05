package com.example.bossai.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import com.mojang.logging.LogUtils

/**
 * Loads and executes a small ONNX model to select a tactic for our boss.  If the
 * model file cannot be found or loaded, a simple rule‑based fallback is used
 * instead.  The model is expected to accept a 1×N float array and output a
 * 1×M tensor of logits, where M is the number of tactics.  The argmax of
 * the logits is mapped to a [Tactic] value.
 */
class TacticsModel {
    private val session: OrtSession?
    private val logger = LogUtils.getLogger()

    init {
        session = try {
            val modelPath = getModelPath()
            if (modelPath != null && Files.exists(modelPath)) {
                val env = OrtEnvironment.getEnvironment()
                val options = OrtSession.SessionOptions()
                logger.info("Loading ONNX tactics model: {}", modelPath)
                env.createSession(modelPath.toString(), options)
            } else {
                logger.warn("ONNX model not found, using heuristic fallback: {}", modelPath)
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to load ONNX model, falling back to heuristics", e)
            null
        }
    }

    /**
     * Given a feature vector, returns a chosen tactic.  When a model is
     * available, performs inference; otherwise uses heuristics.  The feature
     * vector should be normalised in the caller.
     */
    fun selectTactic(features: FloatArray): Tactic {
        session?.let { sess ->
            try {
                val env = OrtEnvironment.getEnvironment()
                val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), longArrayOf(1, features.size.toLong()))
                val result = sess.run(mapOf(sess.inputNames.iterator().next() to inputTensor))
                val logits = (result[0].value as Array<FloatArray>)[0]
                val maxIndex = logits.indices.maxByOrNull { logits[it] } ?: 0
                return Tactic.values().getOrElse(maxIndex) { Tactic.IDLE }
            } catch (ex: Exception) {
                // Fall through to rule based fallback
            }
        }
        // Fallback heuristics: low health -> kite; close players -> burst; otherwise summon.
        val hpPct = features.getOrNull(0) ?: 1f
        val distance = features.getOrNull(1) ?: 16f
        return when {
            hpPct < 0.3f -> Tactic.KITE
            distance < 4f -> Tactic.BURST_AOE
            else -> Tactic.SUMMON
        }
    }

    /**
     * Attempts to locate the model in the config folder (config/boss_ai/boss_tactics.onnx).
     */
    private fun getModelPath(): Path? {
        // Use the standard config directory.  In a development environment this
        // resolves to ./run/config, and in production to the player's config
        // folder.  We avoid referencing FMLPaths directly here to keep the
        // dependency tree minimal; callers can place the file manually.
        val relative = Path("config", "boss_ai", "boss_tactics.onnx")
        val absolute = Path(System.getProperty("user.dir")).resolve(relative)
        return absolute
    }
}
