package com.kolecko.testofdatastore2

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.helper.StaticLabelsFormatter
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var graphView: GraphView
    private lateinit var updateGraphButton: Button
    private lateinit var yValuesTextView: TextView
    private lateinit var controller: MainController
    private lateinit var database: AppDatabase
    private var pointCounter: Int = 0
    private lateinit var lastAddedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        graphView = findViewById(R.id.graphView)
        updateGraphButton = findViewById(R.id.updateGraphButton)
        yValuesTextView = findViewById(R.id.yValuesTextView)

        database = AppDatabase.getInstance(this)
        controller = MainController(DataRepository(database.dataDao()))
        lastAddedDate = getCurrentDate()

        // Načtení aktuálních dat
        GlobalScope.launch {
            val currentDate = getCurrentDate()
            val dataEntity = controller.getDataByDate(currentDate)
            if (dataEntity != null) {
                // Pokud jsou data pro aktuální den k dispozici, použijte je pro inicializaci čítače bodů
                pointCounter = dataEntity.value.toInt()
            }

            // Aktualizace grafu
            updateGraph()
        }

        updateGraphButton.setOnClickListener {
            val currentDate = getCurrentDate()

            // Check if a new day has started
            if (currentDate != lastAddedDate) {
                pointCounter = 0
                lastAddedDate = currentDate
            }

            val value = pointCounter.toDouble()

            GlobalScope.launch {
                controller.insertOrUpdateData(currentDate, value)
                pointCounter++
                updateGraph()
            }
        }
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun createGraph(dataEntities: List<DataEntity>) {
        // Vytvoření bodů pro stupnicový graf
        val dataPoints = dataEntities.mapIndexed { index, dataEntity ->
            DataPoint(index.toDouble() + 1, dataEntity.value)
        }.toTypedArray()

        // Vytvoření série pro stupnicový graf
        val series = BarGraphSeries(dataPoints)

        // Nastavení viditelné oblasti grafu
        val maxX = dataEntities.size.toDouble() + 1
        graphView.viewport.setMinX(1.0)
        graphView.viewport.setMaxX(maxX)
        graphView.viewport.isXAxisBoundsManual = true

        // Nastavení viditelné oblasti osy Y
        val maxY = dataEntities.maxByOrNull { it.value }?.value ?: 0.0
        graphView.viewport.setMinY(0.0)
        graphView.viewport.setMaxY(maxY)
        graphView.viewport.isYAxisBoundsManual = true

        // Přidání série do grafu
        graphView.removeAllSeries()
        graphView.addSeries(series)

        /*
        // Nastavení popisků na ose X pomocí xLabels
        val xLabels = dataEntities.mapIndexed { index, dataEntity ->
            index.toDouble() + 1 to SimpleDateFormat("dd.MM").format(dataEntity.day.toLong())
        }.toMap().toMutableMap().toString()

        val staticLabelsFormatter = StaticLabelsFormatter(graphView)
        staticLabelsFormatter.setHorizontalLabels(xLabels)
        graphView.gridLabelRenderer.labelFormatter = staticLabelsFormatter
          */
        graphView.gridLabelRenderer.numHorizontalLabels = dataEntities.size
        graphView.gridLabelRenderer.setHorizontalLabelsAngle(45)
        graphView.gridLabelRenderer.textSize = 20f

        // Aktualizace TextView s hodnotami na ose Y
        updateYValuesTextView(dataEntities)
    }





    private fun updateYValuesTextView(dataEntities: List<DataEntity>) {
        val yValuesText = "Y Values: ${dataEntities.joinToString { it.value.toString() }}"
        yValuesTextView.text = yValuesText
    }

    private suspend fun updateGraph() {
        val dataEntities = database.dataDao().getAllData()

        withContext(Dispatchers.Main) {
            createGraph(dataEntities)
        }
    }
}
