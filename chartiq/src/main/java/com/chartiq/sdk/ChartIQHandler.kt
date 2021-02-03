package com.chartiq.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import com.chartiq.sdk.adapters.StudyEntityClassTypeAdapter
import com.chartiq.sdk.model.*
import com.chartiq.sdk.model.charttype.ChartAggregationType
import com.chartiq.sdk.model.charttype.ChartType
import com.chartiq.sdk.model.drawingtool.DrawingParameterType
import com.chartiq.sdk.model.drawingtool.DrawingTool
import com.chartiq.sdk.model.study.*
import com.chartiq.sdk.scriptmanager.ChartIQScriptManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.*

@SuppressLint("SetJavaScriptEnabled")
class ChartIQHandler(
    private val chartIQUrl: String,
    context: Context,
) : ChartIQ, ChartIQStudy, JavaScriptHandler {

    private var dataSource: DataSource? = null
    private val scriptManager = ChartIQScriptManager()
    private val chartIQView = ChartIQView(context)
    private var measureCallback: MeasureCallback? = null

    override val chartView: View
        get() = chartIQView

    init {
        chartIQView.apply {
            with(settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            addJavascriptInterface(this@ChartIQHandler, JAVASCRIPT_INTERFACE_QUOTE_FEED)
            addJavascriptInterface(this@ChartIQHandler, JAVASCRIPT_INTERFACE_PARAMETERS)
            loadUrl(chartIQUrl)
        }
    }

    override fun start(onStartCallback: OnStartCallback) {
        chartIQView.apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    executeJavascript(scriptManager.getDetermineOSScript())
//                    executeJavascript(scriptManager.getNativeQuoteFeedScript())//todo comment until it is needed
                    executeJavascript(scriptManager.getAddDrawingListenerScript())
                    executeJavascript(scriptManager.getAddLayoutListenerScript())
                    executeJavascript(scriptManager.getAddMeasureListener())
                    onStartCallback.onStart()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d(TAG, consoleMessage?.message() ?: "Undefined JS exception")
                    return super.onConsoleMessage(consoleMessage)
                }
            }
        }
    }

    @JavascriptInterface
    override fun layoutChange(json: String) = Unit

    @JavascriptInterface
    override fun drawingChange(json: String) = Unit

    @JavascriptInterface
    override fun measureChange(json: String) {
        measureCallback?.onMeasureUpdate(json.substring(1, json.length - 1))
    }

    @JavascriptInterface
    override fun pullInitialData(
        symbol: String?,
        period: String?,
        interval: String?,
        start: String?,
        end: String?,
        meta: String?,
        callbackId: String?
    ) {
        val quoteFeedParams =
            QuoteFeedParams(symbol, period?.toInt(), interval, start, end, meta, callbackId)
        dataSource?.pullInitialData(quoteFeedParams) { data ->
            callbackId?.let {
                invokePullCallback(callbackId, data)
            }
        }
    }

    @JavascriptInterface
    override fun pullUpdate(
        symbol: String?,
        period: String?,
        interval: String?,
        start: String?,
        meta: String?,
        callbackId: String?
    ) {
        val quoteFeedParams =
            QuoteFeedParams(symbol, period?.toInt(), interval, start, null, meta, callbackId)
        dataSource?.pullUpdateData(quoteFeedParams) { data ->
            callbackId?.let {
                invokePullCallback(callbackId, data)
            }
        }
    }

    @JavascriptInterface
    override fun pullPagination(
        symbol: String?,
        period: String?,
        interval: String?,
        start: String?,
        end: String?,
        meta: String?,
        callbackId: String?
    ) {
        val quoteFeedParams =
            QuoteFeedParams(symbol, period?.toInt(), interval, start, end, meta, callbackId)
        dataSource?.pullPaginationData(quoteFeedParams) { data ->
            callbackId?.let {
                invokePullCallback(callbackId, data)
            }
        }
    }

    override fun setSymbol(symbol: String) {
        if (chartIQView.accessibilityManager.isEnabled && chartIQView.accessibilityManager.isTouchExplorationEnabled) {
            executeJavascript(scriptManager.getSetAccessibilityModeScript())
        }
        executeJavascript(scriptManager.getDateFromTickScript())
        executeJavascript(scriptManager.getSetSymbolScript(symbol))
    }

    override fun setDataSource(dataSource: DataSource) {
        this.dataSource = dataSource
    }

    override fun setDataMethod(method: DataMethod, symbol: String) {
        when (method) {
            DataMethod.PUSH -> executeJavascript(scriptManager.getLoadChartScript())
            DataMethod.PULL -> Log.d(
                javaClass.simpleName,
                "If you want to add a QuoteFeed please do so in your javascript code."
            )
        }
    }

    override fun enableCrosshairs() {
        executeJavascript(scriptManager.getEnableCrosshairScript(true))
    }

    override fun disableCrosshairs() {
        executeJavascript(scriptManager.getEnableCrosshairScript(false))
    }

    override fun setPeriodicity(period: Int, interval: String, timeUnit: TimeUnit) {
        val unit = timeUnit.toString().toLowerCase(Locale.ENGLISH)
        executeJavascript(scriptManager.getSetPeriodicityScript(period, interval, unit))
    }

    override fun enableDrawing(type: DrawingTool) {
        executeJavascript(scriptManager.getEnableDrawingScript(type))
    }

    override fun disableDrawing() {
        executeJavascript(scriptManager.getDisableDrawingScript())
    }

    override fun clearDrawing() {
        executeJavascript(scriptManager.getClearDrawingScript())
    }

    override fun getStudyList(callback: OnReturnCallback<List<Study>>) {
        executeJavascript(scriptManager.getGetStudyListScript()) { value: String ->
            val result = if (value.toLowerCase(Locale.ENGLISH) == "null") {
                "[]"
            } else {
                value
            }
            val objectResult = Gson().fromJson(result, Object::class.java)
            val typeToken = object : TypeToken<Map<String, StudyEntity>>() {}.type
            val studyList = Gson().fromJson<Map<String, StudyEntity>>(
                objectResult.toString(), typeToken
            ).map { (key, value) ->
                value.copy(shortName = key)
                    .toStudy()
            }
            callback.onReturn(studyList)
        }
    }

    override fun getActiveStudies(callback: OnReturnCallback<List<Study>>) {
        executeJavascript(scriptManager.getGetActiveStudiesScript()) { value ->
            val result = if (value.toLowerCase(Locale.ENGLISH) == "null") {
                "[]"
            } else {
                value
            }
            val typeToken = object : TypeToken<List<StudyEntity>>() {}.type

            val gson = GsonBuilder()
                .registerTypeAdapter(StudyEntity::class.java, StudyEntityClassTypeAdapter())
                .create()
            val response: List<StudyEntity> = gson.fromJson(result, typeToken)
            val studyList = response.map { it.toStudy() }
            callback.onReturn(studyList)
        }
    }

    override fun setAggregationType(aggregationType: ChartAggregationType) {
        val value = aggregationType.name.toLowerCase(Locale.ENGLISH)
        executeJavascript(scriptManager.getSetAggregationTypeScript(value))
    }

    override fun setChartType(chartType: ChartType) {
        executeJavascript(scriptManager.getSetChartTypeScript(chartType.name.toLowerCase(Locale.ENGLISH)))
    }

    override fun getChartType(callback: OnReturnCallback<ChartType?>) {
        val script = scriptManager.getChartTypeScript()
        executeJavascript(script) {
            if (it == "null") {
                callback.onReturn(null)
            } else {
                callback.onReturn(
                    ChartType.valueOf(
                        it.substring(1, it.length - 1)
                            .toUpperCase(Locale.ENGLISH)
                    )
                )
            }
        }
    }

    override fun getChartAggregationType(callback: OnReturnCallback<ChartAggregationType?>) {
        val script = scriptManager.getAggregationTypeScript()
        executeJavascript(script) { value ->
            if (value.isNullOrEmpty()) {
                callback.onReturn(null)
            } else {
                val parsedValue = value.substring(1, value.length - 1)
                    .toUpperCase(Locale.ENGLISH)
                val type = if (ChartAggregationType.values().any { it.name == parsedValue }) {
                    ChartAggregationType.valueOf(parsedValue)
                } else {
                    null
                }
                callback.onReturn(type)
            }
        }
    }

    override fun getChartScale(callback: OnReturnCallback<ChartScale>) {
        val script = scriptManager.getChartScaleScript()
        executeJavascript(script) {
            try {
                callback.onReturn(
                    ChartScale.valueOf(
                        it.substring(1, it.length - 1)
                            .toUpperCase()
                    )
                )
            } catch (e: Exception) {
                callback.onReturn(ChartScale.LINEAR)
            }
        }
    }

    override fun setChartScale(scale: ChartScale) {
        executeJavascript(scriptManager.getSetChartScaleScript(scale.value))
    }

    override fun removeStudy(study: Study) {
        executeJavascript(scriptManager.getRemoveStudyScript(study.name))
    }

    override fun addStudy(study: Study, forClone: Boolean) {
        val key = if (forClone) {
            study.type!!
        } else {
            study.shortName
        }
        val scripts = scriptManager.getAddStudyScript(key)
        executeJavascript(scripts)
    }

    override fun setStudyParameter(study: Study, parameter: StudyParameterModel) {
        val script = scriptManager.getSetStudyParameterScript(study.name, parameter)
        executeJavascript(script)
    }

    override fun setStudyParameters(study: Study, parameters: List<StudyParameterModel>) {
        executeJavascript(scriptManager.getSetStudyParametersScript(study.name, parameters))
    }

    override fun setDrawingParameter(parameterName: String, value: String) {
        executeJavascript(scriptManager.getSetDrawingParameterScript(parameterName, value))
    }

    override fun setDrawingParameter(parameter: DrawingParameterType, value: String) {
        executeJavascript(scriptManager.getSetDrawingParameterScript(parameter.value, value))
    }

    override fun getDrawingParameters(
        tool: DrawingTool,
        callback: OnReturnCallback<Map<String, Any>>
    ) {
        executeJavascript(scriptManager.getGetDrawingParametersScript(tool.value)) { value ->
            if (value != "null") {
                val result = value
                    .substring(1, value.length - 1)
                    .replace("\\", "")
                val typeToken = object : TypeToken<Map<String, Any>>() {}.type
                val parameters: Map<String, Any> = Gson().fromJson(result, typeToken)
                callback.onReturn(parameters)
            }
            if (tool == DrawingTool.NO_TOOL) {
                // undefined value is returned for notool drawing tool
                callback.onReturn(mapOf<String, String>())
            }
        }
    }

    override fun deleteDrawing() {
        executeJavascript(scriptManager.getDeleteDrawingScript())
    }

    override fun cloneDrawing() {
        executeJavascript(scriptManager.getCloneDrawingScript())
    }

    override fun manageLayer(layer: ChartLayer) {
        executeJavascript(scriptManager.getLayerManagementScript(layer))
    }

    override fun getStudyParameters(
        study: Study,
        type: StudyParameterType,
        callback: OnReturnCallback<List<StudyParameter>>
    ) {
        val script = when (type) {
            StudyParameterType.Inputs -> scriptManager.getStudyInputParametersScript(study.name)
            StudyParameterType.Outputs -> scriptManager.getStudyOutputParametersScript(study.name)
            StudyParameterType.Parameters -> scriptManager.getStudyParametersScript(study.name)
        }
        executeJavascript(script) { value ->
            val typeToken = object : TypeToken<List<StudyParameterEntity>>() {}.type
            val resultEntity = Gson().fromJson<List<StudyParameterEntity>>(value, typeToken)
            callback.onReturn(resultEntity
                .filter { entity -> entity.type in (ParameterEntityValueType.values().map { it.value } + null) }
                .map { it.toParameter(type) })
        }
    }

    override fun getIsInvertYAxis(callback: OnReturnCallback<Boolean>) {
        val script = scriptManager.getInvertYAxisScript()
        executeJavascript(script) {
            callback.onReturn(it.toBoolean())
        }
    }

    override fun setIsInvertYAxis(inverted: Boolean) {
        val script = scriptManager.getSetInvertYAxisScript(inverted)
        executeJavascript(script)
    }

    override fun getIsExtendedHours(callback: OnReturnCallback<Boolean>) {
        val script = scriptManager.getIsExtendedHoursScript()
        executeJavascript(script) {
            callback.onReturn(it.toBoolean())
        }
    }

    override fun setExtendedHours(extended: Boolean) {
        val script = scriptManager.getSetExtendedHoursScript(extended)
        executeJavascript(script)
    }

    override fun getHUDDetails(callback: OnReturnCallback<CrosshairHUD>) {
        executeJavascript(scriptManager.getGetCrosshairHUDDetailsScript()) { value ->
            if (value != "null") {
                val hud = Gson().fromJson(value, CrosshairHUD::class.java)
                callback.onReturn(hud)
            } else {
                callback.onReturn(CrosshairHUD("", "", "", "", "", ""))
            }
        }
    }

    override fun restoreDefaultDrawingConfig(tool: DrawingTool, all: Boolean) {
        executeJavascript(scriptManager.getRestoreDefaultDrawingConfigScript(tool, all))
    }

    override fun addMeasureListener(measureCallback: MeasureCallback) {
        this.measureCallback = measureCallback
    }


    override fun undoDrawing(callback: OnReturnCallback<Boolean>) {
        executeJavascript(scriptManager.getUndoDrawingScript())
    }

    override fun redoDrawing(callback: OnReturnCallback<Boolean>) {
        executeJavascript(scriptManager.getRedoDrawingScript())
    }

    override fun getTranslations(
        languageCode: String,
        callback: OnReturnCallback<Map<String, String>>
    ) {
        val script = scriptManager.getGetTranslationsScript(languageCode)
        executeJavascript(script) {
            if (it != "null") {
                val objectResult = Gson().fromJson(it, Object::class.java)
                val typeToken = object : TypeToken<Map<String, String>>() {}.type
                val translations = Gson().fromJson<Map<String, String>>(
                    objectResult.toString(), typeToken
                )
                callback.onReturn(translations)
            } else {
                callback.onReturn(emptyMap())
            }
        }
    }

    override fun setLanguage(languageCode: String) {
        executeJavascript(scriptManager.getSetLanguageScript(languageCode))
    }

    override fun setTheme(theme: ChartTheme) {
        executeJavascript(scriptManager.getSetThemeScript(theme))
    }

    private fun executeJavascript(script: String, callback: ValueCallback<String>? = null) {
        chartIQView.evaluateJavascript(script, callback)
    }

    private fun invokePullCallback(callbackId: String, data: List<OHLCParams>) {
        executeJavascript(scriptManager.getParseDataScript(data, callbackId))
    }

    override fun getActiveSeries(callback: OnReturnCallback<List<Series>>) {
        executeJavascript(scriptManager.getGetActiveSeriesScript()) { value ->
            val result = value
                .substring(1, value.length - 1)
                .replace("\\", "")
            val typeToken = object : TypeToken<Map<String, Map<String, String>>>() {}.type
            val seriesMap: Map<String, Any> = Gson().fromJson(result, typeToken)
            val seriesList = seriesMap.keys.mapTo(mutableListOf()) { key ->
                val hashColor = (seriesMap[key] as Map<String, String>)["color"]
                val color = hashColor?.let { color ->
                    color
                } ?: "#000000"
                Series(key, color)
            }
            callback.onReturn(seriesList)
        }
    }

    override fun addSeries(series: Series, isComparison: Boolean) {
        executeJavascript(scriptManager.getAddSeriesScript(series.symbolName, series.color, isComparison))
    }

    override fun removeSeries(symbolName: String) {
        executeJavascript(scriptManager.getRemoveSeriesScript(symbolName))
    }

    override fun setSeriesParameter(symbolName: String, parameterName: String, value: String) {
        executeJavascript(scriptManager.getSetSeriesParameterScript(symbolName, parameterName, value))
    }

    companion object {
        private const val JAVASCRIPT_INTERFACE_QUOTE_FEED = "QuoteFeed"
        private const val JAVASCRIPT_INTERFACE_PARAMETERS = "parameters"
        private val TAG = ChartIQHandler::class.java.simpleName
    }
}