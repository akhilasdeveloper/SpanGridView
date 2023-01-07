package com.akhilasdeveloper.span_grid_view.algorithms.models

import com.akhilasdeveloper.span_grid_view.models.Point

data class RectangleCentered(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun contains(point: PointNode): Boolean = (point.x >= x - w &&
            point.x <= x + w &&
            point.y >= y - h &&
            point.y <= y + h)

    fun contains(point: Point): Boolean = (point.x >= x - w &&
            point.x <= x + w &&
            point.y >= y - h &&
            point.y <= y + h)

    fun intersects(range: RectangleCentered): Boolean =
        !(range.x - range.w > x + w ||
                range.x + range.w < x - w ||
                range.y - range.h > y + h ||
                range.y + range.h < y - h)

}
