/*
 * Copyright (c) 2019-2021 StarWishsama.
 *
 * 此源代码的使用受 GNU General Affero Public License v3.0 许可证约束, 欲阅读此许可证, 可在以下链接查看.
 *  Use of this source code is governed by the GNU AGPLv3 license which can be found through the following link.
 *
 * https://github.com/StarWishsama/Comet-Bot/blob/master/LICENSE
 *
 */

package io.github.starwishsama.comet.commands.chats

import io.github.starwishsama.comet.api.command.CommandProps
import io.github.starwishsama.comet.api.command.interfaces.ChatCommand
import io.github.starwishsama.comet.enums.UserLevel
import io.github.starwishsama.comet.managers.GroupConfigManager
import io.github.starwishsama.comet.objects.CometUser
import io.github.starwishsama.comet.service.command.MuteService
import io.github.starwishsama.comet.utils.CometUtil
import io.github.starwishsama.comet.utils.CometUtil.toChain
import io.github.starwishsama.comet.utils.StringUtil.convertToChain
import io.github.starwishsama.comet.utils.TaskUtil
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.EmptyMessageChain
import net.mamoe.mirai.message.data.MessageChain


class MuteCommand : ChatCommand {
    override suspend fun execute(event: MessageEvent, args: List<String>, user: CometUser): MessageChain {
        if (event is GroupMessageEvent && hasPermission(user, event)) {
            return if (event.group.botPermission.isOperator()) {
                if (args.isNotEmpty()) {
                    val at = CometUtil.parseAtAsBotUser(event, args[0])
                    if (at != null) {
                        MuteService.doMute(event.group, at.id, MuteService.getMuteTime(args[1]), false)
                    } else {
                        when (args[0]) {
                            "all", "全体", "全禁", "全体禁言" -> MuteService.doMute(
                                event.group,
                                -1,
                                -1,
                                true
                            )
                            "random", "rand", "随机", "抽奖" -> {
                                TaskUtil.runAsync(500) {
                                    runBlocking {
                                        MuteService.doRandomMute(event)
                                    }
                                }

                                "下面将抽取一位幸运群友禁言".toChain()
                            }
                            else -> getHelp().convertToChain()
                        }
                    }
                } else {
                    getHelp().convertToChain()
                }
            } else {
                "机器人需要管理员权限才能进行禁言!".toChain()
            }
        }
        return EmptyMessageChain
    }

    override fun getProps(): CommandProps =
        CommandProps("mute", arrayListOf("jy", "禁言"), "禁言", "nbot.commands.mute", UserLevel.USER)

    override fun getHelp(): String = """
        ======= 命令帮助 =======
        /mute [@/QQ/all] [禁言时长] 禁言
        /mute [rand/random/抽奖/随机] 随机抽取一位群友禁言
        时长为 0 时解禁
    """.trimIndent()

    override fun hasPermission(user: CometUser, e: MessageEvent): Boolean {
        if (e is GroupMessageEvent) {
            if (e.sender.permission >= MemberPermission.ADMINISTRATOR) return true
            val cfg = GroupConfigManager.getConfigOrNew(e.group.id)
            if (cfg.isHelper(e.sender.id)) return true
        }
        return user.hasPermission(getProps().permission)
    }
}