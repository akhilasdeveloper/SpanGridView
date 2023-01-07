package com.akhilasdeveloper.span_grid_view.algorithms

import com.akhilasdeveloper.span_grid_view.algorithms.models.PointNode
import com.akhilasdeveloper.span_grid_view.algorithms.models.RectangleCentered
import com.akhilasdeveloper.span_grid_view.models.Point
import java.util.concurrent.ConcurrentHashMap

data class QuadTree(
    val boundary: RectangleCentered,
    val capacity: Int
) {

    private val points = ConcurrentHashMap<Point, PointNode>()

    private var northWest: QuadTree? = null
    private var northEast: QuadTree? = null
    private var southWest: QuadTree? = null
    private var southEast: QuadTree? = null

    fun insert(point: PointNode): Boolean {

        val px = Point(x = point.x, y = point.y)

        if (!boundary.contains(px))
            return false

        if (points.keys.contains(px)){
            points[px] = point
            return true
        }

        if (points.size < capacity && northEast == null) {
            points[px] = point
            return true
        }
        if (northEast == null)
            subdivide()

        if (northWest?.insert(point) == true) return true
        if (northEast?.insert(point) == true) return true
        if (southWest?.insert(point) == true) return true
        if (southEast?.insert(point) == true) return true

        return false

    }

    fun remove(point: Point): Boolean {

        if (!boundary.contains(point))
            return false

        if (points.keys.contains(point)){
            points.remove(point)
            return true
        }

        if (northWest?.remove(point) == true) return true
        if (northEast?.remove(point) == true) return true
        if (southWest?.remove(point) == true) return true
        if (southEast?.remove(point) == true) return true

        return false

    }

    fun pull(range: RectangleCentered, found: ArrayList<PointNode> = arrayListOf()): ArrayList<PointNode> {

        if (!boundary.intersects(range))
            return arrayListOf()
        for (p in points) {
            if (range.contains(p.value))
                found.add(p.value)
        }

        if (northEast == null)
            return found

        northWest?.pull(range, found)
        northEast?.pull(range, found)
        southEast?.pull(range, found)
        southWest?.pull(range, found)

        return found
    }

    private fun subdivide() {
        val x = boundary.x
        val y = boundary.y
        val w = boundary.w
        val h = boundary.h

        val nw = RectangleCentered(x + w / 2, y - h / 2, w / 2, h / 2)
        northWest = QuadTree(nw, capacity)
        val ne = RectangleCentered(x - w / 2, y - h / 2, w / 2, h / 2)
        northEast = QuadTree(ne, capacity)
        val sw = RectangleCentered(x + w / 2, y + h / 2, w / 2, h / 2)
        southWest = QuadTree(sw, capacity)
        val se = RectangleCentered(x - w / 2, y + h / 2, w / 2, h / 2)
        southEast = QuadTree(se, capacity)

    }

}
