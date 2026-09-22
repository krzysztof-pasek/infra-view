package com.infraView.telemetry.domain

import java.time.OffsetDateTime

/**
 * Fazy maszyny stanów algorytmu detekcji upadku.
 *
 * Przejścia: NORMAL → IMPACT_DETECTED → CONFIRMING_FALL → COOLDOWN → NORMAL
 */
enum class FallDetectionPhase {
    /** Normalny tryb pracy — monitoring pomiarów */
    NORMAL,

    /** Wykryto anomalię kinematyczną (SVM/GVM poza normą) */
    IMPACT_DETECTED,

    /** Potwierdzanie upadku — sprawdzanie bezruchu i zmiany orientacji */
    CONFIRMING_FALL,

    /** Alarm wyzwolony — okres karencji przed kolejną detekcją */
    COOLDOWN
}

/**
 * Wynik ewaluacji pojedynczego pomiaru przez silnik detekcji upadku.
 */
enum class FallDetectionResult {
    /** Brak upadku */
    NO_FALL,

    /** Wykryto upadek — należy wyzwolić alarm */
    FALL_DETECTED
}

/**
 * Migawka obliczonych metryk z pojedynczego pomiaru telemetrycznego.
 *
 * @param svm Signal Vector Magnitude — √(ax² + ay² + az²), normalnie ≈ 1.0g
 * @param gvm Gyroscope Vector Magnitude — √(gx² + gy² + gz²), w °/s
 * @param tiltAngle kąt nachylenia kasku względem pionu, w stopniach (0° = pionowo)
 * @param recordedAt czas pomiaru
 */
data class TelemetrySnapshot(
    val svm: Double,
    val gvm: Double,
    val tiltAngle: Double,
    val recordedAt: OffsetDateTime
)

/**
 * Kontekst detekcji upadku dla pojedynczego kasku/incydentu.
 * Przechowuje bieżący stan FSM oraz bufor ostatnich pomiarów.
 */
class FallDetectorContext {

    companion object {
        /** Pojemność bufora kołowego — 5 pomiarów = 5 sekund przy 1 Hz */
        const val BUFFER_CAPACITY = 5
    }

    /** Aktualna faza maszyny stanów */
    var phase: FallDetectionPhase = FallDetectionPhase.NORMAL

    /** Bufor kołowy ostatnich pomiarów */
    val buffer: ArrayDeque<TelemetrySnapshot> = ArrayDeque()

    /** Czas wykrycia anomalii (przejście do IMPACT_DETECTED) */
    var impactTime: OffsetDateTime? = null

    /** Bazowy kąt nachylenia sprzed potencjalnego upadku */
    var baselineTilt: Double = 0.0

    /** Czas wyzwolenia ostatniego alarmu */
    var alarmTime: OffsetDateTime? = null

    /**
     * Dodaje migawkę do bufora kołowego, usuwając najstarszą jeśli bufor jest pełny.
     */
    fun addSnapshot(snapshot: TelemetrySnapshot) {
        if (buffer.size >= BUFFER_CAPACITY) {
            buffer.removeFirst()
        }
        buffer.addLast(snapshot)
    }
}
