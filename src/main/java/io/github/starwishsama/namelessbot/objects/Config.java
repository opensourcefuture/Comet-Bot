package io.github.starwishsama.namelessbot.objects;

import com.google.gson.annotations.SerializedName;
import io.github.starwishsama.namelessbot.commands.MusicCommand;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Data
public class Config {
    @SerializedName("owner_id")
    private long ownerID = 0;
    @SerializedName("auto_save_time")
    private int autoSaveTime = 15;
    @SerializedName("bot_admins")
    @Deprecated
    private List<Long> botAdmins = new ArrayList<>();
    @SerializedName("post_port")
    private int postPort = 5700;
    @SerializedName("post_url")
    private String postUrl = "127.0.0.1";
    @SerializedName("bot_name")
    private String botName = "Bot";
    @SerializedName("bot_port")
    private int botPort = 5701;
    @SerializedName("rcon_url")
    private String rconUrl;
    @SerializedName("rcon_port")
    private int rconPort;
    @SerializedName("rcon_password")
    private byte[] rconPwd;
    @SerializedName("netease_api_url")
    private String netEaseApi;
    @SerializedName("cmd_prefix")
    private String[] cmdPrefix = new String[]{"/", "#", "!", "."};
    @SerializedName("bind_minecraft_account")
    private boolean bindMCAccount = false;
    @SerializedName("anti_spam")
    private boolean antiSpam = false;
    @SerializedName("spam_mute_time")
    private int spamMuteTime = 60;
    @SerializedName("cool_down_time")
    private int coolDownTime = 6;
    @SerializedName("filter_words")
    private List<String> filterWords = new ArrayList<>();
    @SerializedName("default_music_api")
    private MusicCommand.MusicType api = MusicCommand.MusicType.QQ;
    @SerializedName("bili_live_api")
    private String liveApi = "https://api.vtbs.moe/v1/info";
    @SerializedName("rss_subscribers")
    private List<Long> subscribers = new ArrayList<>();
    @SerializedName("guess_max_number")
    private int maxNumber = 100;
    private List<Long> blackList = new LinkedList<>();
}