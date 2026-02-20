package com.lifetracker.mobile.data.remote.dto

import com.lifetracker.mobile.domain.model.GameError
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable(with = TaskTypeSerializer::class)
enum class TaskType(val value: Int) {
    Habit(1),
    OneTime(2),
    Unknown(-1);

    companion object {
        fun fromValue(value: Int): TaskType =
            entries.firstOrNull { it.value == value } ?: Unknown
    }
}

@Serializable(with = TaskDifficultySerializer::class)
enum class TaskDifficulty(val value: Int) {
    Easy(1),
    Medium(2),
    Hard(3),
    Epic(4),
    Unknown(-1);

    companion object {
        fun fromValue(value: Int): TaskDifficulty =
            entries.firstOrNull { it.value == value } ?: Unknown
    }
}

abstract class IntEnumSerializer<T : Enum<T>>(
    serialName: String,
    private val fromValue: (Int) -> T,
    private val toValue: (T) -> Int,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName, PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: T): Unit =
        encoder.encodeInt(toValue(value))

    override fun deserialize(decoder: Decoder): T =
        fromValue(decoder.decodeInt())
}

internal object TaskTypeSerializer : IntEnumSerializer<TaskType>(
    "TaskType", TaskType::fromValue, { it.value }
)

internal object TaskDifficultySerializer : IntEnumSerializer<TaskDifficulty>(
    "TaskDifficulty", TaskDifficulty::fromValue, { it.value }
)