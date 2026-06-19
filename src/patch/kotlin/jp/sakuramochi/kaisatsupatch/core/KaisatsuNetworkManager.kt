package jp.sakuramochi.kaisatsupatch.core

import net.minecraft.world.World
import java.util.PriorityQueue

object KaisatsuNetworkManager {

    private data class Edge(val toStation: String, val lineID: String, val distance: Double)

    private data class State(val station: String, val lineID: String, val cost: Double) : Comparable<State> {
        override fun compareTo(other: State) = cost.compareTo(other.cost)
    }

    fun calculateFare(world: World, startStation: String, endStation: String, isICCard: Boolean = false): Int {
        if (startStation == endStation) return 0
        val data = KaisatsuNetworkData.get(world) ?: return -1

        val graph = mutableMapOf<String, MutableList<Edge>>()
        for (line in data.companyLines.values) {
            if (line.stationOrder.size < 2) continue
            for (i in 0 until line.stationOrder.size - 1) {
                val st1 = line.stationOrder[i]
                val st2 = line.stationOrder[i + 1]
                if (st1 == st2) continue
                val dist = getDistance(st1, st2, data)
                if (dist < 0) continue
                graph.getOrPut(st1) { mutableListOf() }.add(Edge(st2, line.lineID, dist))
                graph.getOrPut(st2) { mutableListOf() }.add(Edge(st1, line.lineID, dist))
            }
        }
        if (!graph.containsKey(startStation) || !graph.containsKey(endStation)) return -1

        val pq = PriorityQueue<State>()
        val minCost = mutableMapOf<String, Double>()

        for (line in data.companyLines.values) {
            if (line.stationOrder.contains(startStation)) {
                val initial = line.baseFare.toDouble()
                pq.add(State(startStation, line.lineID, initial))
                minCost["$startStation:${line.lineID}"] = initial
            }
        }

        while (pq.isNotEmpty()) {
            val curr = pq.poll()
            if (curr.station == endStation) {
                return if (isICCard) Math.ceil(curr.cost).toInt()
                else (Math.ceil(curr.cost / 10.0) * 10).toInt()
            }
            if (curr.cost > minCost.getOrDefault("${curr.station}:${curr.lineID}", Double.MAX_VALUE)) continue

            for (edge in graph[curr.station] ?: continue) {
                val nextLine = data.companyLines[edge.lineID] ?: continue
                // 路線が変わる場合は乗換料金を加算
                val transferCost = if (edge.lineID != curr.lineID) nextLine.transferFee.toDouble() else 0.0
                val nextCost = curr.cost + edge.distance * nextLine.costPerBlock + transferCost
                val key = "${edge.toStation}:${edge.lineID}"
                if (nextCost < minCost.getOrDefault(key, Double.MAX_VALUE)) {
                    minCost[key] = nextCost
                    pq.add(State(edge.toStation, edge.lineID, nextCost))
                }
            }
        }
        return -1
    }

    /** この駅から行ける全駅と運賃の一覧を返す（重複なし・昇順）。 */
    fun getAvailableFares(world: World, fromStation: String, isICCard: Boolean = false): List<Pair<String, Int>> {
        val data = KaisatsuNetworkData.get(world) ?: return emptyList()
        return data.globalStations.keys
            .filter { it != fromStation }
            .mapNotNull { dest ->
                val fare = calculateFare(world, fromStation, dest, isICCard)
                if (fare > 0) dest to fare else null
            }
            .sortedBy { it.second }
    }

    private fun getDistance(st1: String, st2: String, data: KaisatsuNetworkData): Double {
        val c1 = data.globalStations[st1] ?: return -1.0
        val c2 = data.globalStations[st2] ?: return -1.0
        val dx = (c1.x - c2.x).toDouble()
        val dy = (c1.y - c2.y).toDouble()
        val dz = (c1.z - c2.z).toDouble()
        return Math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}
