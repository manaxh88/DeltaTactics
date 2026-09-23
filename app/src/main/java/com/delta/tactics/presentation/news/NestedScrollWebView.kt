package com.delta.tactics.presentation.news

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.abs

/**
 * 嵌套滑动增强 WebView：解决 Compose ModalBottomSheet 内部 WebView 无法滚动长文章的问题
 * 1. 向上滑动浏览长文章与高清图报时，锁定父级拦截，让 WebView 内部全权流畅滚动
 * 2. 当网页滚动回最顶部 (scrollY == 0) 且手势向下拖动时，自动释放拦截，让父级 ModalBottomSheet 下拉收起关闭
 */
class NestedScrollWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : WebView(context, attrs, defStyleAttr), NestedScrollingChild3 {

    private val childHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this).apply {
        isNestedScrollingEnabled = true
    }

    private var lastY = 0
    private var initialDownY = 0f
    private var initialDownX = 0f
    private val scrollConsumed = IntArray(2)
    private val scrollOffset = IntArray(2)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val y = event.y.toInt()

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                initialDownY = event.rawY
                initialDownX = event.rawX
                lastY = y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
                // 默认拦截父级，确保点击与滑动事件优先由 WebView 响应
                parent?.requestDisallowInterceptTouchEvent(true)
                return super.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = lastY - y
                val diffY = event.rawY - initialDownY
                val diffX = event.rawX - initialDownX

                val isAtTop = scrollY <= 0
                val isPullingDown = diffY > touchSlop && abs(diffY) > abs(diffX)

                if (isAtTop && isPullingDown && deltaY < 0) {
                    // 处于正文最顶部且继续下拉：允许父级 ModalBottomSheet 接收嵌套滑动进行下拉收起
                    parent?.requestDisallowInterceptTouchEvent(false)
                    dispatchNestedScroll(0, 0, 0, deltaY, scrollOffset, ViewCompat.TYPE_TOUCH)
                    lastY = y
                    return true
                } else {
                    // 处于文章内部浏览或向上滑动看后续图文：独占触摸事件，确保 WebView 滚动自如
                    parent?.requestDisallowInterceptTouchEvent(true)
                }

                if (dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset, ViewCompat.TYPE_TOUCH)) {
                    lastY = y - scrollOffset[1]
                } else {
                    lastY = y
                }

                return super.onTouchEvent(event)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopNestedScroll(ViewCompat.TYPE_TOUCH)
                parent?.requestDisallowInterceptTouchEvent(false)
                return super.onTouchEvent(event)
            }
            else -> return super.onTouchEvent(event)
        }
    }

    // --- NestedScrollingChild3 委托方法 ---
    override fun startNestedScroll(axes: Int, type: Int): Boolean = childHelper.startNestedScroll(axes, type)
    override fun stopNestedScroll(type: Int) = childHelper.stopNestedScroll(type)
    override fun hasNestedScrollingParent(type: Int): Boolean = childHelper.hasNestedScrollingParent(type)
    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?, type: Int,
        consumed: IntArray
    ) = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed)
    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?, type: Int
    ): Boolean = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type)
    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int,
        consumed: IntArray?, offsetInWindow: IntArray?, type: Int
    ): Boolean = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

    // --- NestedScrollingChild 委托方法 ---
    override fun setNestedScrollingEnabled(enabled: Boolean) { childHelper.isNestedScrollingEnabled = enabled }
    override fun isNestedScrollingEnabled(): Boolean = childHelper.isNestedScrollingEnabled
    override fun startNestedScroll(axes: Int): Boolean = childHelper.startNestedScroll(axes)
    override fun stopNestedScroll() = childHelper.stopNestedScroll()
    override fun hasNestedScrollingParent(): Boolean = childHelper.hasNestedScrollingParent()
    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)
    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int,
        consumed: IntArray?, offsetInWindow: IntArray?
    ): Boolean = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
}
