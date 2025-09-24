package com.github.silbaram.bossai.ai

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
    private val session: Any? // Using Any to avoid direct import dependency
    private val logger = LogUtils.getLogger()
    private var onnxAvailable = false
    private var actualSession: Any? = null // For lazy initialization
    private var lastFallbackReason: String? = null
    private var fallbackOccurred = false

    // Prefer detecting available classes over relying solely on a system property (which may not propagate early enough).
    // Use ClassLoader.loadClass instead of Class.forName to avoid initialization
    private fun classExists(name: String): Boolean = try {
        Thread.currentThread().contextClassLoader.loadClass(name)
        true
    } catch (_: ClassNotFoundException) {
        false
    } catch (_: LinkageError) {
        // Handle cases where class exists but can't be linked (e.g., native libs missing)
        false
    } catch (_: Exception) {
        // Handle any other initialization errors
        false
    }

    private val onnxPackage: String = run {
        val sysProp = System.getProperty("boss_ai.dev.env")
        val devHint = sysProp == "true"
        val original = "ai.onnxruntime"
    val shaded = "com.github.silbaram.bossai.shaded.onnxruntime"

        // Detection order:
        // 1. If dev hint set and original exists -> use original
        // 2. If shaded exists and original missing -> use shaded
        // 3. If original exists -> use original
        // 4. Fallback to shaded (will still fail gracefully if absent)
        when {
            devHint && classExists("$original.OrtEnvironment") -> original
            !classExists("$original.OrtEnvironment") && classExists("$shaded.OrtEnvironment") -> shaded
            classExists("$original.OrtEnvironment") -> original
            classExists("$shaded.OrtEnvironment") -> shaded
            else -> original // default; subsequent init will log failure
        }
    }

    init {
        session = try {
            logger.info("========================================")
            logger.info("🤖 Boss AI System Initialization")
            logger.info("========================================")
            logger.info("ONNX package to use (auto-detected): {}", onnxPackage)
            logger.info("System property boss_ai.dev.env = {}", System.getProperty("boss_ai.dev.env"))
            logger.info("Class presence - ai.onnxruntime: {}, shaded: {}",
                classExists("ai.onnxruntime.OrtEnvironment"),
                classExists("com.example.bossai.shaded.onnxruntime.OrtEnvironment")
            )

            // Defer ONNX initialization - only check if classes exist without initializing
            if (!classExists("$onnxPackage.OrtEnvironment")) {
                logger.warn("⚠️ ONNX Runtime library not available (package: {})", onnxPackage)
                logger.info("🔄 Falling back to heuristic AI mode")
                logger.info("🧠 AI MODE: RULE-BASED HEURISTICS (Classic Algorithm)")
                logger.info("========================================")
                null
            } else {
                // Defer actual ONNX session creation until first use to avoid DLL loading during deserialization
                logger.info("✅ ONNX Runtime classes detected, deferring initialization")
                logger.info("🧠 AI MODE: DEFERRED ML INITIALIZATION")
                logger.info("========================================")
                "DEFERRED" // Use a marker string to indicate deferred initialization
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to detect ONNX classes: {}", e.message)
            logger.info("🔄 Falling back to heuristic AI mode")
            logger.info("🧠 AI MODE: RULE-BASED HEURISTICS (Classic Algorithm)")
            logger.info("========================================")
            null
        }
    }

    /**
     * Lazy initialization of ONNX session to avoid DLL loading during entity deserialization
     */
    private fun initializeOnnxSession(): Any? {
        if (actualSession != null || session != "DEFERRED") {
            return actualSession
        }

        try {
            logger.info("🔄 Initializing ONNX session on first use...")

            // Try to dynamically load ONNX classes
            val ortEnvironmentClass = Class.forName("$onnxPackage.OrtEnvironment")
            val ortSessionClass = Class.forName("$onnxPackage.OrtSession")
            logger.info("✅ ONNX Runtime classes loaded successfully")

            val modelPath = getModelPath()
            if (modelPath != null && Files.exists(modelPath)) {
                val getEnvironmentMethod = ortEnvironmentClass.getMethod("getEnvironment")
                val env = getEnvironmentMethod.invoke(null)

                val sessionOptionsClass = Class.forName("$onnxPackage.OrtSession\$SessionOptions")
                val optionsConstructor = sessionOptionsClass.getConstructor()
                val options = optionsConstructor.newInstance()

                val createSessionMethod = ortEnvironmentClass.getMethod("createSession", String::class.java, sessionOptionsClass)
                actualSession = createSessionMethod.invoke(env, modelPath.toString(), options)

                logger.info("🎯 ONNX ML Model loaded successfully: {}", modelPath)
                logger.info("🧠 AI MODE: MACHINE LEARNING (Advanced Neural Network)")
                onnxAvailable = true
                return actualSession
            } else {
                logger.warn("⚠️ ONNX model file not found at: {}", modelPath)
                logger.info("🔄 Using heuristic AI mode")
                recordFallback("Model file not found at: $modelPath")
                return null
            }
        } catch (e: UnsatisfiedLinkError) {
            logger.error("❌ ONNX native library failed to load: {}", e.message)
            logger.info("🔄 Disabling ONNX permanently due to native library issue")
            logger.info("🧠 AI MODE: RULE-BASED HEURISTICS (Native Library Issue)")
            recordFallback("Native library loading failed: ${e.message}")
            // Mark as permanently failed to avoid retrying
            actualSession = "FAILED"
            onnxAvailable = false
            return null
        } catch (e: NoClassDefFoundError) {
            logger.error("❌ ONNX class initialization failed: {}", e.message)
            logger.info("🔄 Disabling ONNX permanently due to class initialization failure")
            logger.info("🧠 AI MODE: RULE-BASED HEURISTICS (Class Init Failure)")
            recordFallback("Class initialization failed: ${e.message}")
            // Mark as permanently failed to avoid retrying
            actualSession = "FAILED"
            onnxAvailable = false
            return null
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize ONNX session: {}", e.message)
            logger.info("🔄 Using heuristic AI mode")
            logger.debug("ONNX initialization error details:", e)
            recordFallback("Session initialization failed: ${e.message}")
            actualSession = "FAILED"
            onnxAvailable = false
            return null
        }
    }

    /**
     * Given a feature vector, returns a chosen tactic.  When a model is
     * available, performs inference; otherwise uses heuristics.  The feature
     * vector should be normalised in the caller.
     */
    fun selectTactic(features: FloatArray): Tactic {
        // Try lazy initialization if we have deferred session
        val currentSession = if (session == "DEFERRED" && actualSession != "FAILED") {
            initializeOnnxSession()
        } else {
            session
        }

        if (onnxAvailable && currentSession != null && currentSession != "FAILED") {
            try {
                // Use reflection to perform ONNX inference
                val ortEnvironmentClass = Class.forName("$onnxPackage.OrtEnvironment")
                val onnxTensorClass = Class.forName("$onnxPackage.OnnxTensor")
                val floatBufferClass = Class.forName("java.nio.FloatBuffer")

                val getEnvironmentMethod = ortEnvironmentClass.getMethod("getEnvironment")
                val env = getEnvironmentMethod.invoke(null)

                // Create tensor using reflection
                val createTensorMethod = onnxTensorClass.getMethod("createTensor", ortEnvironmentClass, floatBufferClass, LongArray::class.java)
                val floatBuffer = java.nio.FloatBuffer.wrap(features)
                val inputTensor = createTensorMethod.invoke(null, env, floatBuffer, longArrayOf(1, features.size.toLong()))

                // Get input names and run session
                val sessionClass = currentSession.javaClass
                val inputNamesMethod = sessionClass.getMethod("getInputNames")
                val inputNames = inputNamesMethod.invoke(currentSession) as Set<*>
                val firstInputName = inputNames.iterator().next() as String

                val runMethod = sessionClass.getMethod("run", Map::class.java)
                val result = runMethod.invoke(currentSession, mapOf(firstInputName to inputTensor))

                // Extract prediction from result (handle both scikit-learn and TensorFlow formats)
                val resultClass = result.javaClass
                val getMethod = resultClass.getMethod("get", Int::class.java)
                val firstResult = getMethod.invoke(result, 0)

                val valueMethod = firstResult.javaClass.getMethod("getValue")
                val resultValue = valueMethod.invoke(firstResult)

                val selectedTactic = when (resultValue) {
                    // scikit-learn style: Long[] (class indices)
                    is LongArray -> {
                        val classIndex = resultValue[0].toInt()
                        Tactic.values().getOrElse(classIndex) { Tactic.IDLE }
                    }
                    // TensorFlow style: Float[][] (logits/probabilities)
                    is Array<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val logits = (resultValue as Array<FloatArray>)[0]
                        val maxIndex = logits.indices.maxByOrNull { logits[it] } ?: 0
                        Tactic.values().getOrElse(maxIndex) { Tactic.IDLE }
                    }
                    else -> {
                        logger.warn("Unknown ONNX output format: {}", resultValue?.javaClass?.name)
                        Tactic.IDLE
                    }
                }

                logger.debug("🧠 ML Model Decision: {}", selectedTactic)
                return selectedTactic
            } catch (ex: Exception) {
                logger.warn("ONNX inference failed, falling back to heuristics: {}", ex.message)
                recordFallback("ONNX inference error: ${ex.message}")
                // Fall through to rule based fallback
            }
        }
        // Enhanced fallback heuristics considering cooldowns
        val hpPct = features.getOrNull(0) ?: 1f
        val distance = features.getOrNull(1) ?: 16f
        val nearbyCount = features.getOrNull(2) ?: 0f
        val burstOnCooldown = features.getOrNull(3) ?: 0f
        val kiteOnCooldown = features.getOrNull(4) ?: 0f  
        val summonOnCooldown = features.getOrNull(5) ?: 0f
        
        val selectedTactic = when {
            // Prioritize kiting when low health and not on cooldown
            hpPct < 0.5f && kiteOnCooldown == 0f -> Tactic.KITE
            // Use burst when players are close and not on cooldown  
            distance < 8f && burstOnCooldown == 0f -> Tactic.BURST_AOE
            // Summon when any players nearby and not on cooldown (싱글플레이 지원)
            nearbyCount >= 1f && summonOnCooldown == 0f -> Tactic.SUMMON
            // More aggressive fallback tactics
            hpPct < 0.7f && kiteOnCooldown == 0f -> Tactic.KITE
            distance < 12f && burstOnCooldown == 0f -> Tactic.BURST_AOE
            summonOnCooldown == 0f -> Tactic.SUMMON
            else -> Tactic.BURST_AOE  // 기본값도 더 공격적으로
        }
        logger.debug("🎯 Heuristic Decision: {} (HP: {:.1f}%, Dist: {:.1f}, Players: {:.0f})", 
                    selectedTactic, hpPct * 100, distance, nearbyCount)
        return selectedTactic
    }

    /**
     * ML 모드가 활성화되어 있는지 확인합니다.
     *
     * @return ML 모드 활성화 여부
     */
    fun isMLMode(): Boolean {
        return onnxAvailable && (actualSession != null && actualSession != "FAILED")
    }

    /**
     * 특정 전술에 대한 신뢰도 점수를 반환합니다 (ML 모드에서만).
     *
     * @param tactic 평가할 전술
     * @param features 기능 벡터
     * @return 신뢰도 점수 (0.0-1.0), ML 모드가 아닌 경우 0.0
     */
    fun getTacticConfidence(tactic: Tactic, features: FloatArray): Double {
        if (!isMLMode()) {
            return 0.0
        }

        try {
            val currentSession = if (session == "DEFERRED" && actualSession != "FAILED") {
                initializeOnnxSession()
            } else {
                session
            }

            if (currentSession != null && currentSession != "FAILED") {
                // ONNX 추론을 통해 모든 전술의 확률 분포 획득
                val probabilities = getFullPredictionProbabilities(features, currentSession)
                val tacticIndex = tactic.ordinal
                return if (tacticIndex < probabilities.size) {
                    probabilities[tacticIndex].toDouble()
                } else {
                    0.0
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to get tactic confidence: {}", e.message)
        }

        return 0.0
    }

    /**
     * ONNX 모델에서 전체 확률 분포를 획득합니다.
     */
    private fun getFullPredictionProbabilities(features: FloatArray, currentSession: Any): FloatArray {
        val ortEnvironmentClass = Class.forName("$onnxPackage.OrtEnvironment")
        val onnxTensorClass = Class.forName("$onnxPackage.OnnxTensor")
        val floatBufferClass = Class.forName("java.nio.FloatBuffer")

        val getEnvironmentMethod = ortEnvironmentClass.getMethod("getEnvironment")
        val env = getEnvironmentMethod.invoke(null)

        val createTensorMethod = onnxTensorClass.getMethod("createTensor", ortEnvironmentClass, floatBufferClass, LongArray::class.java)
        val floatBuffer = java.nio.FloatBuffer.wrap(features)
        val inputTensor = createTensorMethod.invoke(null, env, floatBuffer, longArrayOf(1, features.size.toLong()))

        val sessionClass = currentSession.javaClass
        val inputNamesMethod = sessionClass.getMethod("getInputNames")
        val inputNames = inputNamesMethod.invoke(currentSession) as Set<*>
        val firstInputName = inputNames.iterator().next() as String

        val runMethod = sessionClass.getMethod("run", Map::class.java)
        val result = runMethod.invoke(currentSession, mapOf(firstInputName to inputTensor))

        val resultClass = result.javaClass
        val getMethod = resultClass.getMethod("get", Int::class.java)
        val firstResult = getMethod.invoke(result, 0)

        val valueMethod = firstResult.javaClass.getMethod("getValue")
        val resultValue = valueMethod.invoke(firstResult)

        return when (resultValue) {
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                val logits = (resultValue as Array<FloatArray>)[0]
                // Convert logits to probabilities using softmax
                softmax(logits)
            }
            else -> {
                // 다른 형식의 경우 균등 분포 반환
                FloatArray(Tactic.values().size) { 1f / Tactic.values().size }
            }
        }
    }

    /**
     * Softmax 함수 구현 (로짓을 확률로 변환)
     */
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expValues = logits.map { kotlin.math.exp((it - maxLogit).toDouble()).toFloat() }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toFloatArray()
    }

    /**
     * 폴백 발생 여부를 반환합니다.
     */
    fun hasFallbackOccurred(): Boolean = fallbackOccurred

    /**
     * 마지막 폴백 이유를 반환합니다.
     */
    fun getLastFallbackReason(): String? = lastFallbackReason

    /**
     * 폴백 정보를 리셋합니다.
     */
    fun resetFallbackInfo() {
        fallbackOccurred = false
        lastFallbackReason = null
    }

    /**
     * Attempts to locate the model in the config folder (config/boss_ai/boss_tactics.onnx).
     */
    private fun getModelPath(): Path? {
        // Try multiple possible locations for the ONNX model file
        val userDir = System.getProperty("user.dir")
        logger.info("ONNX search - user.dir: {}", userDir)

        val possiblePaths = listOf(
            // Development environment (runs/client/config)
            Path(System.getProperty("user.dir"), "runs", "client", "config", "boss_ai", "boss_tactics.onnx"),
            // Standard config directory
            Path(System.getProperty("user.dir"), "config", "boss_ai", "boss_tactics.onnx"),
            // Direct relative path
            Path("config", "boss_ai", "boss_tactics.onnx")
        )
        // Log absolute forms of all candidates for easier debugging
        runCatching {
            val abs = possiblePaths.map { it.toAbsolutePath().normalize().toString() }
            logger.info("ONNX search - candidate paths: {}", abs)
        }
        
        for (path in possiblePaths) {
            if (Files.exists(path)) {
                logger.info("Found ONNX model at: {}", path)
                return path
            }
        }
        runCatching {
            val abs = possiblePaths.map { it.toAbsolutePath().normalize().toString() }
            logger.warn("ONNX model not found in any of the expected locations (absolute): {}", abs)
        }.onFailure {
            logger.warn("ONNX model not found; also failed to enumerate absolute paths: {}", it.message)
        }
        return null
    }

    /**
     * 폴백 발생을 기록합니다.
     */
    private fun recordFallback(reason: String) {
        fallbackOccurred = true
        lastFallbackReason = reason
    }
}
