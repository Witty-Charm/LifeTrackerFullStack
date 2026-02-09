package com.lifetracker.mobile.data.remote.dto

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable(with = TaskTypeSerializer::class)
enum class TaskType(val value: Int) {
    Habit(1),
    OneTime(2);

    companion object {
        fun fromValue(value: Int): TaskType =
            entries.firstOrNull() { it.value == value }
                ?: throw SerializationException("Unknown TaskType: $value")
    }
}

internal object TaskTypeSerializer : KSerializer<TaskType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TaskType", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: TaskType): Unit =
        encoder.encodeInt(value.value)

    override fun deserialize(decoder: Decoder): TaskType =
        TaskType.fromValue(decoder.decodeInt())
}

@Serializable(with = TastDifficultySerializer::class)
enum class TaskDifficulty(val value: Int) {
    Easy(1),
    Medium(2),
    Hard(3),
    Epic(4);

    companion object {
        fun fromValue(value: Int): TaskDifficulty =
            entries.firstOrNull { it.value == value }
                ?: throw SerializationException("Unknown TaskDifficulty: $value")
    }
}

internal object TastDifficultySerializer : KSerializer<TaskDifficulty> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TaskDifficulty", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: TaskDifficulty): Unit =
        encoder.encodeInt(value.value)

    override fun deserialize(decoder: Decoder): TaskDifficulty =
        TaskDifficulty.fromValue(decoder.decodeInt())
}

