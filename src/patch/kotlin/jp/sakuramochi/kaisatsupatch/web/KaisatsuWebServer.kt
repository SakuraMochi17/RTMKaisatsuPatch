package jp.sakuramochi.kaisatsupatch.web

import com.sun.net.httpserver.HttpServer
import jp.sakuramochi.kaisatsupatch.RTMKaisatsuPatchCore
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import net.minecraft.server.MinecraftServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

object KaisatsuWebServer {

    const val PORT = 19190
    private var server: HttpServer? = null

    fun start() {
        try {
            val http = HttpServer.create(InetSocketAddress(PORT), 0)

            // GET /api/seats → JSON
            http.createContext("/api/seats") { ex ->
                val json = buildSeatsJson()
                respond(ex, "application/json; charset=utf-8", json)
            }

            // GET / → HTML ページ
            http.createContext("/") { ex ->
                respond(ex, "text/html; charset=utf-8", INDEX_HTML)
            }

            http.executor = null
            http.start()
            server = http
            RTMKaisatsuPatchCore.logger.info("KaizPatch WebServer started on port $PORT")
        } catch (e: Exception) {
            RTMKaisatsuPatchCore.logger.warn("KaizPatch WebServer failed to start: ${e.message}")
        }
    }

    fun stop() {
        server?.stop(0)
        server = null
        RTMKaisatsuPatchCore.logger.info("KaizPatch WebServer stopped")
    }

    // -----------------------------------------------------------------------
    // JSON ビルド
    // -----------------------------------------------------------------------
    private fun buildSeatsJson(): String {
        val mcServer = MinecraftServer.getServer()
            ?: return """{"error":"server not ready"}"""
        val world = mcServer.worldServers?.getOrNull(0)
            ?: return """{"error":"world not found"}"""
        val data = KaisatsuNetworkData.get(world)
            ?: return """{"trains":[]}"""

        val sb = StringBuilder()
        sb.append("{\"trains\":[")
        data.trainData.values.forEachIndexed { ti, train ->
            if (ti > 0) sb.append(",")
            sb.append("{")
            sb.append("\"trainID\":${j(train.trainID)},")
            sb.append("\"trainName\":${j(train.trainName)},")
            sb.append("\"trainType\":${j(train.trainType)},")
            sb.append("\"stopStations\":[${train.stopStations.joinToString(",") { j(it) }}],")
            sb.append("\"cars\":[")
            train.cars.forEachIndexed { ci, car ->
                if (ci > 0) sb.append(",")
                val taken = data.reservations.keys
                    .count { it.startsWith("${train.trainID}:${car.carNumber}:") }
                val available = maxOf(0, car.seatCount - taken)
                sb.append("{")
                sb.append("\"carNumber\":${car.carNumber},")
                sb.append("\"carClass\":${j(car.carClass)},")
                sb.append("\"totalSeats\":${car.seatCount},")
                sb.append("\"reservedSeats\":$taken,")
                sb.append("\"available\":$available")
                sb.append("}")
            }
            sb.append("]}")
        }
        sb.append("]}")
        return sb.toString()
    }

    /** JSON 文字列エスケープ（クォート付き） */
    private fun j(s: String) = "\"${s.replace("\\", "\\\\").replace("\"", "\\\"")}\""

    // -----------------------------------------------------------------------
    // HTTP レスポンス送信
    // -----------------------------------------------------------------------
    private fun respond(ex: com.sun.net.httpserver.HttpExchange, contentType: String, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        ex.responseHeaders.apply {
            add("Content-Type", contentType)
            add("Access-Control-Allow-Origin", "*")  // 別オリジンから fetch() してもOK
        }
        ex.sendResponseHeaders(200, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }

    // -----------------------------------------------------------------------
    // 空席表示 HTML（埋め込み）
    // -----------------------------------------------------------------------
    private val INDEX_HTML = """
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>指定席 空席情報</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', sans-serif; background: #1a1a2e; color: #ddd; padding: 24px; }
    h1 { color: #e2b74b; font-size: 1.6em; margin-bottom: 4px; }
    .subtitle { color: #888; font-size: 0.9em; margin-bottom: 20px; }
    .train-card {
      background: #16213e; border-radius: 10px;
      padding: 16px; margin-bottom: 16px;
      border-left: 4px solid #0f9b8e;
    }
    .train-header { display: flex; align-items: baseline; gap: 10px; margin-bottom: 6px; }
    .train-name { font-size: 1.15em; font-weight: bold; color: #7ee8e8; }
    .train-type { background: #0f3460; color: #aaa; font-size: 0.8em; padding: 2px 8px; border-radius: 4px; }
    .stops { color: #777; font-size: 0.85em; margin-bottom: 12px; }
    table { width: 100%; border-collapse: collapse; font-size: 0.9em; }
    th { background: #0f3460; color: #aaa; padding: 7px 10px; text-align: left; }
    td { padding: 7px 10px; border-bottom: 1px solid #253050; }
    .green  { color: #55ff55; font-weight: bold; }
    .yellow { color: #ffcc00; font-weight: bold; }
    .red    { color: #ff5555; font-weight: bold; }
    .bar-bg { background: #253050; border-radius: 4px; height: 8px; width: 100%; }
    .bar-fill { background: #0f9b8e; border-radius: 4px; height: 8px; transition: width .4s; }
    .bar-fill.yellow { background: #ffcc00; }
    .bar-fill.red    { background: #ff5555; }
    #status { color: #666; font-size: 0.82em; margin-top: 16px; }
    #error  { color: #ff5555; margin-top: 20px; }
    .empty  { color: #555; margin-top: 20px; }
  </style>
</head>
<body>
  <h1>🚄 指定席 空席情報</h1>
  <p class="subtitle" id="status">読み込み中…</p>
  <div id="content"></div>
  <p id="error"></p>
  <script>
    async function refresh() {
      try {
        const res = await fetch('/api/seats');
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();
        const now = new Date().toLocaleTimeString('ja-JP');
        document.getElementById('status').textContent = '最終更新: ' + now + '（10秒ごと自動更新）';
        document.getElementById('error').textContent = '';

        const trains = data.trains || [];
        if (trains.length === 0) {
          document.getElementById('content').innerHTML = '<p class="empty">列車データがありません。まず列車管理ブロックで列車を設定してください。</p>';
          return;
        }

        let html = '';
        trains.forEach(train => {
          const totalAll = train.cars.reduce((s, c) => s + c.totalSeats, 0);
          const availAll = train.cars.reduce((s, c) => s + c.available, 0);
          const pct = totalAll > 0 ? (availAll / totalAll * 100).toFixed(0) : 0;
          const barCls = pct <= 0 ? 'red' : pct <= 20 ? 'yellow' : '';

          html += '<div class="train-card">';
          html += '<div class="train-header">';
          html += '<span class="train-name">' + esc(train.trainName) + '</span>';
          html += '<span class="train-type">' + esc(train.trainType) + '</span>';
          html += '</div>';
          html += '<div class="stops">停車駅: ' + train.stopStations.map(esc).join(' → ') + '</div>';
          html += '<div class="bar-bg" style="margin-bottom:10px"><div class="bar-fill ' + barCls + '" style="width:' + pct + '%"></div></div>';
          html += '<table><tr><th>号車</th><th>クラス</th><th>空席 / 全席</th><th>状況</th></tr>';
          train.cars.forEach(car => {
            const ratio = car.totalSeats > 0 ? car.available / car.totalSeats : 0;
            const cls = ratio === 0 ? 'red' : ratio <= 0.1 ? 'yellow' : 'green';
            const status = ratio === 0 ? '満席' : ratio <= 0.1 ? '残りわずか' : '空席あり';
            html += '<tr>';
            html += '<td>' + car.carNumber + '号車</td>';
            html += '<td>' + esc(car.carClass) + '</td>';
            html += '<td class="' + cls + '">' + car.available + ' / ' + car.totalSeats + '</td>';
            html += '<td class="' + cls + '">' + status + '</td>';
            html += '</tr>';
          });
          html += '</table></div>';
        });
        document.getElementById('content').innerHTML = html;
      } catch (e) {
        document.getElementById('error').textContent = 'データ取得エラー: ' + e.message;
      }
    }
    function esc(s) {
      return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }
    refresh();
    setInterval(refresh, 10000);
  </script>
</body>
</html>
""".trimIndent()
}
