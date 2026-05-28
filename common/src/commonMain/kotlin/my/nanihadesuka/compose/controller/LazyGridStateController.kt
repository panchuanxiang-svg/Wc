package my.nanihadesuka.compose.controller

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.ScrollbarSelectionMode
import kotlin.math.ceil
import kotlin.math.floor

import androidx.compose.runtime.State as ComposeState

@Composable
internal fun rememberLazyGridStateController(
    state: LazyGridState,
    thumbMinLength: Float,
    thumbMaxLength: Float,
    alwaysShowScrollBar: Boolean,
    selectionMode: ScrollbarSelectionMode,
    orientation: Orientation
): LazyGridStateController {

    val coroutineScope = rememberCoroutineScope()

    val thumbMinLengthUpdated = rememberUpdatedState(thumbMinLength)
    val thumbMaxLengthUpdated = rememberUpdatedState(thumbMaxLength)
    val alwaysShowScrollBarUpdated = rememberUpdatedState(alwaysShowScrollBar)
    val selectionModeUpdated = rememberUpdatedState(selectionMode)
    val orientationUpdated = rememberUpdatedState(orientation)

    val isSelected = remember { mutableStateOf(false) }
    val dragOffset = remember { mutableFloatStateOf(0f) }

    val realFirstVisibleItem = remember {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.firstOrNull {
                it.index == state.firstVisibleItemIndex
            }
        }
    }

    val nElementsMainAxis = remember {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo
                .maxOfOrNull {
                    if (orientation == Orientation.Vertical) {
                        it.column
                    } else {
                        it.row
                    }
                }
                ?.plus(1)
                ?: 1
        }
    }

    val isStickyHeaderInAction = remember {
        derivedStateOf {
            val realIndex =
                realFirstVisibleItem.value?.index
                    ?: return@derivedStateOf false

            val firstVisibleIndex =
                state.layoutInfo.visibleItemsInfo
                    .firstOrNull()
                    ?.index
                    ?: return@derivedStateOf false

            realIndex != firstVisibleIndex
        }
    }

    fun LazyGridItemInfo.fractionHiddenTop(
        firstItemOffset: Int
    ): Float {
        return when (orientationUpdated.value) {
            Orientation.Vertical -> {
                if (size.height == 0) {
                    0f
                } else {
                    firstItemOffset / size.height.toFloat()
                }
            }

            Orientation.Horizontal -> {
                if (size.width == 0) {
                    0f
                } else {
                    firstItemOffset / size.width.toFloat()
                }
            }
        }
    }

    fun LazyGridItemInfo.fractionVisibleBottom(
        viewportEndOffset: Int
    ): Float {
        return when (orientationUpdated.value) {
            Orientation.Vertical -> {
                if (size.height == 0) {
                    0f
                } else {
                    (viewportEndOffset - offset.y).toFloat() /
                            size.height.toFloat()
                }
            }

            Orientation.Horizontal -> {
                if (size.width == 0) {
                    0f
                } else {
                    (viewportEndOffset - offset.x).toFloat() /
                            size.width.toFloat()
                }
            }
        }
    }

    val thumbSizeNormalizedReal = remember {
        derivedStateOf {

            val layoutInfo = state.layoutInfo

            if (layoutInfo.totalItemsCount == 0) {
                return@derivedStateOf 0f
            }

            val firstItem =
                realFirstVisibleItem.value
                    ?: return@derivedStateOf 0f

            val firstPartial =
                firstItem.fractionHiddenTop(
                    state.firstVisibleItemScrollOffset
                )

            val lastPartial =
                1f - (
                        layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.fractionVisibleBottom(
                                layoutInfo.viewportEndOffset
                            )
                            ?: 0f
                        )

            val realSize =
                ceil(
                    layoutInfo.visibleItemsInfo.size.toFloat() /
                            nElementsMainAxis.value.toFloat()
                ) - if (isStickyHeaderInAction.value) {
                    1f
                } else {
                    0f
                }

            val realVisibleSize =
                realSize - firstPartial - lastPartial

            realVisibleSize /
                    ceil(
                        layoutInfo.totalItemsCount.toFloat() /
                                nElementsMainAxis.value.toFloat()
                    )
        }
    }

    val thumbSizeNormalized = remember {
        derivedStateOf {
            thumbSizeNormalizedReal.value.coerceIn(
                thumbMinLengthUpdated.value,
                thumbMaxLengthUpdated.value
            )
        }
    }

    val thumbOffsetNormalized = remember {
        derivedStateOf {

            val layoutInfo = state.layoutInfo

            if (
                layoutInfo.totalItemsCount == 0 ||
                layoutInfo.visibleItemsInfo.isEmpty()
            ) {
                return@derivedStateOf 0f
            }

            val firstItem =
                realFirstVisibleItem.value
                    ?: return@derivedStateOf 0f

            (
                ceil(
                    firstItem.index.toFloat() /
                            nElementsMainAxis.value.toFloat()
                ) +
                        firstItem.fractionHiddenTop(
                            state.firstVisibleItemScrollOffset
                        )
                ) /
                    ceil(
                        layoutInfo.totalItemsCount.toFloat() /
                                nElementsMainAxis.value.toFloat()
                    )
        }
    }

    val thumbIsInAction = remember {
        derivedStateOf {
            state.isScrollInProgress ||
                    isSelected.value ||
                    alwaysShowScrollBarUpdated.value
        }
    }

    return remember {
        LazyGridStateController(
            thumbSizeNormalized = thumbSizeNormalized,
            thumbOffsetNormalized = thumbOffsetNormalized,
            thumbIsInAction = thumbIsInAction,
            isSelected = isSelected,
            dragOffset = dragOffset,
            selectionMode = selectionModeUpdated,
            realFirstVisibleItem = realFirstVisibleItem,
            thumbSizeNormalizedReal = thumbSizeNormalizedReal,
            thumbMinLength = thumbMinLengthUpdated,
            orientation = orientationUpdated,
            nElementsMainAxis = nElementsMainAxis,
            state = state,
            coroutineScope = coroutineScope
        )
    }
}

internal class LazyGridStateController(
    override val thumbSizeNormalized: ComposeState<Float>,
    override val thumbOffsetNormalized: ComposeState<Float>,
    override val thumbIsInAction: ComposeState<Boolean>,
    override val isSelected: MutableState<Boolean>,
    private val dragOffset: MutableFloatState,
    private val selectionMode: ComposeState<ScrollbarSelectionMode>,
    private val realFirstVisibleItem: ComposeState<LazyGridItemInfo?>,
    private val thumbSizeNormalizedReal: ComposeState<Float>,
    private val thumbMinLength: ComposeState<Float>,
    private val orientation: ComposeState<Orientation>,
    private val nElementsMainAxis: ComposeState<Int>,
    private val state: LazyGridState,
    private val coroutineScope: CoroutineScope
) : StateController<Int> {

    override fun indicatorValue(): Int {
        return state.firstVisibleItemIndex
    }

    override fun onDraggableState(
        deltaPixels: Float,
        maxLengthPixels: Float
    ) {

        if (!isSelected.value) {
            return
        }

        setScrollOffset(
            dragOffset.floatValue +
                    deltaPixels / maxLengthPixels
        )
    }

    override fun onDragStarted(
        offsetPixels: Float,
        maxLengthPixels: Float
    ) {

        if (maxLengthPixels <= 0f) {
            return
        }

        val newOffset =
            offsetPixels / maxLengthPixels

        val currentOffset =
            thumbOffsetNormalized.value

        when (selectionMode.value) {

            ScrollbarSelectionMode.Full -> {

                if (
                    newOffset in
                    currentOffset..(
                            currentOffset +
                                    thumbSizeNormalized.value
                            )
                ) {
                    setDragOffset(currentOffset)
                } else {
                    setScrollOffset(newOffset)
                }

                isSelected.value = true
            }

            ScrollbarSelectionMode.Thumb -> {

                if (
                    newOffset in
                    currentOffset..(
                            currentOffset +
                                    thumbSizeNormalized.value
                            )
                ) {
                    setDragOffset(currentOffset)
                    isSelected.value = true
                }
            }

            ScrollbarSelectionMode.Disabled -> Unit
        }
    }

    override fun onDragStopped() {
        isSelected.value = false
    }

    private fun setScrollOffset(
        newOffset: Float
    ) {

        setDragOffset(newOffset)

        val totalItemsCount =
            ceil(
                state.layoutInfo.totalItemsCount.toFloat() /
                        nElementsMainAxis.value.toFloat()
            )

        val exactIndex =
            offsetCorrectionInverse(
                totalItemsCount *
                        dragOffset.floatValue
            )

        val index =
            floor(exactIndex).toInt() *
                    nElementsMainAxis.value

        val remainder =
            exactIndex - floor(exactIndex)

        coroutineScope.launch {

            state.scrollToItem(
                index = index,
                scrollOffset = 0
            )

            val offset =
                realFirstVisibleItem.value
                    ?.size
                    ?.let {

                        val size =
                            if (
                                orientation.value ==
                                Orientation.Vertical
                            ) {
                                it.height
                            } else {
                                it.width
                            }

                        (size.toFloat() * remainder)
                            .toInt()
                    }
                    ?: 0

            state.scrollToItem(
                index = index,
                scrollOffset = offset
            )
        }
    }

    private fun setDragOffset(
        value: Float
    ) {

        val maxValue =
            (
                    1f -
                            thumbSizeNormalized.value
                    )
                .coerceAtLeast(0f)

        dragOffset.floatValue =
            value.coerceIn(0f, maxValue)
    }

    private fun offsetCorrectionInverse(
        top: Float
    ): Float {

        if (
            thumbSizeNormalizedReal.value >=
            thumbMinLength.value
        ) {
            return top
        }

        val topRealMax =
            1f - thumbSizeNormalizedReal.value

        val topMax =
            1f - thumbMinLength.value

        return top * topRealMax / topMax
    }
}
