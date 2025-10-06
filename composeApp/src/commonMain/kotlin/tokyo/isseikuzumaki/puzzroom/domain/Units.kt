package tokyo.isseikuzumaki.puzzroom.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline


@Serializable
@JvmInline
value class Centimeter(val value: Int) {
    init {
        require(value >= 0) { "Centimeter must be greater than 0" }
        require(value < Int.MAX_VALUE) { "Out of range " }
    }

    operator fun plus(other: Centimeter): Centimeter {
        return Centimeter(this.value + other.value)
    }

    operator fun minus(other: Centimeter): Centimeter {
        return Centimeter(this.value - other.value)
    }

    operator fun compareTo(other: Centimeter): Int {
        return this.value - other.value
    }

    override fun toString(): String {
        return "${value}cm"
    }

    companion object {
        fun Int.cm() = Centimeter(this)
    }
}

@Serializable
@JvmInline
value class Degree(val value: Float) {
    init {
        require(value >= 0f) { "Degree must be greater than 0" }
        require(value < 360f) { "Degree must be less than 360" }
    }

    operator fun plus(other: Degree): Degree {
        val newValue = (this.value + other.value) % 360f
        return Degree(newValue)
    }

    operator fun minus(other: Degree): Degree {
        val newValue = (this.value - other.value + 360f) % 360f
        return Degree(newValue)
    }

    operator fun compareTo(other: Degree): Int {
        return this.value.compareTo(other.value)
    }

    companion object {
        fun Float.degree(): Degree {
            var positive = this % 360f
            while (positive < 0f) {
                positive += 360f
            }
            return Degree(positive)
        }

        fun Int.degree(): Degree {
            var positive = this % 360
            while (positive < 0) {
                positive += 360
            }
            return Degree(positive.toFloat())
        }
    }
}
