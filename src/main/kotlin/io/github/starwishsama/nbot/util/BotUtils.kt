package io.github.starwishsama.nbot.util

import com.deadmandungeons.serverstatus.MinecraftServerStatus
import io.github.starwishsama.nbot.BotConstants
import io.github.starwishsama.nbot.BotInstance
import io.github.starwishsama.nbot.enums.UserLevel
import io.github.starwishsama.nbot.objects.BotUser
import io.github.starwishsama.nbot.objects.BotUser.Companion.isBotOwner
import java.io.IOException
import java.net.URISyntaxException
import java.time.LocalDateTime
import java.util.*

/**
 * 用于辅助机器人运行中的各种工具方法
 *
 * @author Nameless
 */

object BotUtils {
    private var coolDown: MutableMap<Long, Long> = HashMap()

    /**
     * @param string 需要去除彩色符号的字符串
     * @return 去除彩色符号的字符串
     */
    private fun removeColor(string: String): String {
        return string.replace("§\\S".toRegex(), "")
    }

    /**
     * 获取 Minecraft 服务器信息 (SRV解析)
     * @author NamelessSAMA
     * @param address 服务器地址
     * @return 服务器状态信息
     */
    fun getServerInfo(address: String): String {
        return try {
            val response = MinecraftServerStatus.pingServerStatus(address)
            """
     在线玩家: ${response.players}
     延迟:${response.latency}ms
     MOTD: ${removeColor(response.description.toString())}
     版本: ${response.version}
     """.trimIndent()
        } catch (e: IOException) {
            BotInstance.logger.warning("在获取服务器信息时出现了问题, $e")
            "Bot > 无法连接至 $address"
        } catch (e: URISyntaxException) {
            BotInstance.logger.warning("在获取服务器信息时出现了问题, $e")
            "Bot > 无法连接至 $address"
        }
    }

    /**
     * 获取 Minecraft 服务器信息 (非SRV解析)
     * @author NamelessSAMA
     * @param address 服务器地址
     * @param port 服务器端口
     * @return 服务器状态信息
     */
    fun getServerInfo(address: String, port: Int): String {
        return try {
            val response = MinecraftServerStatus.pingServerStatus(address, port)
            """
     在线玩家: ${response.players}
     延迟:${response.latency}ms
     MOTD: ${removeColor(response.description.toString())}
     版本: ${response.version}
     """.trimIndent()
        } catch (e: IOException) {
            BotInstance.logger.warning("在获取服务器信息时出现了问题, $e")
            "Bot > 无法连接至 $address"
        } catch (e: URISyntaxException) {
            BotInstance.logger.warning("在获取服务器信息时出现了问题, $e")
            "Bot > 无法连接至 $address"
        }
    }

    /**
     * 获取 Minecraft 服务器信息 (非SRV解析 + 自定义消息样式)
     * @param address 服务器IP
     * @param port 端口
     * @param msg 自定义消息
     * @return 服务器状态信息
     */
    fun getCustomServerInfo(address: String, port: Int, msg: String): String {
        return try {
            val response = MinecraftServerStatus.pingServerStatus(address, port)
            msg.replace("%延迟%".toRegex(), response.latency.toString() + "ms")
                .replace("%在线玩家%".toRegex(), response.players.toString())
                .replace("%换行%".toRegex(), "\n")
                .replace("%MOTD%".toRegex(), removeColor(response.description.toString()))
                .replace("%版本%".toRegex(), response.version.toString())
        } catch (e: IOException) {
            BotInstance.logger.warning("在获取服务器信息时出现了问题, $e")
            "Bot > 无法连接至 $address"
        } catch (e: URISyntaxException) {
            BotInstance.logger.warning("在获取服务器信息时出现了问题, $e")
            "Bot > 无法连接至 $address"
        }
    }

    /**
     * 获取 Minecraft 服务器信息 (SRV解析 + 自定义消息样式)
     * @param address 服务器IP
     * @param msg 自定义消息
     * @return 服务器状态信息
     */
    fun getCustomServerInfo(address: String, msg: String): String {
        return try {
            val response = MinecraftServerStatus.pingServerStatus(address)
            msg.replace("%延迟%".toRegex(), response.latency.toString() + "ms")
                .replace("%在线玩家%".toRegex(), response.players.toString())
                .replace("%换行%".toRegex(), "\n")
                .replace("%MOTD%".toRegex(), removeColor(response.description.toString()))
                .replace("%版本%".toRegex(), response.version.toString())
        } catch (e: IOException) {
            BotInstance.logger.warning("在获取服务器信息时出现了问题, $e")
            "Bot > 无法连接至 $address"
        } catch (e: URISyntaxException) {
            BotInstance.logger.warning("在获取服务器信息时出现了问题, $e")
            "Bot > 无法连接至 $address"
        }
    }

    /**
     * 判断是否签到过了
     *
     * @author NamelessSAMA
     * @param user 机器人账号
     * @return 是否签到
     */
    fun isChecked(user: BotUser): Boolean {
        val now = LocalDateTime.now()
        return if (user.lastCheckInTime.month == now.month
            && user.lastCheckInTime.dayOfMonth == now.dayOfMonth - 1
        ) {
            false
        } else user.lastCheckInTime.month >= now.month
    }

    /**
     * 判断指定QQ号是否仍在冷却中
     *
     * @author NamelessSAMA
     * @param qq 指定的QQ号
     * @return 目标QQ号是否处于冷却状态
     */
    fun isNoCoolDown(qq: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        if (qq == 80000000L) {
            return false
        }
        if (coolDown.containsKey(qq) && !isBotOwner(qq)) {
            if (currentTime - coolDown[qq]!! < BotConstants.cfg.coolDownTime * 1000) {
                return false
            } else {
                coolDown.remove(qq)
            }
        } else {
            coolDown[qq] = currentTime
        }
        return true
    }

    /**
     * 判断指定QQ号是否仍在冷却中
     * (可以自定义命令冷却时间)
     *
     * @author Nameless
     * @param qq 要检测的QQ号
     * @param seconds 自定义冷却时间
     * @return 目标QQ号是否处于冷却状态
     */
    fun isNoCoolDown(qq: Long, seconds: Int): Boolean {
        val currentTime = System.currentTimeMillis()
        if (qq == 80000000L) {
            return false
        }

        if (qq == BotConstants.cfg.ownerId){
            return false
        }

        if (coolDown.containsKey(qq) && !isBotOwner(qq)) {
            if (currentTime - coolDown[qq]!! < seconds * 1000) {
                return false
            } else {
                coolDown.remove(qq)
            }
        } else {
            coolDown[qq] = currentTime
        }
        return true
    }

    /**
     * 判断ID是否符合育碧账号昵称格式规范
     *
     * @author NamelessSAMA
     * @param username 用户名
     * @return 是否符合规范
     */
    fun isLegitId(username: String): Boolean {
        return username.matches(Regex("[a-zA-Z0-9_.-]*"))
    }

    /**
     * 获取本地化文本
     *
     * @author NamelessSAMA
     * @param node 本地化文本节点
     * @return 本地化文本
     */
    fun getLocalMessage(node: String): String? {
        for ((n, t) in BotConstants.msg) {
            if (n.contentEquals(node)) {
                return t
            }
        }
        return "PlaceHolder"
    }

    /**
     * 获取带有本地化文本的消息
     *
     * @author NamelessSAMA
     * @param node 本地化文本节点
     * @param otherText 需要添加的文本
     * @return 本地化文本
     */
    fun sendLocalMessage(node: String, otherText: String): String {
        return if (getLocalMessage(node) != null) {
            getLocalMessage(node) + otherText
        } else "PlaceHolder"
    }

    /**
     * 获取带有本地化文本的消息
     *
     * @author NamelessSAMA
     * @param node 本地化文本节点
     * @param otherText 需要添加的文本
     * @return 本地化文本
     */
    fun sendLocalMessage(node: String, vararg otherText: String?): String {
        if (getLocalMessage(node) != null) {
            val sb = StringBuilder()
            sb.append(getLocalMessage(node)).append(" ")
            for (s in otherText) {
                sb.append(s).append("\n")
            }
            return sb.toString().trim { it <= ' ' }
        }
        return "PlaceHolder"
    }

    /**
     * 获取用户的权限组等级
     *
     * @author NamelessSAMA
     * @param qq 指定用户的QQ号
     * @return 权限组等级
     */
    fun getLevel(qq: Long): UserLevel {
        val user = BotUser.getUser(qq)
        if (user != null) {
            return user.level
        }
        return UserLevel.USER
    }

    fun getRestStringInArgs(args: List<String>, startAt: Int): String{
        val sb = StringBuilder()
        for (index in startAt..args.size){
            sb.append(args[index]).append(" ")
        }
        return sb.toString().trim()
    }

}
