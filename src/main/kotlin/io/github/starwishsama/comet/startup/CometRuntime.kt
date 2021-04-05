package io.github.starwishsama.comet.startup

import io.github.starwishsama.comet.BotVariables
import io.github.starwishsama.comet.BotVariables.cfg
import io.github.starwishsama.comet.BotVariables.cometServer
import io.github.starwishsama.comet.BotVariables.consoleCommandLogger
import io.github.starwishsama.comet.BotVariables.daemonLogger
import io.github.starwishsama.comet.BuildConfig
import io.github.starwishsama.comet.CometApplication
import io.github.starwishsama.comet.api.command.CommandExecutor
import io.github.starwishsama.comet.api.thirdparty.bilibili.DynamicApi
import io.github.starwishsama.comet.api.thirdparty.bilibili.FakeClientApi
import io.github.starwishsama.comet.api.thirdparty.bilibili.VideoApi
import io.github.starwishsama.comet.api.thirdparty.twitter.TwitterApi
import io.github.starwishsama.comet.api.thirdparty.youtube.YoutubeApi
import io.github.starwishsama.comet.commands.chats.*
import io.github.starwishsama.comet.commands.console.BroadcastCommand
import io.github.starwishsama.comet.commands.console.DebugCommand
import io.github.starwishsama.comet.commands.console.StopCommand
import io.github.starwishsama.comet.file.BackupHelper
import io.github.starwishsama.comet.file.DataSetup
import io.github.starwishsama.comet.listeners.*
import io.github.starwishsama.comet.logger.HinaLogLevel
import io.github.starwishsama.comet.logger.RetrofitLogger
import io.github.starwishsama.comet.service.gacha.GachaService
import io.github.starwishsama.comet.service.pusher.PusherManager
import io.github.starwishsama.comet.service.webhook.WebHookServer
import io.github.starwishsama.comet.utils.CometUtil.getRestString
import io.github.starwishsama.comet.utils.FileUtil
import io.github.starwishsama.comet.utils.LoggerAppender
import io.github.starwishsama.comet.utils.RuntimeUtil
import io.github.starwishsama.comet.utils.StringUtil.getLastingTimeAsString
import io.github.starwishsama.comet.utils.TaskUtil
import io.github.starwishsama.comet.utils.network.NetUtil
import kotlinx.coroutines.runBlocking
import net.kronos.rkon.core.Rcon
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.utils.MiraiLogger
import okhttp3.OkHttpClient
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object CometRuntime {
    fun postSetup() {
        BotVariables.filePath = FileUtil.getJarLocation()
        BotVariables.startTime = LocalDateTime.now()
        BotVariables.loggerAppender = LoggerAppender(FileUtil.getLogLocation())

        Runtime.getRuntime().addShutdownHook(Thread { shutdownTask() })

        BotVariables.logger.info(
            """
        
           ______                     __ 
          / ____/___  ____ ___  ___  / /_
         / /   / __ \/ __ `__ \/ _ \/ __/
        / /___/ /_/ / / / / / /  __/ /_  
        \____/\____/_/ /_/ /_/\___/\__/  


    """
        )

        DataSetup.init()

        BotVariables.client = OkHttpClient().newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .readTimeout(5, TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true }
            .also {
                if (cfg.proxySwitch) {
                    if (NetUtil.checkProxyUsable()) {
                        it.proxy(Proxy(cfg.proxyType, InetSocketAddress(cfg.proxyUrl, cfg.proxyPort)))
                    }
                }
            }
            .addInterceptor(RetrofitLogger())
            .build()
    }

    private fun shutdownTask(){
        BotVariables.logger.info("[Bot] 正在关闭 Bot...")
        DataSetup.saveAllResources()
        PusherManager.savePushers()
        cometServer.stop()
        BotVariables.service.shutdown()
        BotVariables.rCon?.disconnect()
        BotVariables.loggerAppender.close()
    }

    fun setupBot(bot: Bot, logger: MiraiLogger) {
        setupRCon()

        GachaService.loadAllPools()

        CommandExecutor.setupCommand(
            arrayOf(
                AdminCommand(),
                ArkNightCommand(),
                BiliBiliCommand(),
                CheckInCommand(),
                ClockInCommand(),
                io.github.starwishsama.comet.commands.chats.DebugCommand(),
                DivineCommand(),
                PCRCommand(),
                GuessNumberCommand(),
                HelpCommand(),
                InfoCommand(),
                MusicCommand(),
                MuteCommand(),
                PictureSearchCommand(),
                R6SCommand(),
                RConCommand(),
                KickCommand(),
                TwitterCommand(),
                VersionCommand(),
                GroupConfigCommand(),
                RSPCommand(),
                RollCommand(),
                YoutubeCommand(),
                MinecraftCommand(),
                PusherCommand(),
                NoteCommand(),
                GithubCommand(),
                // Console Command
                StopCommand(),
                DebugCommand(),
                io.github.starwishsama.comet.commands.console.AdminCommand(),
                io.github.starwishsama.comet.commands.console.BiliBiliCommand(),
                BroadcastCommand()
            )
        )

        logger.info("[命令] 已注册 " + CommandExecutor.countCommands() + " 个命令")

        /** 监听器 */
        val listeners = arrayOf(
            ConvertLightAppListener,
            RepeatListener,
            BotGroupStatusListener,
            AutoReplyListener,
            GroupMemberChangedListener,
            GroupRequestListener,
            NoteListener
        )

        listeners.forEach { listener ->
            if (listener.eventToListen.isEmpty()) {
                daemonLogger.warning("监听器 ${listener::class.java.simpleName} 没有监听任何一个事件, 请检查是否正确!")
            } else {
                listener.eventToListen.forEach { eventClass ->
                    bot.globalEventChannel().subscribeAlways(eventClass) {
                        if (BotVariables.switch) {
                            listener.listen(this)
                        }
                    }
                }
            }

            logger.info("[监听器] 已注册 ${listener.getName()} 监听器")
        }

        runScheduleTasks()

        PusherManager.initPushers(bot)

        startupServer()

        DataSetup.initPerGroupSetting(bot)

        logger.info("彗星 Bot 启动成功, 版本 ${BuildConfig.version}, 耗时 ${BotVariables.startTime.getLastingTimeAsString()}")

        RuntimeUtil.doGC()

        CommandExecutor.startHandler(bot)
    }

    fun setupRCon() {
        val address = cfg.rConUrl
        val password = cfg.rConPassword
        if (address != null && password != null && BotVariables.rCon == null) {
            BotVariables.rCon = Rcon(address, cfg.rConPort, password.toByteArray())
        }
    }

    private fun startupServer() {
        try {
            if (cfg.webHookSwitch) {
                val customSuffix = cfg.webHookAddress.replace("http://", "").replace("https://", "").split("/")
                cometServer = WebHookServer(cfg.webHookPort, customSuffix.getRestString(1, "/"))
            }
        } catch (e: Throwable) {
            daemonLogger.warning("Comet 服务端启动失败", e)
        }
    }

    private fun runScheduleTasks() {
        TaskUtil.runAsync { BackupHelper.checkOldFiles() }

        val apis = arrayOf(DynamicApi, TwitterApi, YoutubeApi, VideoApi)

        /** 定时任务 */
        BackupHelper.scheduleBackup()
        TaskUtil.runScheduleTaskAsync(5, 5, TimeUnit.HOURS) {
            BotVariables.users.forEach { it.value.addTime(100) }
        }


        val pwd = cfg.biliPassword
        val username = cfg.biliUserName

        TaskUtil.runAsync(5) {
            if (pwd != null && username != null) {
                runBlocking {
                    FakeClientApi.login(username, pwd)
                }
            } else {
                daemonLogger.info("未设置哔哩哔哩账号, 部分哔哩哔哩相关功能可能受限")
            }
        }

        apis.forEach {
            TaskUtil.runScheduleTaskAsync(it.duration.toLong(), it.duration.toLong(), TimeUnit.HOURS) {
                it.resetTime()
            }
        }

        TaskUtil.runScheduleTaskAsync(1, 1, TimeUnit.HOURS) {
            RuntimeUtil.doGC()
        }
    }

    fun handleConsoleCommand() {
        TaskUtil.runAsync {
            consoleCommandLogger.log(HinaLogLevel.Info, "后台已启用", prefix = "后台管理")

            while (true) {
                var line: String

                runBlocking {
                    try {
                        line = CometApplication.console.readLine(">")
                        val result = CommandExecutor.dispatchConsoleCommand(line)
                        if (result.isNotEmpty()) {
                            consoleCommandLogger.info(result)
                        }
                    } catch (ignored: EndOfFileException) {
                    } catch (ignored: UserInterruptException) {
                    }
                }
            }
        }
    }
}