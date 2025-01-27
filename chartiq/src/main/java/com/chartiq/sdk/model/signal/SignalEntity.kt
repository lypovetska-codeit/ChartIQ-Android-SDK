package com.chartiq.sdk.model.signal

import android.os.Parcelable
import com.chartiq.sdk.model.study.StudyEntity
import com.chartiq.sdk.model.study.toStudy
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SignalEntity(
    @Expose
    val uniqueId: String,
    @Expose
    @SerializedName(value = "name", alternate = ["signalName"])
    val signalName: String,
    val conditions: List<ConditionEntity>,
    @Expose
    val joiner: String,
    @Expose
    val description: String?,
    @Expose
    val disabled: Boolean,
    @Expose
    val studyEntity: StudyEntity?,
    @Expose
    val studyName: String
) : Parcelable

fun SignalEntity.toSignal(): Signal {
    return Signal(
        uniqueId = uniqueId,
        name = signalName,
        conditions = conditions.map { it.toCondition() },
        joiner = when (joiner) {
            "|" -> SignalJoiner.OR
            "&" -> SignalJoiner.AND
            else -> SignalJoiner.OR
        },
        description = description ?: "",
        disabled = disabled,
        study = studyEntity!!.toStudy()
    )
}

