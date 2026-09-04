package com.realbuds.app.proto

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One error, as the UI needs it.
 *
 * [detail] is separate from [message] so the alert can lead with something
 * readable and keep the wire trace underneath, rather than putting a hex dump
 * in a dialog title.
 */
data class BudsError(
    val message: String,
    val detail: String? = null,
    val kind: Kind = Kind.PROTOCOL,
) {
    enum class Kind { CONNECTION, PROTOCOL, REFUSED, UNEXPECTED }

    /**
     * Text for the alert body, hard-capped.
     *
     * A malformed reply can carry an arbitrarily long hex dump, and an
     * unbounded string in a dialog either overflows the screen or pushes the
     * dismiss button out of reach. Truncation is marked so a clipped trace is
     * never mistaken for a complete one.
     */
    fun body(limit: Int = MAX_BODY): String {
        val full = if (detail.isNullOrBlank()) message else "$message\n\n$detail"
        return if (full.length <= limit) full
        else full.take(limit - ELLIPSIS.length) + ELLIPSIS
    }

    companion object {
        const val MAX_BODY = 400
        private const val ELLIPSIS = "\n… (truncated)"
    }
}

/**
 * Single funnel for everything that goes wrong.
 *
 * Every failure path — a dropped write, a refused command, a malformed reply,
 * a socket error — reports here instead of only writing a log line nobody
 * reads. The UI observes [current] and shows one alert at a time.
 *
 * Deliberately *not* a queue. If a connection drops mid-refresh, ten commands
 * fail at once; queueing would mean ten dialogs to dismiss. The first error
 * is held until acknowledged and later ones are dropped, because after the
 * first the rest are almost always the same cause.
 */
class ErrorBus {

    private val _current = MutableStateFlow<BudsError?>(null)
    val current: StateFlow<BudsError?> = _current.asStateFlow()

    /** Reported but not shown, so a dismissed burst can still be counted. */
    private val _suppressed = MutableStateFlow(0)
    val suppressed: StateFlow<Int> = _suppressed.asStateFlow()

    fun report(error: BudsError) {
        if (_current.value == null) _current.value = error
        else _suppressed.value += 1
    }

    fun report(
        message: String,
        detail: String? = null,
        kind: BudsError.Kind = BudsError.Kind.PROTOCOL,
    ) = report(BudsError(message, detail, kind))

    /**
     * A command the earbuds explicitly refused.
     *
     * Status is meaningful on this protocol only when non-zero: a status-0
     * reply proves the frame parsed, not that anything changed, so only
     * non-zero reaches here.
     */
    fun refused(what: String, status: Int, commandId: Int) = report(
        BudsError(
            message = "$what was refused by the earbuds.",
            detail = "status=$status  command=0x%04X".format(commandId),
            kind = BudsError.Kind.REFUSED,
        )
    )

    /** A reply that parsed as a frame but not as the payload we expected. */
    fun unexpected(what: String, commandId: Int, body: ByteArray) = report(
        BudsError(
            message = "Unexpected reply while reading $what.",
            detail = "command=0x%04X  %d bytes\n%s".format(
                commandId, body.size, BudsClient.hex(body),
            ),
            kind = BudsError.Kind.UNEXPECTED,
        )
    )

    fun connection(message: String, cause: String? = null) = report(
        BudsError(message, cause, BudsError.Kind.CONNECTION)
    )

    fun dismiss() {
        _current.value = null
        _suppressed.value = 0
    }
}
