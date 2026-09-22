package com.infraView.telemetry.domain

import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Silnik detekcji upadku strażaka oparty na maszynie stanów (FSM).
 *
 * Algorytm jest dostosowany do niskiej częstotliwości próbkowania (1 Hz, narzuconej
 * przez protokół komunikacji kasku). Zamiast szukać krótkotrwałego piku uderzenia
 * (co wymaga 50–100 Hz), wykrywa wzorzec czasowy składający się z trzech faz:
 *
 * 1. Anomalia kinematyczna — odchylenie SVM od ~1g lub gwałtowna rotacja (GVM)
 * 2. Zmiana orientacji — kąt nachylenia kasku zmienił się znacząco
 * 3. Bezruch — strażak nie rusza się po upadku (niska wariancja SVM)
 *
 * Stan detekcji jest utrzymywany w pamięci per incidentId (ConcurrentHashMap).
 * Restart serwera resetuje stan — akceptowalne, bo kask wyśle nowe dane za sekundę.
 */
class FallDetectionEngine {

    private val contexts = ConcurrentHashMap<Long, FallDetectorContext>()

    // ── Progi detekcji (wartości startowe, do kalibracji empirycznej) ──────────

    /** Dolny próg SVM [g] — poniżej tej wartości podejrzewamy swobodne spadanie */
    private val svmLowerThreshold = 0.6

    /** Górny próg SVM [g] — powyżej tej wartości podejrzewamy uderzenie */
    private val svmUpperThreshold = 1.8

    /** Próg GVM [°/s] — powyżej tej wartości wykrywamy gwałtowną rotację */
    private val gvmThreshold = 80.0

    /** Minimalna zmiana kąta nachylenia [°] wskazująca na upadek */
    private val tiltChangeThreshold = 50.0

    /** Próg wariancji SVM dla detekcji bezruchu */
    private val stillnessVarianceThreshold = 0.05

    /** Dolna granica SVM [g] dla stabilnego leżenia */
    private val stillnessSvmMin = 0.9

    /** Górna granica SVM [g] dla stabilnego leżenia */
    private val stillnessSvmMax = 1.1

    /** Minimalny kąt nachylenia [°] wskazujący na leżącą pozycję kasku */
    private val lyingTiltThreshold = 50.0

    /** Timeout fazy IMPACT_DETECTED [s] — po tym czasie wracamy do NORMAL */
    private val impactTimeoutSec = 3L

    /** Minimalny czas od impaktu do wyzwolenia alarmu [s] */
    private val alarmDelaySec = 5L

    /** Okres karencji po wyzwolonym alarmie [s] */
    private val cooldownSec = 30L

    // ── API publiczne ─────────────────────────────────────────────────────────

    /**
     * Ewaluuje nowy pomiar telemetryczny i zwraca wynik detekcji upadku.
     *
     * Metoda jest thread-safe — synchronizuje dostęp do kontekstu danego incydentu.
     * Jeśli brakuje danych akcelerometru (null), pomiar jest pomijany.
     *
     * @param telemetry nowy pomiar z czujników kasku
     * @return FALL_DETECTED jeśli algorytm potwierdził upadek, NO_FALL w przeciwnym razie
     */
    fun evaluate(telemetry: Telemetry): FallDetectionResult {
        val incidentId = telemetry.incidentId
            ?: return FallDetectionResult.NO_FALL

        // Brak danych z akcelerometru — nie da się ocenić upadku
        val ax = telemetry.accelRawX ?: return FallDetectionResult.NO_FALL
        val ay = telemetry.accelRawY ?: return FallDetectionResult.NO_FALL
        val az = telemetry.accelRawZ ?: return FallDetectionResult.NO_FALL

        // Żyroskop opcjonalny — jeśli brak, przyjmujemy 0 (brak rotacji)
        val gx = telemetry.gyroRawX ?: 0.0
        val gy = telemetry.gyroRawY ?: 0.0
        val gz = telemetry.gyroRawZ ?: 0.0

        // Obliczenie metryk
        val svm = sqrt(ax * ax + ay * ay + az * az)
        val gvm = sqrt(gx * gx + gy * gy + gz * gz)
        val tilt = if (svm > 0.01) {
            acos((az / svm).coerceIn(-1.0, 1.0)).toDegrees()
        } else {
            0.0
        }

        val snapshot = TelemetrySnapshot(svm, gvm, tilt, telemetry.recordedAt)
        val context = contexts.computeIfAbsent(incidentId) { FallDetectorContext() }

        synchronized(context) {
            context.addSnapshot(snapshot)
            return evaluateFsm(context, snapshot)
        }
    }

    /**
     * Czyści kontekst detekcji dla danego incydentu (np. po zakończeniu akcji).
     */
    fun clearContext(incidentId: Long) {
        contexts.remove(incidentId)
    }

    // ── Logika FSM ────────────────────────────────────────────────────────────

    private fun evaluateFsm(
        context: FallDetectorContext,
        snapshot: TelemetrySnapshot
    ): FallDetectionResult {

        when (context.phase) {

            FallDetectionPhase.NORMAL -> {
                val svmAnomaly = snapshot.svm < svmLowerThreshold || snapshot.svm > svmUpperThreshold
                val gvmAnomaly = snapshot.gvm > gvmThreshold

                if (svmAnomaly || gvmAnomaly) {
                    context.phase = FallDetectionPhase.IMPACT_DETECTED
                    context.impactTime = snapshot.recordedAt

                    // Bazowy tilt — średnia z poprzednich pomiarów (bez bieżącego)
                    context.baselineTilt = if (context.buffer.size > 1) {
                        context.buffer.toList().dropLast(1).map { it.tiltAngle }.average()
                    } else {
                        snapshot.tiltAngle
                    }
                }
            }

            FallDetectionPhase.IMPACT_DETECTED -> {
                val tiltChange = abs(snapshot.tiltAngle - context.baselineTilt)
                val isStill = snapshot.svm in stillnessSvmMin..stillnessSvmMax

                if (tiltChange > tiltChangeThreshold && isStill) {
                    context.phase = FallDetectionPhase.CONFIRMING_FALL
                } else if (secondsSince(context.impactTime, snapshot.recordedAt) > impactTimeoutSec) {
                    // Timeout — brak potwierdzenia, wracamy do normalnej pracy
                    context.phase = FallDetectionPhase.NORMAL
                }
            }

            FallDetectionPhase.CONFIRMING_FALL -> {
                val recentSnapshots = context.buffer.toList().takeLast(3)
                val variance = svmVariance(recentSnapshots)
                val stillLying = snapshot.tiltAngle > lyingTiltThreshold

                if (variance < stillnessVarianceThreshold && stillLying) {
                    if (secondsSince(context.impactTime, snapshot.recordedAt) >= alarmDelaySec) {
                        context.phase = FallDetectionPhase.COOLDOWN
                        context.alarmTime = snapshot.recordedAt
                        return FallDetectionResult.FALL_DETECTED
                    }
                } else {
                    // Strażak wznowił ruch — fałszywy alarm, powrót do normy
                    context.phase = FallDetectionPhase.NORMAL
                }
            }

            FallDetectionPhase.COOLDOWN -> {
                if (secondsSince(context.alarmTime, snapshot.recordedAt) > cooldownSec) {
                    context.phase = FallDetectionPhase.NORMAL
                }
            }
        }

        return FallDetectionResult.NO_FALL
    }

    // ── Funkcje pomocnicze ────────────────────────────────────────────────────

    private fun secondsSince(from: OffsetDateTime?, to: OffsetDateTime): Long {
        return from?.let { Duration.between(it, to).seconds } ?: 0L
    }

    /**
     * Oblicza wariancję SVM z listy migawek.
     * Zwraca MAX_VALUE dla zbyt małej próbki, żeby nie wyzwalać fałszywego bezruchu.
     */
    private fun svmVariance(snapshots: List<TelemetrySnapshot>): Double {
        if (snapshots.size < 2) return Double.MAX_VALUE
        val mean = snapshots.map { it.svm }.average()
        return snapshots.map { (it.svm - mean).pow(2) }.average()
    }

    private fun Double.toDegrees(): Double = this * 180.0 / PI
}
