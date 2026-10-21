package com.jinggong.timer.ui.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 从通知跳转到当前练习页的请求。 */
data class OpenPracticeRequest(
    val nonce: Long,
    val finish: Boolean,
)

/**
 * Activity 与 Compose 导航之间的轻量事件桥。
 *
 * 使用 StateFlow 而不是 SharedFlow，避免冷启动时 NavHost 尚未开始收集就丢失事件。
 */
class NavigationEvents {

    private val _openPractice = MutableStateFlow<OpenPracticeRequest?>(null)
    val openPractice: StateFlow<OpenPracticeRequest?> = _openPractice.asStateFlow()

    private val _pendingFinish = MutableStateFlow(false)
    val pendingFinish: StateFlow<Boolean> = _pendingFinish.asStateFlow()

    fun requestOpenPractice(finish: Boolean) {
        if (finish) _pendingFinish.value = true
        _openPractice.value = OpenPracticeRequest(System.nanoTime(), finish)
    }

    fun consumeOpenPractice() {
        _openPractice.value = null
    }

    /**
     * PracticeViewModel 消费结束请求。
     *
     * 返回 true 表示本次调用应进入结束小结。
     */
    fun consumePendingFinish(): Boolean {
        if (!_pendingFinish.value) return false
        _pendingFinish.value = false
        return true
    }
}
