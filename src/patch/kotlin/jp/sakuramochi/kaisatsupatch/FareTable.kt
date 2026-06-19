package jp.sakuramochi.kaisatsupatch

object FareTable {

    private const val DEFAULT_FARE = 150

    // "駅コードA:駅コードB" -> 運賃（円） のマップ。Phase2でJSON外部化予定。
    private val fareMap: Map<String, Int> = mapOf(
        "STATION_A:STATION_B" to 150,
        "STATION_B:STATION_A" to 150,
        "STATION_A:STATION_C" to 220,
        "STATION_C:STATION_A" to 220,
        "STATION_B:STATION_C" to 200,
        "STATION_C:STATION_B" to 200
    )

    fun getFare(fromStation: String, toStation: String): Int {
        val key = "$fromStation:$toStation"
        return fareMap[key] ?: DEFAULT_FARE
    }
}
