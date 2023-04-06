package com.tangem.sdk.ui.widget.leapfrogWidget

import android.view.View

/**
 * Created by Anton Zhilenkov on 07/10/2021.
 */
class LeapView(
    val view: View,
    val index: Int,
    position: Int,
    private val maximalPosition: Int,
    private val calculator: PropertyCalculator,
) {
    var state: LeapViewState
        private set

    val initialState: LeapViewState

    init {
        val initialProperties = createInitialProperty(position)
        state = LeapViewState(index, position, position, initialProperties)
        initialState = state.copy()
    }

    fun initView() {
        applyState(initialState)
    }

    fun leap(): LeapFrogAnimation {
        val previousPosition = state.currentPosition
        val currentPosition = when (previousPosition) {
            0 -> maximalPosition
            else -> state.currentPosition - 1
        }

        state = state.copy(
            currentPosition = currentPosition,
            previousPosition = previousPosition,
            properties = createLeapAnimationProperty(previousPosition, currentPosition),
        )

        return when {
            previousPosition == 0 && currentPosition == maximalPosition -> LeapFrogAnimation.LEAP
            else -> LeapFrogAnimation.PULL
        }
    }

    fun leapBack(): LeapFrogAnimation {
        val previousPosition = state.currentPosition
        val currentPosition = when (previousPosition) {
            maximalPosition -> 0
            else -> state.currentPosition + 1
        }
        state = state.copy(
            currentPosition = currentPosition,
            previousPosition = previousPosition,
            properties = createLeapAnimationProperty(previousPosition, currentPosition),
        )

        return when {
            previousPosition == maximalPosition && currentPosition == 0 -> LeapFrogAnimation.LEAP
            else -> LeapFrogAnimation.PULL
        }
    }

    fun fold() {
        state = state.copy(
            properties = state.properties.toFold(),
        )
    }

    fun unfold() {
        state = state.copy(properties = state.properties.toUnfold(calculator))
    }

    fun applyState(state: LeapViewState) {
        this.state = state
        setProperties(this.state.properties)
    }

    private fun setProperties(properties: LeapViewProperties) {
        view.elevation = properties.elevationEnd
        view.translationY = properties.yTranslation
        view.scaleX = properties.scale
        view.scaleY = properties.scale
//        initialProperties.hasForeground
    }

    private fun createInitialProperty(endPosition: Int): LeapViewProperties {
        return LeapViewProperties(
            endPosition,
            endPosition,
            calculator.scale(endPosition),
            calculator.elevation(endPosition, maximalPosition + 1),
            calculator.elevation(endPosition, maximalPosition + 1),
            calculator.yTranslation(0),
            calculator.hasForeground(endPosition),
        )
    }

    private fun createLeapAnimationProperty(startPosition: Int, endPosition: Int): LeapViewProperties {
        return LeapViewProperties(
            startPosition,
            endPosition,
            calculator.scale(endPosition),
            calculator.elevation(startPosition, maximalPosition + 1),
            calculator.elevation(endPosition, maximalPosition + 1),
            calculator.yTranslation(endPosition),
            calculator.hasForeground(endPosition),
        )
    }

    private fun LeapViewProperties.toUnfold(calculator: PropertyCalculator): LeapViewProperties {
        return this.copy(yTranslation = calculator.yTranslation(this.positionEnd))
    }

    private fun LeapViewProperties.toFold(): LeapViewProperties {
        return this.copy(yTranslation = 0f)
    }

    override fun toString(): String = "index: $index, position: ${state.currentPosition}, code: ${view.hashCode()}"
}

data class LeapViewState(
    val index: Int,
    val currentPosition: Int,
    val previousPosition: Int,
    val properties: LeapViewProperties,
)

enum class LeapFrogAnimation {
    LEAP, PULL
}

data class LeapViewProperties(
    val positionStart: Int,
    val positionEnd: Int,
    val scale: Float,
    val elevationStart: Float,
    val elevationEnd: Float,
    val yTranslation: Float,
    val hasForeground: Boolean,
)
