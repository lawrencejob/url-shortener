package com.lawrencejob.unit

import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisAsyncCommandsImpl
import io.lettuce.core.RedisConnectionStateListener
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.push.PushListener
import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.protocol.RedisCommand
import io.lettuce.core.resource.ClientResources
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * Minimal RedisAsyncCommands implementation that never talks to a real Redis instance.
 *
 * The service under test short-circuits before dispatching any Redis commands when invalid aliases
 * are supplied, so this lightweight mock is sufficient for exercising validation logic.
 */
class MockRedisAsyncCommands :
    RedisAsyncCommandsImpl<String, String>(MockStatefulRedisConnection(), StringCodec.UTF8)

private class MockStatefulRedisConnection : StatefulRedisConnection<String, String> {
    private var timeout: Duration = Duration.ofSeconds(60)
    private val clientResources: ClientResources = ClientResources.create()

    override fun addListener(listener: RedisConnectionStateListener) {
        // No-op: connection state listeners are unsupported in this mock.
    }

    override fun removeListener(listener: RedisConnectionStateListener) {
        // No-op
    }

    override fun setTimeout(timeout: Duration) {
        this.timeout = timeout
    }

    override fun getTimeout(): Duration = timeout

    override fun <T> dispatch(command: RedisCommand<String, String, T>): RedisCommand<String, String, T> {
        error("Mock connection cannot dispatch Redis commands")
    }

    override fun dispatch(
        commands: MutableCollection<out RedisCommand<String, String, *>>
    ): MutableCollection<RedisCommand<String, String, *>> {
        error("Mock connection cannot dispatch Redis commands")
    }

    override fun close() {
        // Nothing to release.
    }

    override fun closeAsync(): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

    override fun isOpen(): Boolean = true

    override fun getOptions(): ClientOptions = ClientOptions.create()

    override fun getResources(): ClientResources = clientResources

    @Deprecated("Deprecated in lettuce")
    override fun reset() {
        // No-op
    }

    override fun setAutoFlushCommands(autoFlush: Boolean) {
        // No-op
    }

    override fun flushCommands() {
        // No-op
    }

    override fun isMulti(): Boolean = false

    override fun sync(): RedisCommands<String, String> =
        error("Synchronous API not supported by mock connection")

    override fun async(): RedisAsyncCommands<String, String> =
        error("Async API access from the connection is not supported")

    override fun reactive(): RedisReactiveCommands<String, String> =
        error("Reactive API not supported by mock connection")

    override fun addListener(listener: PushListener) {
        // No-op
    }

    override fun removeListener(listener: PushListener) {
        // No-op
    }
}
