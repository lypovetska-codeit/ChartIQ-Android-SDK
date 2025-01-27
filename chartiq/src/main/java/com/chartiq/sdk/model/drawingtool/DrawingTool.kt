package com.chartiq.sdk.model.drawingtool

/**
 * An enumeration of available drawing tools
 */
enum class DrawingTool(
    /**
     * @suppress
     */
    val value: String?
) {
    ANNOTATION("annotation"),
    ARROW("arrow"),
    AVERAGE_LINE("average"),
    CALLOUT("callout"),
    CHANNEL("channel"),
    CHECK("check"),
    CONTINUOUS_LINE("continuous"),
    CROSS("xcross"),
    CROSSLINE("crossline"),
    DOODLE("freeform"),
    ELLIOTT_WAVE("elliottwave"),
    ELLIPSE("ellipse"),
    FIB_ARC("fibarc"),
    FIB_FAN("fibfan"),
    FIB_PROJECTION("fibprojection"),
    FIB_RETRACEMENT("retracement"),
    FIB_TIME_ZONE("fibtimezone"),
    FOCUS("focusarrow"),
    GANN_FAN("gannfan"),
    GARTLEY("gartley"),
    HEART("heart"),
    HORIZONTAL_LINE("horizontal"),
    LINE("line"),
    MEASURE("measure"),
    NO_TOOL(""),
    PITCHFORK("pitchfork"),
    QUADRANT_LINES("quadrant"),
    RAY("ray"),
    RECTANGLE("rectangle"),
    REGRESSION_LINE("regression"),
    SEGMENT("segment"),
    SPEED_RESISTANCE_ARC("speedarc"),
    SPEED_RESISTANCE_LINE("speedline"),
    STAR("star"),
    TIME_CYCLE("timecycle"),
    TIRONE_LEVELS("tirone"),
    TREND_LINE("trendline"),
    VERTICAL_LINE("vertical"),
    VOLUME_PROFILE("volumeprofile"),
    NONE(null)
}
