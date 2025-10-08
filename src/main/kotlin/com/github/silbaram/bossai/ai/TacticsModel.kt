package com.github.silbaram.bossai.ai

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import com.mojang.logging.LogUtils

/**
 * 작은 ONNX 모델을 로드하고 실행하여 보스의 전술을 선택합니다. 모델 파일을 찾거나 로드할 수 없는 경우,
 * 간단한 규칙 기반 폴백이 대신 사용됩니다. 모델은 1×N 부동 소수점 배열을 입력으로 받고,
 * M은 전술의 수인 1×M 텐서를 출력하는 것으로 예상됩니다. 로그잇의 argmax는 [Tactic] 값에 매핑됩니다.
 */
class TacticsModel {
    private val session: Any? // 직접적인 import 의존성을 피하기 위해 Any 사용
    private val logger = LogUtils.getLogger()
    private var onnxAvailable = false
    private var actualSession: Any? = null // 지연 초기화를 위한 변수
    private var lastFallbackReason: String? = null
    private var fallbackOccurred = false

    // 사용 가능한 클래스를 감지하는 것을 선호하며, 시스템 속성에만 의존하지 않습니다 (조기에 전파되지 않을 수 있음).
    // Class.forName 대신 ClassLoader.loadClass를 사용하여 초기화를 피합니다.
    private fun classExists(name: String): Boolean = try {
        Thread.currentThread().contextClassLoader.loadClass(name)
        true
    } catch (_: ClassNotFoundException) {
        false
    } catch (_: LinkageError) {
        // 클래스는 존재하지만 연결할 수 없는 경우 처리 (예: 네이티브 라이브러리 누락)
        false
    } catch (_: Exception) {
        // 기타 초기화 오류 처리
        false
    }

    private val onnxPackage: String = run {
        val sysProp = System.getProperty("boss_ai.dev.env")
        val devHint = sysProp == "true"
        val original = "ai.onnxruntime"
    val shaded = "com.github.silbaram.bossai.shaded.onnxruntime"

        // 탐지 순서:
        // 1. 개발자 힌트가 설정되고 원본이 존재하면 -> 원본 사용
        // 2. 음영이 존재하고 원본이 없으면 -> 음영 사용
        // 3. 원본이 존재하면 -> 원본 사용
        // 4. 음영으로 대체 (부재 시에도 정상적으로 실패)
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
            logger.info("🤖 Boss AI initialization - ONNX package: {}", onnxPackage)

            // ONNX 초기화를 지연 - 클래스가 존재하는지 초기화 없이 확인만 수행
            if (!classExists("$onnxPackage.OrtEnvironment")) {
                logger.warn("⚠️ ONNX Runtime library not available (package: {})", onnxPackage)
                logger.info("🔄 AI MODE: RULE-BASED HEURISTICS")
                null
            } else {
                // 실제 ONNX 세션 생성을 첫 사용까지 연기하여 역직렬화 중 DLL 로딩을 방지합니다.
                logger.info("✅ AI MODE: MACHINE LEARNING (Deferred Initialization)")
                "DEFERRED" // 지연 초기화를 나타내는 마커 문자열을 사용합니다.
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to detect ONNX classes: {}", e.message)
            logger.info("🔄 AI MODE: RULE-BASED HEURISTICS")
            null
        }
    }

    /**
    * 엔티티 역직렬화 중 DLL 로딩을 방지하기 위한 ONNX 세션의 지연 초기화
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
     * 주어진 특성 벡터를 기반으로 전술을 선택하여 반환합니다.
     * ONNX 모델이 사용 가능하면 모델 추론을 시도하고, 그렇지 않으면 규칙 기반 휴리스틱을 사용합니다.
     * 특성 벡터는 호출자가 정규화하여 전달해야 합니다.
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
     * config/boss_ai/boss_tactics.onnx 경로에서 모델 파일을 찾도록 시도합니다.
     * 여러 표준 위치(개발용 runs/client/config, 사용자/프로젝트의 config 디렉터리 등)를 확인하며,
     * 찾지 못하면 null을 반환합니다.
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
