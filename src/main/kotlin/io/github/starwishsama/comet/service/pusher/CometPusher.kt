package io.github.starwishsama.comet.service.pusher

import cn.hutool.core.util.RandomUtil
import io.github.starwishsama.comet.BotVariables
import io.github.starwishsama.comet.service.pusher.config.EmptyPusherConfig
import io.github.starwishsama.comet.service.pusher.config.PusherConfig
import io.github.starwishsama.comet.service.pusher.context.PushContext
import io.github.starwishsama.comet.service.pusher.context.PushStatus
import io.github.starwishsama.comet.utils.TaskUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import java.util.concurrent.TimeUnit

/**
 * [CometPusher]
 */
abstract class CometPusher(val bot: Bot, val name: String) {
    open var config: PusherConfig = EmptyPusherConfig()

    abstract val cachePool: MutableList<PushContext>

    val duration: Long = config.interval

    var callTime: Int = 0

    var pushTime: Int = 0

    abstract fun retrieve()

    open fun push() {
        cachePool.forEach { context ->
            if (context.status == PushStatus.READY) {
                context.getPushTarget().forEach group@{
                    try {
                        val group = bot.getGroup(it) ?: return@group

                        runBlocking {
                            group.sendMessage(context.toMessageWrapper().toMessageChain(group))
                            delay(RandomUtil.randomLong(1000, 2000))
                        }
                    } catch (e: Exception) {
                        BotVariables.daemonLogger.warning("在推送开播消息至群 $it 时出现异常", e)
                    }
                }

                context.clearPushTarget()
                context.status = PushStatus.FINISHED
            }
        }
    }

    fun start() {
        TaskUtil.runScheduleTaskAsync(duration, duration, TimeUnit.SECONDS) {
            retrieve()
            push()
        }
    }

    fun addPushTime(){
        pushTime += 1
    }

    fun resetPushTime() {
        pushTime = 0
    }
}