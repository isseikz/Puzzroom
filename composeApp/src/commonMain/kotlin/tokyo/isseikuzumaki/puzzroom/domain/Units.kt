package tokyo.isseikuzumaki.puzzroom.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Centimeter(val value: Int): Comparable<Centimeter> {
    init {
        require(value < Int.MAX_VALUE) { "Out of range " }
    }

    fun toLong(): Long {
        return value.toLong()
    }

    operator fun plus(other: Centimeter): Centimeter {
        return Centimeter(this.value + other.value)
    }

    operator fun minus(other: Centimeter): Centimeter {
        return Centimeter(this.value - other.value)
    }

    override operator fun compareTo(other: Centimeter): Int {
        return this.value - other.value
    }

    operator fun compareTo(other: Length): Int {
        return this.value - other.cm.value
    }


    operator fun compareTo(other: Int): Int {
        return this.value - other
    }

    override fun toString(): String {
        return "${value}cm"
    }

    companion object Companion {
        fun Int.cm() = Centimeter(this)
        val Int.cm get() = Centimeter(this)
    }
}

@Serializable
@JvmInline
value class Length(val cm: Centimeter) {
    init {
        require(cm >= 0) { "Centimeter must be greater than 0" }
        require(cm < Int.MAX_VALUE) { "Out of range " }
    }

    constructor(cm: Int) : this(Centimeter(cm))

    operator fun plus(other: Length): Length {
        return Length(this.cm + other.cm)
    }

    operator fun minus(other: Length): Length {
        return Length(this.cm - other.cm)
    }

    operator fun compareTo(other: Length): Int {
        return (this - other).cm.value
    }

    override fun toString(): String {
        return "${cm}cm"
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
