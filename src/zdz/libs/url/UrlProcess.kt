@file:Suppress("unused", "HttpUrlsUsage")

package zdz.libs.url

import zdz.libs.endecode.decodeUnicode16
import zdz.libs.url.URLType.Companion.getType
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

val urlReg = Regex("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", RegexOption.IGNORE_CASE)
val urlNoParamReg = Regex("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", RegexOption.IGNORE_CASE)
val urlNoProtocolReg = Regex("[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", RegexOption.IGNORE_CASE)

/**
 * 从文本中提取第一个URL
 * @param[source]原始文本
 * @return 文本中的url
 * @throws[java.net.MalformedURLException]参阅[URL]的异常抛出
 */
fun getURL(@URLString source: String) =
    URL(
        source.replace(
            Regex(".*?((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]).*"),
            "$1",
        )
    )

/**
 * 获取网页源码
 * @param[url]要获取的网页网址
 * @param[encode]网页源代码的编码格式
 * @return 网页源代码
 * @throws[Exception]请求失败,失败代码将在throw的信息中给出
 */
fun getSourceCode(url: URL, encode: String = "UTF-8"): String {
    var contentBuffer: StringBuffer? = StringBuffer()
    val responseCode: Int
    var con: HttpURLConnection? = null
    try {
        con = url.openConnection() as HttpURLConnection
        con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)") // IE代理进行下载
        con.connectTimeout = 60000
        con.readTimeout = 60000
        // 获得网页返回信息码
        responseCode = con.responseCode
        if (responseCode == -1) {
            con.disconnect()
            throw Exception("请求失败, 连接不成功")
        }
        // 请求失败
        if (responseCode >= 400) {
            con.disconnect()
            throw Exception("请求失败, 响应代码$responseCode")
        }
        val inStr: InputStream = con.inputStream
        val iStreamReader = InputStreamReader(inStr, encode)
        val buffStr = BufferedReader(iStreamReader)
        var str: String?
        while (buffStr.readLine().also { str = it } != null) contentBuffer!!.append(str)
        inStr.close()
    } catch (e: IOException) {
        e.printStackTrace()
        contentBuffer = null
        println("error: $url")
    } finally {
        con?.disconnect()
    }
    return contentBuffer.toString()
}

/**
 * 获取对应链接的封面图像
 * @param[url]原网址
 * @return 图片网址
 * @throws[IllegalArgumentException]当传入的网址不为
 */
fun getImgURL(url: URL): URL {
    val sourceCode = getSourceCode(url)
    val type = getType(sourceCode)
    val result1 = type.replacement.find(sourceCode)?.value
    checkNotNull(result1) { "ImageURLGetter, getImgURL: result1" }
    val result2 = decodeUnicode16(result1)
    val result = urlReg.find(result2)?.value
    checkNotNull(result) { "ImageURLGetter, getImgURL: result" }
    return toHTTPS(result)
}

/**
 * 重定向.不适用于php重定向
 * @param[url]待重定型的网址
 * @return 重定向后的网址
 * @throws[java.net.MalformedURLException]参阅[URL]的异常抛出
 */
fun redirection(url: URL): URL {
    val conn = url.openConnection() as HttpURLConnection
    conn.instanceFollowRedirects = false
    conn.connectTimeout = 5000
    return URL(conn.getHeaderField("Location"))
}

/**
 * 清理URL参数
 * @param[url]待清理参数的URL,
 * @return 清理参数后的URL
 * @throws[IllegalStateException]非空检查异常
 */
fun cleanURLParam(url: URL): URL {
    val match = urlNoParamReg.find(url.toString())
    checkNotNull(match) { "ImageURLGetter, zdz.url.cleanURLParam: " }
    return URL(match.value)
}

/**
 * 将http网址转换为https网址
 */
fun toHTTPS(url: URL): URL {
    return URL(url.toString().replace("http://", "https://", true))
}

/**
 * 将http网址转换为https网址
 */
fun toHTTPS(url: String): URL {
    return URL(url.replace("http://", "https://", true))
}