package tokyo.isseikuzumaki.puzzroom.domain


/**
 * 外積を計算（z成分のみ）
 * 2次元平面上の3点から外積のz成分を計算する
 *
 * @param o 原点となる点
 * @param a 第1の点
 * @param b 第2の点
 * @return 外積のz成分（正: 反時計回り、負: 時計回り、0: 一直線上）
 */
internal fun crossProduct(o: Point, a: Point, b: Point): Long {
    return (a.x.value.toLong() - o.x.value.toLong()) * (b.y.value.toLong() - o.y.value.toLong()) -
            (a.y.value.toLong() - o.y.value.toLong()) * (b.x.value.toLong() - o.x.value.toLong())
}

/**
 * 辺同士が交差しているかチェック
 *
 * @param e1 辺1
 * @param e2 辺2
 * @return 交差している場合true
 */
fun edgesIntersect(e1: Edge, e2: Edge): Boolean {
    val o1 = e1.start
    val a1 = e1.end
    val o2 = e2.start
    val a2 = e2.end

    val d1 = crossProduct(o1, a1, o2)
    val d2 = crossProduct(o1, a1, a2)
    val d3 = crossProduct(o2, a2, o1)
    val d4 = crossProduct(o2, a2, a1)

    // 一般的な交差: 両線分が互いに対して反対側にある
    if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
        ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
    ) {
        return true
    }

    // 同一直線上にある場合
    if (d1 == 0L && d2 == 0L) {
        // 線分2の両端点が直線o1-a1上にある → 重なりチェック
        return segmentsOverlap(o1, a1, o2, a2)
    }
    if (d3 == 0L && d4 == 0L) {
        // 線分1の両端点が直線o2-a2上にある → 重なりチェック
        return segmentsOverlap(o2, a2, o1, a1)
    }

    // 片方の端点が他の線分の内部にある場合（端点同士の接触を除く）
    if (d1 == 0L && onSegmentInterior(o1, o2, a1)) return true
    if (d2 == 0L && onSegmentInterior(o1, a2, a1)) return true
    if (d3 == 0L && onSegmentInterior(o2, o1, a2)) return true
    if (d4 == 0L && onSegmentInterior(o2, a1, a2)) return true

    return false
}

/**
 * 点qが線分p-rの内部にあるかチェック（端点を除く）
 *
 * @param p 線分の始点
 * @param q チェックする点
 * @param r 線分の終点
 * @return 点qが線分p-rの内部にある場合true（端点は除く）
 */
private fun onSegmentInterior(p: Point, q: Point, r: Point): Boolean {
    // 端点との一致を除外
    if (q == p || q == r) return false

    return q.x.value >= minOf(p.x.value, r.x.value) &&
            q.x.value <= maxOf(p.x.value, r.x.value) &&
            q.y.value >= minOf(p.y.value, r.y.value) &&
            q.y.value <= maxOf(p.y.value, r.y.value)
}

/**
 * 同一直線上の2線分が重なっているかチェック
 *
 * @param p1 線分1の始点
 * @param p2 線分1の終点
 * @param q1 線分2の始点
 * @param q2 線分2の終点
 * @return 重なっている場合true（端点のみの接触は除く）
 */
private fun segmentsOverlap(p1: Point, p2: Point, q1: Point, q2: Point): Boolean {
    val minP = minOf(p1.x.value, p2.x.value) to minOf(p1.y.value, p2.y.value)
    val maxP = maxOf(p1.x.value, p2.x.value) to maxOf(p1.y.value, p2.y.value)
    val minQ = minOf(q1.x.value, q2.x.value) to minOf(q1.y.value, q2.y.value)
    val maxQ = maxOf(q1.x.value, q2.x.value) to maxOf(q1.y.value, q2.y.value)

    // 端点のみの接触を除外
    if ((maxP.first == minQ.first && maxP.second == minQ.second) ||
        (minP.first == maxQ.first && minP.second == maxQ.second)) {
        return false
    }

    // 重なり判定
    return !(maxP.first < minQ.first || maxQ.first < minP.first ||
            maxP.second < minQ.second || maxQ.second < minP.second)
}