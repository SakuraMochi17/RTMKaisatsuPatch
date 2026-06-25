package jp.sakuramochi.kaisatsupatch.core

import java.io.File
import java.nio.charset.Charset

/**
 * OuDia (.oud) 形式の時刻表ファイルをパースする。
 * OuDia 1.x / 2.x の主要フィールドに対応。
 */
object OuDiaParser {

    fun parse(file: File): TimetableData {
        val charset = try { Charset.forName("Shift_JIS") } catch (_: Exception) { Charsets.UTF_8 }
        return parseContent(file.readText(charset))
    }

    fun parseContent(content: String): TimetableData {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val stations    = mutableListOf<String>()
        val trainTypes  = mutableListOf<String>()
        val diaNames    = mutableListOf<String>()
        val trains      = mutableListOf<TrainEntry>()

        val sectionStack = ArrayDeque<String>()
        var currentDia       = ""
        var currentDirection = ""

        // 各セクションのバッファ
        var tmpStation   = ""
        var tmpTypeName  = ""
        var tmpTrainNum  = ""
        var tmpTrainName = ""
        var tmpTypeIdx   = 0
        var tmpJikoku    = ""   // EkiJikoku の値

        for (line in lines) {
            when {
                // セクション終端
                line == "." -> {
                    when (sectionStack.removeLastOrNull()) {
                        "Eki" -> {
                            if (tmpStation.isNotEmpty()) stations.add(tmpStation)
                            tmpStation = ""
                        }
                        "Ressyasyubetsu" -> {
                            if (tmpTypeName.isNotEmpty()) trainTypes.add(tmpTypeName)
                            tmpTypeName = ""
                        }
                        "Ressya" -> {
                            trains.add(TrainEntry(
                                trainNumber = tmpTrainNum,
                                trainName   = tmpTrainName,
                                typeName    = trainTypes.getOrElse(tmpTypeIdx) { "普通" },
                                direction   = currentDirection,
                                diaName     = currentDia,
                                stops       = parseEkiJikoku(tmpJikoku, stations)
                            ))
                            tmpTrainNum = ""; tmpTrainName = ""; tmpTypeIdx = 0; tmpJikoku = ""
                        }
                    }
                }
                // 上り/下り ブロック終端（一部ファイルで使われる）
                line == "KudariEnd" -> { sectionStack.removeLastOrNull(); currentDirection = "" }
                line == "NoboriEnd" -> { sectionStack.removeLastOrNull(); currentDirection = "" }
                line == "EkiListEnd" -> sectionStack.removeLastOrNull()

                // セクション開始: "XXX." 形式（値を含まないもの）
                line.endsWith(".") && !line.contains("=") -> {
                    val sec = line.dropLast(1)
                    sectionStack.addLast(sec)
                    when (sec) {
                        "Kudari" -> currentDirection = "下り"
                        "Nobori" -> currentDirection = "上り"
                    }
                }

                // キー=値
                line.contains("=") -> {
                    val eqIdx = line.indexOf('=')
                    val key   = line.substring(0, eqIdx)
                    val value = line.substring(eqIdx + 1)
                    when (sectionStack.lastOrNull()) {
                        "Eki"            -> if (key == "Ekimei") tmpStation = value
                        "Ressyasyubetsu" -> if (key == "Syubetsumei") tmpTypeName = value
                        "Dia"            -> if (key == "DiaName") {
                            currentDia = value
                            if (!diaNames.contains(value)) diaNames.add(value)
                        }
                        "Ressya" -> when (key) {
                            "Ressyabangou"       -> tmpTrainNum  = value
                            "Ressyamei"          -> tmpTrainName = value
                            "Ressyasyubetuindex" -> tmpTypeIdx   = value.toIntOrNull() ?: 0
                            "EkiJikoku"          -> tmpJikoku    = value
                        }
                    }
                }
            }
        }

        return TimetableData(stations, trainTypes, diaNames, trains)
    }

    // ── EkiJikoku パース ──────────────────────────────────────────────

    /**
     * EkiJikoku=;0830;0845/0850;; のような値を駅ごとの StopInfo に変換する。
     * セミコロン区切りで stations の順番に対応する。
     */
    private fun parseEkiJikoku(raw: String, stations: List<String>): List<Pair<String, StopInfo?>> {
        val parts = raw.split(";")
        return stations.mapIndexed { i, st ->
            val part = parts.getOrElse(i) { "" }.trim()
            if (part.isEmpty()) return@mapIndexed st to null
            val slash = part.indexOf('/')
            val depStr = if (slash >= 0) part.substring(0, slash) else part
            val arrStr = if (slash >= 0) part.substring(slash + 1) else null
            st to StopInfo(parseTime(depStr), arrStr?.let { parseTime(it) })
        }
    }

    /** "HHMM" または "HH:MM" → 分数 (0:00 = 0, 25:30 = 1530 等、翌日5時まで対応) */
    private fun parseTime(s: String): Int? {
        val digits = s.filter { it.isDigit() }
        if (digits.length < 3) return null
        val padded = digits.padStart(4, '0')
        val h = padded.take(2).toIntOrNull() ?: return null
        val m = padded.drop(2).take(2).toIntOrNull() ?: return null
        if (h > 47 || m > 59) return null
        return h * 60 + m
    }
}
