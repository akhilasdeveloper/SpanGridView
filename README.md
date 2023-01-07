# SpanGridView

SpanGridView is an android view component which displays a grid and can plots points on the given coordinate.

https://user-images.githubusercontent.com/73016717/211165779-3b39a033-ff36-442e-8c36-fc8f9fbbfbaf.mp4


## Installation

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Step 2. Add the dependency

```groovy
dependencies {
	        implementation 'com.github.akhilasdeveloper:SpanGridView:Tag'
	}
```

## Usage

Add it in the xml as below

```xml
<com.akhilasdeveloper.span_grid_view.SpanGridView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:id="@+id/gridViewHolder"
        app:gridColor="#1B4913"
        app:lineColor="#DFDFDF"
        app:lineWidth="2"
        app:scale=".5" 
        app:brushSize="1"
        app:enableLine="true"
        app:enableScale="true"
        app:enableSpan="true" />
```

Attributes and functions

```kotlin

spanGridView.gridHeight //Returns Grid height of current screen
spanGridView.gridWidth //Returns Grid width of current screen
spanGridView.brushSize // set/get brush size [1, 2, 3].
spanGridView.drawEnabled // enable/disable drawing [true, false]
spanGridView.gridColor // set/get grid background color
spanGridView.lineColor // set/get grid line color
spanGridView.lineEnabled // enable/disable grid line [true, false]
spanGridView.scaleEnabled // enable/disable grid zooming/scaling
spanGridView.spanEnabled // enable/disable grid spanning
spanGridView.lineWidth // set grid line width
spanGridView.mode // get/set current mode [MODE_VIEW, MODE_DRAW]

//MODE_VIEW: spanning, pinch zooming of the grid view is allowed. spanning can be performed using one finger. Drawing will be disabled. 

//MODE_DRAW: Drawing will be allowed. Spanning using 2 fingers is allowed. Pinch zooming is allowed.

spanGridView.pointsOnScreen //Returns the list of coordinates of the drawn points which are currently visible on the screen.
spanGridView.scale // set/get scale [0.0f to 1.0f] of the grid.
spanGridView.startPoint //Returns the coordinate of the top left point on the current screen
spanGridView.plotPoint(Point(2,3), Color.RED) //Draws a point on the grid position (2,3) with color red.
spanGridView.clearPoint(Point(3,5)) //Clear the drawn point on the grid position (3,5)
spanGridView.clearData() //Clear all drawn points


        spanGridView.setGridSelectListener(eventListener = object: SpanGridView.OnGridSelectListener{

            /**
             * px is the touched grid cell
             */
            override fun onDraw(px: Point) {
                spanGridView.plotPoint(px, Color.RED) // plots a point on position px with color red.
            }

            /**
             * Returns the current mode [MODE_VIEW, MODE_DRAW]
             */
            override fun onModeChange(mode: Int) {

            }
        })

```

## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.

