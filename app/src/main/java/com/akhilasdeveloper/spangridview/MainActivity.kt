package com.akhilasdeveloper.spangridview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.akhilasdeveloper.span_grid_view.SpanGridView
import com.akhilasdeveloper.span_grid_view.models.Point
import com.akhilasdeveloper.spangridview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private var isErase = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val spanGridView = binding.gridViewHolder

        spanGridView.setGridSelectListener(eventListener = object :
            SpanGridView.OnGridSelectListener {

            /**
             * px is the touched grid cell
             */
            override fun onDraw(px: Point) {
                if (isErase)
                spanGridView.clearPoint(
                    px
                ) // plots a point on position px with color red.
                else
                    spanGridView.plotPoint(
                        px,
                        Color.argb(255,0,50,100)
                    )
            }

            /**
             * Returns the current mode [MODE_VIEW, MODE_DRAW]
             */
            override fun onModeChange(mode: Int) {

            }
        })

        binding.button.setOnClickListener {
            isErase = !isErase
        }
    }
}