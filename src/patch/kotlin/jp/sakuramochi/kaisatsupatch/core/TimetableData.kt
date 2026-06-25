package jp.sakuramochi.kaisatsupatch.core

data class StopInfo(val departureMin: Int?, val arrivalMin: Int?)

data class DepartureRow(
    val time: String,
    val destination: String,
    val typeName: String,
    val trainNumber: String,
    val trainName: String
)

data class TrainEntry(
    val trainNumber: String,
    val trainName: String,
    val typeName: String,
    val direction: String,   // "下り" / "上り"
    val diaName: String,
    val stops: List<Pair<String, StopInfo?>>
) {
    fun getStopAt(station: String): StopInfo? =
        stops.firstOrNull { it.first == station && it.second != null }?.second

    /** 指定駅より後の最終停車駅（行き先表示用） */
    fun getDestination(fromStation: String): String {
        val idx = stops.indexOfFirst { it.first == fromStation && it.second != null }
        if (idx < 0) return ""
        return stops.drop(idx + 1).lastOrNull { it.second != null }?.first ?: ""
    }
}

data class TimetableData(
    val stationNames: List<String>,
    val trainTypes: List<String>,
    val diaNames: List<String>,
    val trains: List<TrainEntry>
) {
    /**
     * 指定駅の次発 [count] 件を返す。
     * fromMin = 午前0時からの経過分数。日をまたぐ場合は翌日扱いでソートする。
     * lineStations が指定されると、その駅リストに含まれる停車駅だけを持つ列車に絞り込む。
     */
    fun getNextDepartures(
        station: String, diaFilter: String, dirFilter: String,
        fromMin: Int, count: Int,
        lineStations: Set<String>? = null
    ): List<DepartureRow> {
        return trains
            .filter { t ->
                (diaFilter.isEmpty() || t.diaName == diaFilter) &&
                (dirFilter.isEmpty() || dirFilter == "両方" || t.direction == dirFilter) &&
                t.getStopAt(station) != null &&
                (lineStations == null ||
                    t.stops.filter { it.second != null }.all { it.first in lineStations })
            }
            .mapNotNull { t ->
                val dep = t.getStopAt(station)?.departureMin ?: return@mapNotNull null
                t to dep
            }
            .sortedWith(compareBy {
                val dep = it.second
                if (dep >= fromMin) dep - fromMin else dep + 1440 - fromMin
            })
            .take(count)
            .map { (t, dep) ->
                DepartureRow(
                    time        = "%02d:%02d".format(dep / 60, dep % 60),
                    destination = t.getDestination(station),
                    typeName    = t.typeName,
                    trainNumber = t.trainNumber,
                    trainName   = t.trainName
                )
            }
    }
}
