package my.nanihadesuka.compose.controller

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.ScrollbarSelectionMode
import kotlin.math.ceil
import kotlin.math.floor

// 强制隔离 Compose State，避免和你项目里的 State 冲突
import androidx.compose.runtime.State as ComposeState

@Composable
internal fun rememberLazyStaggeredGridStateController(
    state: LazyStaggeredGridState,
    reverseLayout: Boolean,
    thumbMinLength: Float,
    thumbMaxLength: Float,
    alwaysShowScrollBar: Boolean,
    selectionMode: ScrollbarSelectionMode,
    orientation: Orientation
): LazyStaggeredGridStateController {

    val scope = rememberCoroutineScope()

    val thumbMin = rememberUpdatedState(thumbMinLength)
    val thumbMax = rememberUpdatedState(thumbMaxLength)
    val alwaysShow = rememberUpdatedState(alwaysShowScrollBar)
    val mode = rememberUpdatedState(selectionMode)
    val ori = rememberUpdatedState(orientation)

    val reverse = remember { derivedStateOf { reverseLayout } }

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
            var count = 0
            for (item in state.layoutInfo.visibleItemsInfo) {
                val lane = item.lane
                if (lane == -1) break
                if (count == lane) count++ else break
            }
            count.coerceAtLeast(1)
        }
    }

    val isStickyHeaderInAction = remember {
        derivedStateOf {
            val realIndex = realFirstVisibleItem.value?.index ?: return@derivedStateOf false
            val first = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                ?: return@derivedStateOf false
            realIndex != first
        }
    }

    fun LazyStaggeredGridItemInfo.fractionHiddenTop(offset: Int): Float {
        return when (ori.value) {
            Orientation.Vertical ->
                if (size.height == 0) 0f else offset / size.height.toFloat()
            Orientation.Horizontal ->
                if (size.width == 0) 0f else offset / size.width.toFloat()
        }
    }

    fun LazyStaggeredGridItemInfo.fractionVisibleBottom(end: Int): Float {
        return when (ori.value) {
            Orientation.Vertical ->
                if (size.height == 0) 0f else (end - offset.y) / size.height.toFloat()
            Orientation.Horizontal ->
                if (size.width == 0) 0f else (end - offset.x) / size.width.toFloat()
        }
    }

    val thumbSizeReal = remember {
        derivedStateOf {
            val info = state.layoutInfo
            if (info.totalItemsCount == 0) return@derivedStateOf 0f

            val first = realFirstVisibleItem.value ?: return@derivedStateOf 0f
            val firstPartial = first.fractionHiddenTop(state.firstVisibleItemScrollOffset)

            val lastPartial =
                1f - (info.visibleItemsInfo.lastOrNull()
                    ?.fractionVisibleBottom(info.viewportEndOffset) ?: 0f)

            val realSize =
                ceil(info.visibleItemsInfo.size.toFloat() / nElementsMainAxis.value.toFloat()) -
                        if (isStickyHeaderInAction.value) 1f else 0f

            val visible = realSize - firstPartial - lastPartial

            visible / ceil(info.totalItemsCount.toFloat() / nElementsMainAxis.value.toFloat())
        }
    }

    val thumbSize = remember {
        derivedStateOf {
            thumbSizeReal.value.coerceIn(thumbMin.value, thumbMax.value)
        }
    }

    fun correction(top: Float): Float {
        val max = (1f - thumbSizeReal.value).coerceIn(0f, 1f)

        return if (thumbSizeReal.value >= thumbMin.value) {
            if (reverse.value) max - top else top
        } else {
            val max2 = 1f - thumbMin.value
            if (reverse.value) (max - top) * max2 / max
            else top * max2 / max
        }
    }

    val thumbOffset = remember {
        derivedStateOf {
            val info = state.layoutInfo
            if (info.totalItemsCount == 0 || info.visibleItemsInfo.isEmpty()) return@derivedStateOf 0f

            val first = realFirstVisibleItem.value ?: return@derivedStateOf 0f

            val top =
                ceil(first.index.toFloat() / nElementsMainAxis.value.toFloat()) +
                        first.fractionHiddenTop(state.firstVisibleItemScrollOffset)

            correction(top / ceil(info.totalItemsCount.toFloat() / nElementsMainAxis.value.toFloat()))
        }
    }

    val thumbActive = remember {
        derivedStateOf {
            state.isScrollInProgress || isSelected.value || alwaysShow.value
        }
    }

    return remember {
        LazyStaggeredGridStateController(
            thumbSize,
            thumbOffset,
            thumbActive,
            isSelected,
            dragOffset,
            mode,
            realFirstVisibleItem,
            thumbSizeReal,
            thumbMin,
            reverse,
            ori,
            nElementsMainAxis,
            state,
            scope
        )
    }
}

internal class LazyStaggeredGridStateController(
    override val thumbSizeNormalized: ComposeState<Float>,
    override val thumbOffsetNormalized: ComposeState<Float>,
    override val thumbIsInAction: ComposeState<Boolean>,
    private val _isSelected: MutableState<Boolean>,
    private val dragOffset: MutableFloatState,
    private val selectionMode: ComposeState<ScrollbarSelectionMode>,
    private val realFirstVisibleItem: ComposeState<LazyStaggeredGridItemInfo?>,
    private val thumbSizeReal: ComposeState<Float>,
    private val thumbMin: ComposeState<Float>,
    private val reverse: ComposeState<Boolean>,
    private val orientation: ComposeState<Orientation>,
    private val nElementsMainAxis: ComposeState<Int>,
    private val state: LazyStaggeredGridState,
    private val scope: CoroutineScope
) : StateController<Int> {

    override val isSelected: MutableState<Boolean> = _isSelected

    override fun indicatorValue(): Int = state.firstVisibleItemIndex

    override fun onDraggableState(deltaPixels: Float, maxLengthPixels: Float) {
        val d = if (reverse.value) -deltaPixels else deltaPixels
        if (isSelected.value) {
            setScrollOffset(dragOffset.floatValue + d / maxLengthPixels)
        }
    }

    override fun onDragStarted(offsetPixels: Float, maxLengthPixels: Float) {
        if (maxLengthPixels <= 0f) return

        val newOffset =
            if (reverse.value)
                (maxLengthPixels - offsetPixels) / maxLengthPixels
            else offsetPixels / maxLengthPixels

        val current =
            if (reverse.value)
                1f - thumbOffsetNormalized.value - thumbSizeNormalized.value
            else thumbOffsetNormalized.value

        when (selectionMode.value) {
            ScrollbarSelectionMode.Full -> {
                if (newOffset in current..(current + thumbSizeNormalized.value)) {
                    setDragOffset(current)
                } else {
                    setScrollOffset(newOffset)
                }
                _isSelected.value = true
            }

            ScrollbarSelectionMode.Thumb -> {
                if (newOffset in current..(current + thumbSizeNormalized.value)) {
                    setDragOffset(current)
                    _isSelected.value = true
                }
            }

            ScrollbarSelectionMode.Disabled -> Unit
        }
    }

    override fun onDragStopped() {
        _isSelected.value = false
    }

    private fun setScrollOffset(offset: Float) {
        setDragOffset(offset)

        val total =
            ceil(state.layoutInfo.totalItemsCount.toFloat() / nElementsMainAxis.value.toFloat())

        val exact = offsetCorrectionInverse(total * dragOffset.floatValue)

        val index = floor(exact).toInt() * nElementsMainAxis.value
        val rem = exact - floor(exact)

        scope.launch {
            state.scrollToItem(index, 0)

            val itemSize = realFirstVisibleItem.value?.size
            val pixel = itemSize?.let {
                val s = if (orientation.value == Orientation.Vertical) it.height else it.width
                (s * rem).toInt()
            } ?: 0

            state.scrollToItem(index, pixel)
        }
    }

    private fun setDragOffset(v: Float) {
        val max = (1f - thumbSizeNormalized.value).coerceAtLeast(0f)
        dragOffset.floatValue = v.coerceIn(0f, max)
    }

    private fun offsetCorrectionInverse(top: Float): Float {
        if (thumbSizeReal.value >= thumbMin.value) return top

        val realMax = 1f - thumbSizeReal.value
        val max = 1f - thumbMin.value

        return top * realMax / max
    }
}
