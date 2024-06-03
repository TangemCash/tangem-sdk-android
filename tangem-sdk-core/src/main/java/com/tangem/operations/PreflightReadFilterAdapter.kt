package com.tangem.operations

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.operations.preflightread.PreflightReadFilter

internal class PreflightReadFilterAdapter : JsonAdapter<PreflightReadFilter>() {

    private val moshi: Moshi by lazy { MoshiJsonConverter.default().moshi }

    @FromJson
    override fun fromJson(reader: JsonReader): PreflightReadFilter? {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Any>()
            return null
        }

        val jsonAdapter = moshi.adapter<Any>(PreflightReadFilter::class.java)
        return jsonAdapter.fromJson(reader) as? PreflightReadFilter
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: PreflightReadFilter?) {
        if (value == null) {
            writer.nullValue()
        } else {
            val jsonAdapter = moshi.adapter<Any>(PreflightReadFilter::class.java)
            jsonAdapter.toJson(writer, value)
        }
    }
}
