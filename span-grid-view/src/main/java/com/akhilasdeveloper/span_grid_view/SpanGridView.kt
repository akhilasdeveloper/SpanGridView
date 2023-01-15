package com.akhilasdeveloper.span_grid_view

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import com.akhilasdeveloper.span_grid_view.algorithms.QuadTree
import com.akhilasdeveloper.span_grid_view.algorithms.models.Node
import com.akhilasdeveloper.span_grid_view.algorithms.models.PointNode
import com.akhilasdeveloper.span_grid_view.algorithms.models.RectangleCentered
import com.akhilasdeveloper.span_grid_view.models.Point
import com.akhilasdeveloper.span_grid_view.models.PointF
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt


class SpanGridView(
    context: Context,
    attributeSet: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : View(context, attributeSet, defStyleAttr, defStyleRes) {

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : this(
        context,
        attributeSet,
        defStyleAttr,
        0
    ) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet) : this(context, attributeSet, 0, 0) {
        init(attributeSet)
    }

    constructor(context: Context) : this(context, null, 0, 0) {
        init(null)
    }

    companion object {
        const val MODE_VIEW: Int = -2
        const val MODE_DRAW: Int = -3
    }

    //Determines the velocity in which the fling animation starts
    private val minDistanceMoved = 50

    //Fling friction
    private val flingFriction = 1.1f

    var minResolution = 20f
        set(value) {
            if (value > maxResolution)
                maxResolution = value
            field = value
            setScaleToResolution(scale)
        }
    var maxResolution = 150f
        set(value) {
            if (value < minResolution)
                minResolution = value
            field = value
            setScaleToResolution(scale)
        }
    private var touchCount = 0
    private val fact
        get() = gridWidth / width

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private var mListener: OnGridSelectListener? = null

    //List of all the points in the screen
    var pointsOnScreen = ConcurrentHashMap<Point, Node>()
        private set

    //Quad Tree data structure to store the points
    private var historyQuad = createQuadTree()

    /**
     * Creates and initializes quad tree
     */
    private fun createQuadTree() =
        QuadTree(RectangleCentered(0f, 0f, Int.MAX_VALUE.toFloat(), Int.MAX_VALUE.toFloat()), 4)

    var gridColor = ResourcesCompat.getColor(resources, R.color.grid_color, null)
    var lineColor = ResourcesCompat.getColor(resources, R.color.line_color, null)

    var scaleEnabled = true
    var drawEnabled = true
        set(value) {
            field = value
            determineViewMode()
        }
    var spanEnabled = true
    var lineEnabled = true
        set(value) {
            field = value
            postInvalidate()
        }
    var mode: Int = MODE_DRAW
        set(value) {
            field = value
            mListener?.onModeChange(mode)
        }

    var brushSize = 1
        set(value) {
            val data = value.coerceIn(1, 5)
            field = data
        }

    private var resolution: Float = minResolution
        set(value) {
            val data = value.coerceIn(minResolution, maxResolution)
            field = data
            this.reDraw()
            Log.d("scale", "scale: $scale")
        }

    var scale: Float = .5f
        set(value) {
            val data = value.coerceIn(0f, 1f)
            field = data
            setScaleToResolution(data)
        }
        get() = getScaleFromResolution()


    var lineWidth: Float = 1f
        set(value) {
            field = value
            _lineWidth = value
            this.reDraw()
        }

    private var _lineWidth: Float = 1f
    private var _strokeWidth: Float = 5f

    private var xOff = 0f
    private var yOff = 0f

    private var lineStart: Point? = null
    private var linePoints = mutableSetOf<Point>()

    //Returns the coordinates of top left point in the current screen
    var startPoint: Point = Point(0, 0)
        get() = getPixelDetails(PointF(0f, 0f))
        private set

    //Returns width of current grid
    val gridWidth: Float
        get() = ((width - _lineWidth) / (resolution + _lineWidth))

    //Returns height of current grid
    val gridHeight: Float
        get() = ((height - _lineWidth) / (resolution + _lineWidth))

    private var xFlingValue: Float = 0f
        set(value) {
            field = value
            xOff = (value * fact)

            reDraw()
        }

    private var yFlingValue: Float = 0f
        set(value) {
            field = value
            yOff = (value * fact)

            reDraw()
        }


    private val xFling = object : FloatPropertyCompat<SpanGridView>("xFling") {
        override fun getValue(`object`: SpanGridView?): Float {
            return `object`?.xFlingValue ?: 0f
        }

        override fun setValue(`object`: SpanGridView?, value: Float) {
            `object`?.xFlingValue = value
        }

    }

    private val yFling = object : FloatPropertyCompat<SpanGridView>("yFling") {
        override fun getValue(`object`: SpanGridView?): Float {
            return `object`?.yFlingValue ?: 0f
        }

        override fun setValue(`object`: SpanGridView?, value: Float) {
            `object`?.yFlingValue = value
        }

    }

    private val flingX = FlingAnimation(this@SpanGridView, xFling).setFriction(flingFriction)
    private val flingY = FlingAnimation(this@SpanGridView, yFling).setFriction(flingFriction)

    private fun init(set: AttributeSet?) {
        set?.let { attrSet ->

            val ta = context.obtainStyledAttributes(attrSet, R.styleable.SpanGridView)

            lineColor = ta.getColor(
                R.styleable.SpanGridView_lineColor,
                ResourcesCompat.getColor(resources, R.color.line_color, null)
            )
            gridColor = ta.getColor(
                R.styleable.SpanGridView_gridColor,
                ResourcesCompat.getColor(resources, R.color.grid_color, null)
            )
            scale = ta.getFloat(R.styleable.SpanGridView_scale, 1f)
            lineWidth = ta.getDimension(R.styleable.SpanGridView_lineWidth, 1f).dpToPx()
            minResolution =
                ta.getDimension(R.styleable.SpanGridView_minResolution, minResolution).dpToPx()
            maxResolution =
                ta.getDimension(R.styleable.SpanGridView_maxResolution, maxResolution).dpToPx()
            scaleEnabled = ta.getBoolean(R.styleable.SpanGridView_enableScale, true)
            spanEnabled = ta.getBoolean(R.styleable.SpanGridView_enableSpan, true)
            lineEnabled = ta.getBoolean(R.styleable.SpanGridView_enableLine, true)
            brushSize = ta.getInteger(R.styleable.SpanGridView_brushSize, 1)

            ta.recycle()

        }
    }

    private fun setScaleToResolution(scale: Float) {
        resolution = (maxResolution - minResolution) * scale
    }

    private fun getScaleFromResolution() = resolution / (maxResolution - minResolution)

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {

            if (mode == MODE_VIEW && spanEnabled) {

                xFling.setValue(this@SpanGridView, (xOff - distanceX * fact) / fact)
                yFling.setValue(this@SpanGridView, (yOff - distanceY * fact) / fact)

            }
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {

            if (mode == MODE_VIEW) {

                val distanceInX: Float = abs(e2.rawX - e1.rawX)
                val distanceInY: Float = abs(e2.rawY - e1.rawY)


                if (distanceInX > minDistanceMoved) {
                    flingX.setStartVelocity(velocityX)
                        .start()
                }
                if (distanceInY > minDistanceMoved) {
                    flingY.setStartVelocity(velocityY)
                        .start()
                }
            }

            return true
        }
    }


    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {

            if (mode == MODE_VIEW && scaleEnabled) {

                val scale = resolution * detector.scaleFactor
                if (scale in minResolution..maxResolution) {

                    _lineWidth *= detector.scaleFactor
                    _strokeWidth *= detector.scaleFactor

                    val factS =
                        (scale - resolution) / ((resolution + _lineWidth) * (scale + _lineWidth))

                    xFling.setValue(this@SpanGridView, (xOff - detector.focusX * factS) / fact)
                    yFling.setValue(this@SpanGridView, (yOff - detector.focusY * factS) / fact)

                    resolution = scale

                }

            }

            return true
        }

    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)
    private val mGestureDetector = GestureDetector(context, mGestureListener)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        reDraw()
    }

    /**
     * Clear all the points
     */
    fun clearData() {
        pointsOnScreen.clear()
        historyQuad = createQuadTree()
        postInvalidate()
    }

    /***
     * Change the canvas coordinates to Grid Coordinates
     */
    private fun getPixelDetails(px: PointF): Point {
        val fact = resolution + _lineWidth
        val sx = (px.x / fact) - xOff
        val sy = (px.y / fact) - yOff
        return Point(floor(sx).toInt(), floor(sy).toInt())
    }

    private fun determineViewMode() {
        mode = if (touchCount > 1 || !drawEnabled)
            MODE_VIEW
        else
            MODE_DRAW
    }

    private fun setTouchCount(count: Int) {
        if (count == 0)
            touchCount = 0
        else
            if (count > touchCount)
                touchCount = count
        determineViewMode()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        mGestureDetector.onTouchEvent(event)
        mScaleDetector.onTouchEvent(event)
        this.performClick()

        setTouchCount(event.pointerCount)

        val pxF = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                flingX.cancel()
                flingY.cancel()
                lineStart = null
                linePoints.clear()
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == MODE_DRAW) {
                    val px = getPixelDetails(pxF)
                    lineStart?.let {
                        drawLine(it.x, it.y, px.x, px.y)
                    }
                    lineStart = px
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mode == MODE_DRAW) {
                    val px = getPixelDetails(pxF)
                    drawCenterSquare(px.x, px.y, brushSize)
                    linePoints.clear()
                    lineStart = null
                }

                setTouchCount(0)
            }
        }
        return true
    }

    private fun drawCenterSquare(xc: Int, yc: Int, r: Int) {
        val rr = r - 1
        val x1 = xc - rr
        val y1 = yc - rr
        val x2 = xc + rr
        val y2 = yc + rr

        for (x in x1..x2)
            for (y in y1..y2) {
                val point = Point(x, y)
                if (!linePoints.contains(point)){
                    linePoints.add(point)
                    mListener?.onDraw(Point(x, y))
                }
            }
    }

    private fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
        var x = x1
        var y = y1
        val w = x2 - x
        val h = y2 - y
        var dx1 = 0
        var dy1 = 0
        var dx2 = 0
        var dy2 = 0
        if (w < 0) dx1 = -1 else if (w > 0) dx1 = 1
        if (h < 0) dy1 = -1 else if (h > 0) dy1 = 1
        if (w < 0) dx2 = -1 else if (w > 0) dx2 = 1
        var longest = abs(w)
        var shortest = abs(h)
        if (longest <= shortest) {
            longest = abs(h)
            shortest = abs(w)
            if (h < 0) dy2 = -1 else if (h > 0) dy2 = 1
            dx2 = 0
        }
        var numerator = longest shr 1
        for (i in 0..longest) {

            drawCenterSquare(x, y, brushSize)

            numerator += shortest
            if (numerator >= longest) {
                numerator -= longest
                x += dx1
                y += dy1
            } else {
                x += dx2
                y += dy2
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /**
     * Plot point with color
     */
    fun plotPoint(px: Point, color: Int) {
        val pointNode = PointNode(x = px.x, y = px.y, fill = color)
        historyQuad.insert(pointNode)
        if (isPointInCurrentScreen(px)) {
            addToPoints(pointNode)
            postInvalidate()
        }
    }

    fun plotPoints(pxs: ArrayList<Point>, color: Int) {
        var postInvalidate = false
        pxs.forEach { px->
            val pointNode = PointNode(x = px.x, y = px.y, fill = color)
            historyQuad.insert(pointNode)
            if (isPointInCurrentScreen(px)) {
                addToPoints(pointNode)
                postInvalidate = true
            }
        }
        if (postInvalidate)
            postInvalidate()
    }

    private fun isPointInCurrentScreen(point: Point): Boolean {

        val w = (gridWidth / 2) + 2
        val h = (gridHeight / 2) + 2
        val x = w - xOff - 1
        val y = h - yOff - 1

        return point.x >= x - w &&
                point.x <= x + w &&
                point.y >= y - h &&
                point.y <= y + h
    }

    /**
     * Clear given point
     */
    fun clearPoint(px: Point) {
        historyQuad.remove(px)
        pointsOnScreen.remove(px)
        postInvalidate()
    }

    fun clearPoints(pxs: ArrayList<Point>){
        pxs.forEach {px->
            historyQuad.remove(px)
            pointsOnScreen.remove(px)
        }
        postInvalidate()
    }

    private fun reDraw() {
        val w = (gridWidth / 2) + 2
        val h = (gridHeight / 2) + 2
        val x = w - xOff - 1
        val y = h - yOff - 1
        pointsOnScreen.clear()
        historyQuad.pull(
            RectangleCentered(
                x = x,
                y = y,
                w = w,
                h = h
            )
        ).forEach {
            addToPoints(it)
        }
        postInvalidate()
    }

    private fun addToPoints(px: PointNode) {
        val xs = getViewFactor(px.x + xOff) + (_lineWidth / 2)
        val ys = getViewFactor(px.y + yOff) + (_lineWidth / 2)
        val fill = px.fill

        pointsOnScreen[Point(x = px.x, y = px.y)] =
            Node(
                x = px.x,
                y = px.y,
                x1 = xs,
                y1 = ys,
                x2 = xs + resolution,
                y2 = ys + resolution,
                fill = fill
            )

    }

    private fun getViewFactor(c: Float) = (c * (resolution + _lineWidth))

    private fun Canvas.drawLines(x1: Float, y1: Float, x2: Float, y2: Float) {
        val xs1 = getViewFactor(x1)
        val ys1 = getViewFactor(y1)

        val xs2 = getViewFactor(x2)
        val ys2 = getViewFactor(y2)

        paint.color = lineColor
        paint.strokeWidth = _lineWidth
        paint.style = Paint.Style.STROKE
        this.drawLine(xs1, ys1, xs2, ys2, paint)
    }

    private fun Canvas.drawGridLines() {
        drawColor(gridColor)

        val factX = -xOff.toInt() + xOff
        val factY = -yOff.toInt() + yOff
        if (lineEnabled) {
            for (xx in -1..gridWidth.toInt() + 1) {

                drawLines(
                    xx + factX,
                    -1 + factY,
                    xx + factX,
                    gridHeight.toInt() + factY + 2
                )

            }

            for (yy in -1..gridHeight.toInt() + 1) {

                drawLines(
                    -1 + factX,
                    yy + factY,
                    gridWidth.toInt() + factX + 2,
                    yy + factY
                )

            }
        }
    }

    private fun Canvas.drawGridPoints() {
        pointsOnScreen.forEach { data ->

            val px = data.value
            paint.color = px.fill
            paint.style = Paint.Style.FILL
            this.drawRect(px.x1, px.y1, px.x2, px.y2, paint)

        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawGridLines()
            drawGridPoints()
        }
    }

    /**
     * Set listener for the SpanGridView
     */
    fun setGridSelectListener(eventListener: OnGridSelectListener) {
        mListener = eventListener
    }

    interface OnGridSelectListener {
        fun onDraw(px: Point)
        fun onModeChange(mode: Int)
    }

    private fun Float.dpToPx(): Float = (this * Resources.getSystem().displayMetrics.density)

}


