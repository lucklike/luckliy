package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 浏览器伪装
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StaticHeader("[SET]User-Agent: #{def(ann($mc$, 'com.luckyframework.httpclient.proxy.annotations.BrowserFeign')?.userAgent, 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0')}")
@Combination(StaticHeader.class)
public @interface BrowserFeign {

    // ========== Windows Chrome ==========
    String CHROME_WIN_10_X64 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    String CHROME_WIN_10_X86 = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    String CHROME_WIN_11 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0";
    String CHROME_WIN_8 = "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    String CHROME_WIN_7 = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    // ========== macOS Chrome ==========
    String CHROME_MAC_INTEL = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    String CHROME_MAC_ARM = "Mozilla/5.0 (Macintosh; ARM Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    String CHROME_MAC_SONOMA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    // ========== Linux Chrome ==========
    String CHROME_LINUX_X64 = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    String CHROME_LINUX_UBUNTU = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:122.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    String CHROME_LINUX_FEDORA = "Mozilla/5.0 (X11; Fedora; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    // ========== Android Chrome ==========
    String CHROME_ANDROID_SAMSUNG = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
    String CHROME_ANDROID_XIAOMI = "Mozilla/5.0 (Linux; Android 13; 22081212C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
    String CHROME_ANDROID_HUAWEI = "Mozilla/5.0 (Linux; Android 13; LIO-AL00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
    String CHROME_ANDROID_OPPO = "Mozilla/5.0 (Linux; Android 13; CPH2581) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
    String CHROME_ANDROID_VIVO = "Mozilla/5.0 (Linux; Android 13; V2218A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
    String CHROME_ANDROID_ONEUI = "Mozilla/5.0 (Linux; Android 14; SM-S926B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";

    // ========== iOS Chrome ==========
    String CHROME_IOS_17 = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/122.0.0.0 Mobile/15E148 Safari/604.1";
    String CHROME_IOS_16 = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/122.0.0.0 Mobile/15E148 Safari/604.1";
    String CHROME_IOS_IPAD = "Mozilla/5.0 (iPad; CPU OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/122.0.0.0 Mobile/15E148 Safari/604.1";

    // ========== Microsoft Edge ==========
    String EDGE_WIN_11 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0";
    String EDGE_WIN_10 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0";
    String EDGE_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0";
    String EDGE_LINUX = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0";
    String EDGE_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0";
    String EDGE_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 EdgiOS/122.0.0.0 Mobile/15E148 Safari/605.1.15";

    // ========== Mozilla Firefox ==========
    String FIREFOX_WIN_10 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0";
    String FIREFOX_WIN_7 = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0";
    String FIREFOX_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:123.0) Gecko/20100101 Firefox/123.0";
    String FIREFOX_LINUX = "Mozilla/5.0 (X11; Linux x86_64; rv:123.0) Gecko/20100101 Firefox/123.0";
    String FIREFOX_ANDROID = "Mozilla/5.0 (Android 14; Mobile; rv:123.0) Gecko/123.0 Firefox/123.0";
    String FIREFOX_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/123.0 Mobile/15E148 Safari/605.1.15";

    // ========== Apple Safari ==========
    String SAFARI_MAC_SONOMA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15";
    String SAFARI_MAC_VENTURA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Safari/605.1.15";
    String SAFARI_IOS_17 = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";
    String SAFARI_IOS_16 = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1";
    String SAFARI_IPADOS = "Mozilla/5.0 (iPad; CPU OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";

    // ========== Opera ==========
    String OPERA_WIN = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 OPR/107.0.0.0";
    String OPERA_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 OPR/107.0.0.0";
    String OPERA_LINUX = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 OPR/107.0.0.0";
    String OPERA_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 OPR/77.0.0.0";
    String OPERA_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1 OPR/64.0.0.0";

    // ========== 国内浏览器：QQ浏览器 ==========
    String QQ_WIN = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 QQBrowser/12.8.6267.400";
    String QQ_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 QQBrowser/12.8.6267.400";
    String QQ_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 QQBrowser/12.8.6267.400";
    String QQ_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1 QQBrowser/12.8.6267.400";

    // ========== 国内浏览器：UC浏览器 ==========
    String UC_ANDROID = "Mozilla/5.0 (Linux; U; Android 14; zh-CN; SM-S901U Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/122.0.0.0 Mobile Safari/537.36 UCBS/2.8.2.1024 UWS/3.22.2.1 Mobile";
    String UC_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1 UCBrowser/13.7.8.1124";
    String UC_WIN = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 UBrowser/7.0.8.1024";

    // ========== 国内浏览器：百度浏览器 ==========
    String BAIDU_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 T7/14.50 baidubrowser/14.50.0.10";
    String BAIDU_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1 baidubrowser/5.7.6.10";
    String BAIDU_WIN = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 baidubrowser/8.7.6.0";

    // ========== 国内浏览器：搜狗浏览器 ==========
    String SOGOU_WIN = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 SE 2.X MetaSr 1.0";
    String SOGOU_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 SogouSearch Android1.0 version/6.0.0";

    // ========== 国内浏览器：猎豹浏览器 ==========
    String LIEBAO_WIN = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 LBBROWSER/10.7.8.1002";
    String LIEBAO_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 liebao/6.8.0.1010";

    // ========== 国内浏览器：傲游浏览器 ==========
    String MAXTHON_WIN = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Maxthon/7.0.0.2000 Chrome/122.0.0.0 Safari/537.36";
    String MAXTHON_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 Maxthon/7.0.0.2000";

    // ========== 国内浏览器：星愿浏览器 ==========
    String TWINKLE_WIN = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Twinkle/10.7.8.1002";

    // ========== 移动端：微信内置浏览器 ==========
    String WECHAT_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/122.0.0.0 Mobile Safari/537.36 MMWEBID/9323 MicroMessenger/8.0.47.2560(0x28002F51) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN";
    String WECHAT_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.47(0x18002f29) NetType/WIFI Language/zh_CN";

    // ========== 移动端：支付宝内置浏览器 ==========
    String ALIPAY_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/122.0.0.0 Mobile Safari/537.36 Alipay/10.5.6.8000";
    String ALIPAY_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Alipay/10.5.6.8000";

    // ========== 移动端：钉钉内置浏览器 ==========
    String DINGTALK_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 DingTalk/7.0.30.5010178";
    String DINGTALK_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 DingTalk/7.0.30.5010178";

    // ========== 移动端：QQ手机浏览器 ==========
    String QQ_BROWSER_ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/122.0.0.0 Mobile MQQBrowser/12.8.6267.400 Safari/537.36";
    String QQ_BROWSER_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1 MQQBrowser/12.8.6267.400";

    // ========== 移动端：小米浏览器 ==========
    String MI_BROWSER_ANDROID = "Mozilla/5.0 (Linux; Android 14; 22081212C) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/122.0.0.0 Mobile Safari/537.36 XiaoMi/MiuiBrowser/18.8.0";

    // ========== 移动端：华为浏览器 ==========
    String HUAWEI_BROWSER_ANDROID = "Mozilla/5.0 (Linux; Android 14; LIO-AL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/122.0.0.0 Mobile Safari/537.36 HuaweiBrowser/18.1.7.303";

    // ========== 其他浏览器：Internet Explorer ==========
    String IE_11 = "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko";
    String IE_10 = "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 10.0; WOW64; Trident/7.0)";
    String IE_9 = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 10.0; WOW64; Trident/7.0)";

    // ========== 其他浏览器：Chromium ==========
    String CHROMIUM_LINUX = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/122.0.0.0 Chrome/122.0.0.0 Safari/537.36";

    // ========== 其他浏览器：Brave ==========
    String BRAVE_WIN = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Brave/122.0.0.0";
    String BRAVE_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Brave/122.0.0.0";

    // ========== 其他浏览器：Vivaldi ==========
    String VIVALDI_WIN = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Vivaldi/6.5.3206.53";
    String VIVALDI_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Vivaldi/6.5.3206.53";

    // ========== 其他浏览器：Yandex ==========
    String YANDEX_WIN = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 YaBrowser/23.11.0.0 Safari/537.36";
    String YANDEX_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 YaBrowser/23.11.0.0 Safari/537.36";

    // ========== 原生浏览器 ==========
    String ANDROID_BROWSER_SAMSUNG = "Mozilla/5.0 (Linux; Android 14; SM-S901U) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/22.0 Chrome/122.0.0.0 Mobile Safari/537.36";
    String ANDROID_BROWSER_XIAOMI = "Mozilla/5.0 (Linux; Android 14; 22081212C) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/122.0.0.0 Mobile Safari/537.36";
    String ANDROID_BROWSER_HUAWEI = "Mozilla/5.0 (Linux; Android 14; LIO-AL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/122.0.0.0 Mobile Safari/537.36";

    // ========== 爬虫/机器人 ==========
    String GOOGLEBOT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
    String GOOGLEBOT_NEWS = "Googlebot-News";
    String GOOGLEBOT_IMAGE = "Googlebot-Image/1.0";
    String BINGBOT = "Mozilla/5.0 (compatible; Bingbot/2.0; +http://www.bing.com/bingbot.htm)";
    String BAIDUBOT = "Mozilla/5.0 (compatible; Baiduspider/2.0; +http://www.baidu.com/search/spider.html)";
    String BAIDU_MOBILE_BOT = "Mozilla/5.0 (Linux;u;Android 4.2.2;zh-cn;) AppleWebKit/534.46 (KHTML,like Gecko) Version/5.1 Mobile Safari/10600.6.3 (compatible; Baiduspider/2.0; +http://www.baidu.com/search/spider.html)";
    String YANDEXBOT = "Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)";
    String DUCKDUCKBOT = "DuckDuckBot/1.0; (+http://duckduckgo.com/duckduckbot.html)";
    String FACEBOOK_BOT = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)";
    String TWITTER_BOT = "Twitterbot/1.0";
    String SLURP = "Mozilla/5.0 (compatible; Yahoo! Slurp; http://help.yahoo.com/help/us/ysearch/slurp)";
    String APPLEBOT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/8.0.2 Safari/600.2.5 (Applebot/0.1)";
    String MICROSOFT_BOT = "msnbot/2.0b (+http://search.msn.com/msnbot.htm)";

    // ========== 命令行工具 ==========
    String CURL = "curl/7.88.1";
    String WGET = "Wget/1.21.3";
    String HTTPIE = "HTTPie/3.2.1";

    // ========== 游戏主机浏览器 ==========
    String PS4_BROWSER = "Mozilla/5.0 (PlayStation 4 11.00) AppleWebKit/605.1.15 (KHTML, like Gecko)";
    String PS5_BROWSER = "Mozilla/5.0 (PlayStation 5 8.00) AppleWebKit/605.1.15 (KHTML, like Gecko)";
    String XBOX_BROWSER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; Xbox; Xbox One) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edge/122.0.0.0";
    String NINTENDO_SWITCH = "Mozilla/5.0 (Nintendo Switch; WebApplet) AppleWebKit/609.4 (KHTML, like Gecko) NF/6.0.2.21.3 NintendoBrowser/5.1.0.22474";

    // ========== 智能电视/盒子 ==========
    String SMART_TV_SAMSUNG = "Mozilla/5.0 (SMART-TV; Linux; Tizen 6.5) AppleWebKit/538.1 (KHTML, like Gecko) Version/6.5 TV Safari/538.1";
    String SMART_TV_LG = "Mozilla/5.0 (Web0S; Linux/SmartTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 WebAppManager";
    String ANDROID_TV = "Mozilla/5.0 (Linux; Android 11; ADT-3 Build/RTT0.210909.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    // ========== 车机系统 ==========
    String TESLA_BROWSER = "Mozilla/5.0 (X11; GNU/Linux) AppleWebKit/537.36 (KHTML, like Gecko) QtCarBrowser Safari/537.36";
    String ANDROID_AUTO = "Mozilla/5.0 (Linux; Android 13; Automotive OS) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    // ========== 物联网设备 ==========
    String RASPBERRY_PI = "Mozilla/5.0 (X11; Ubuntu; Linux armv7l; rv:122.0) Gecko/20100101 Firefox/122.0";
    String ESP8266 = "ESP8266HTTPClient";



    String userAgent() default CHROME_WIN_11;
}
