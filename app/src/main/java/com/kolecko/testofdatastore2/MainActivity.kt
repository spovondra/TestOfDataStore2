package com.kolecko.testofdatastore2

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    private suspend fun createGraph(dataEntities: List<DataEntity>, formattedDateStrings: Array<String>) {
        // Vytvoření bodů pro stupnicový graf
        val dataPoints = dataEntities.mapIndexed { index, dataEntity ->
            DataPoint(index.toDouble() + 1, dataEntity.value)
        }.toTypedArray()

        // Vytvoření série pro stupnicový graf
        val series = BarGraphSeries(dataPoints)

        // Nastavení viditelné oblasti grafu
        val maxX = dataEntities.size.toDouble()
        graphView.viewport.setMinX(0.5)
        graphView.viewport.setMaxX(maxX + 0.5)
        graphView.viewport.isXAxisBoundsManual = true

        // Nastavení viditelné oblasti osy Y
        val maxY = dataEntities.maxByOrNull { it.value }?.value ?: 0.0
        graphView.viewport.setMinY(0.0)
        graphView.viewport.setMaxY(maxY)
        graphView.viewport.isYAxisBoundsManual = true

        // Přidání série do grafu
        graphView.removeAllSeries()
        graphView.addSeries(series)

        // Ensure there are at least two labels before setting them
        if (formattedDateStrings.size >= 2) {
            // Přidání aktuálního data do pole pro popisky na ose X
            val currentDate = getCurrentDate()
            val dataEntity = controller.getDataByDate(currentDate)
            val formattedCurrentDate = dataEntity?.formattedDate ?: ""
            val allFormattedDates = formattedDateStrings + arrayOf(formattedCurrentDate)

            // Nastavení popisků na ose X pomocí allFormattedDates
            val staticLabelsFormatter = StaticLabelsFormatter(graphView)
            staticLabelsFormatter.setHorizontalLabels(allFormattedDates)
            graphView.gridLabelRenderer.labelFormatter = staticLabelsFormatter
            graphView.gridLabelRenderer.setHorizontalLabelsAngle(35)
            graphView.gridLabelRenderer.labelHorizontalHeight = 70
            graphView.gridLabelRenderer.numHorizontalLabels = allFormattedDates.size
            graphView.gridLabelRenderer.textSize = 30f
        }

        // Aktualizace TextView s hodnotami na ose Y
        updateYValuesTextView(dataEntities)
    }

    private fun updateYValuesTextView(dataEntities: List<DataEntity>) {
        val yValuesText = "Y Values: ${dataEntities.joinToString { it.value.toString() }}"
        yValuesTextView.text = yValuesText
    }

    private suspend fun updateGraph() {
        val dataEntities = database.dataDao().getAllData()
        val formattedDateStrings = database.dataDao().getFormattedDates()

        withContext(Dispatchers.Main) {
            createGraph(dataEntities, formattedDateStrings)
        }
    }
}
