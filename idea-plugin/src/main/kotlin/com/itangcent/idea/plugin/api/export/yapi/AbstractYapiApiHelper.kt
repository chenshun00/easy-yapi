package com.itangcent.idea.plugin.api.export.yapi

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.inject.Inject
import com.itangcent.common.logger.traceError
import com.itangcent.common.utils.GsonUtils
import com.itangcent.idea.plugin.api.export.ReservedResponseHandle
import com.itangcent.idea.plugin.api.export.StringResponseHandler
import com.itangcent.idea.plugin.api.export.reserved
import com.itangcent.idea.plugin.settings.SettingBinder
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.extend.sub
import com.itangcent.intellij.logger.Logger
import com.itangcent.suv.http.HttpClientProvider
import org.apache.commons.lang3.StringUtils
import java.io.ByteArrayOutputStream
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap
import kotlin.concurrent.withLock

abstract class AbstractYapiApiHelper : YapiApiHelper {
    @Inject
    private val settingBinder: SettingBinder? = null

    @Inject
    protected val logger: Logger? = null

    @Inject
    private val configReader: ConfigReader? = null

    @Inject
    protected val httpClientProvide: HttpClientProvider? = null

    @Volatile
    var server = "http://yapi.raycloud.com"

    private var api_server = "http://api.raycloud.com"

    private var projectIdCache: HashMap<String, String> = HashMap()//token->id

    var projectInfoCache: HashMap<String, JsonElement> = HashMap()//id->info

    protected var cacheLock: ReadWriteLock = ReentrantReadWriteLock()

    open fun hasPrivateToken(module: String): Boolean {
        return getPrivateToken(module) != null
    }

    override fun findServer(): String? {
        return "http://yapi.raycloud.com"
    }

    override fun setYapiServer(yapiServer: String) {
//        val settings = settingBinder!!.read()
//        settings.yapiServer = yapiServer
//        settingBinder.save(settings)
    }

    open protected fun findErrorMsg(res: String?): String? {
        if (res == null) return "no response"
        if (StringUtils.isNotBlank(res) && res.contains("errmsg")) {
            val returnObj = GsonUtils.parseToJsonTree(res)
            val errMsg = returnObj
                .sub("errmsg")
                ?.asString
            if (StringUtils.isNotBlank(errMsg) && !errMsg!!.contains("成功")) {
                return errMsg
            }
        }
        return null
    }

    /**
     * 获取业务ID,其实就是看下边的接口
     */
    override fun getProjectIdByToken(token: String): String? {
        var projectId = cacheLock.readLock().withLock { projectIdCache[token] }
        if (projectId != null) return projectId
        try {
            projectId = getProjectInfo(token, null)
                ?.sub("data")
                ?.sub("businessId")
                ?.asString
        } catch (e: IllegalStateException) {
            logger!!.error("invalid token:$token")
        }
        if (projectId != null) {
            cacheLock.writeLock().withLock {
                projectIdCache[token] = projectId
            }
        }
        return projectId
    }

    /**
     * todo 这个接口算是比较core的了,根据token和可选的projectId获取项目信息
     */
    override fun getProjectInfo(token: String, projectId: String?): JsonElement? {
        if (projectId != null) {
            val cachedProjectInfo = cacheLock.readLock().withLock { projectInfoCache[projectId] }
            if (cachedProjectInfo != null) {
                if (cachedProjectInfo == NULL_PROJECT) {
                    return null
                }
                return cachedProjectInfo
            }
        }

        var url = "$api_server$GET_PROJECT_URL?token=$token"
        if (projectId != null) {
            url = "$url&id=$projectId"
        }

        val ret = getByApi(url, false) ?: return null
        var projectInfo: JsonObject? = null
        try {
            projectInfo = GsonUtils.parseToJsonTree(ret) as? JsonObject
        } catch (e: Exception) {
            logger!!.error("error to parse project [$projectId] info:$ret")
        }

        if (projectId != null && projectInfo != null) {
            if (projectInfo.has("errcode")) {
                if (projectInfo.get("errcode").asInt == 40011) {
                    logger!!.warn("project:$projectId may be deleted.")
                    cacheLock.writeLock().withLock { projectInfoCache[projectId] = NULL_PROJECT }
                    return null
                }
            }
        }
        return projectInfo
    }

    /**
     * 获取业务信息，但是这里还有哪些东西是必须的呢，不确定
     */
    override fun getProjectInfo(token: String): JsonObject? {
        val projectId = getProjectIdByToken(token) ?: return null
        return getProjectInfo(token, projectId) as? JsonObject ?: return null
    }

    /**
     * 执行一次http请求【花里胡哨】
     */
    open fun getByApi(url: String, dumb: Boolean = true): String? {
        return try {
            httpClientProvide!!.getHttpClient()
                .get(url)
                .call()
                .string()
        } catch (e: SocketTimeoutException) {
            if (!dumb) {
                logger!!.trace("$url connect timeout")
                throw e
            }
            logger!!.error("$url connect timeout")
            null
        } catch (e: SocketException) {
            if (!dumb) {
                logger!!.trace("$url is unreachable (connect failed)")
                throw e
            }
            logger!!.error("$url is unreachable (connect failed)")
            null
        } catch (e: Exception) {
            if (!dumb) {
                logger!!.traceError("request $url failed", e)
                throw e
            }
            logger!!.traceError("request $url failed", e)
            null
        }
    }

    protected fun reservedResponseHandle(): ReservedResponseHandle<String> {
        return StringResponseHandler.DEFAULT_RESPONSE_HANDLER.reserved()
    }

    /**
     * Tokens in setting.
     * Map<module,<token,state>>
     * state: null->no_checked, true->valid, false->invalid
     */
    private var tokenMap: HashMap<String, Pair<String, Boolean?>>? = null

    /**
     * 根据模块获取token
     */
    override fun getPrivateToken(module: String): String? {

        //授权是从缓存中进行读取
        cacheLock.readLock().withLock {
            if (tokenMap != null) {
                val token = tokenMap!![module] ?: return null
                when (token.second) {
                    true -> {
                        return token.first
                    }
                    false -> {
                        return null
                    }
                }
            }
        }

        cacheLock.writeLock().withLock {
            if (tokenMap == null) {
                initToken()
            }
            val token = tokenMap!![module] ?: return null
            return if (getProjectInfo(token.first) == null) {
                tokenMap!![module] = token.first to false
                null
            } else {
                tokenMap!![module] = token.first to true
                token.first
            }
        }
    }

    /**
     * 初始化;;;;;主要是读取文件
     */
    private fun initToken() {
        tokenMap = HashMap()
        val settings = settingBinder!!.read()
        if (settings.yapiTokens != null) {
            val properties = Properties()
            properties.load(settings.yapiTokens!!.byteInputStream())
            properties.forEach { t, u -> tokenMap!![t.toString()] = u.toString() to null }
        }
    }

    /**
     * 单机特定文件操作
     * 从特定文件读取后又写入文件
     */
    private fun updateTokens(handle: (Properties) -> Unit) {

        cacheLock.writeLock().withLock {
            val settings = settingBinder!!.read()
            val properties = Properties()
            if (settings.yapiTokens != null) {
                properties.load(settings.yapiTokens!!.byteInputStream())
            }
            handle(properties)

            val byteOutputStream = ByteArrayOutputStream()
            properties.store(byteOutputStream, "")
            settings.yapiTokens = byteOutputStream.toString()
            settingBinder.save(settings)
            if (tokenMap == null) {
                tokenMap = HashMap()
            } else {
                tokenMap!!.clear()
            }
            properties.forEach { t, u -> tokenMap!![t.toString()] = u.toString() to null }
        }
    }

    /**
     * 单机操作
     * 为某一个模块设置token
     */
    override fun setToken(module: String, token: String) {
        updateTokens { properties ->
            properties[module] = token
        }
    }

    /**
     * 单机操作
     * 根据模块移除token
     */
    override fun removeTokenByModule(module: String) {
        updateTokens { properties ->
            properties.remove(module)
        }
    }

    /**
     * 单机操作
     * 移除token/以及对应的模块
     */
    override fun removeToken(token: String) {
        updateTokens { properties ->
            val removedKeys = properties.entries
                .filter { it.value == token }
                .map { it.key }
                .toList()
            removedKeys.forEach { properties.remove(it) }
        }
    }

    /**
     * 单机操作
     * 获取缓存的token数据(重点在于理解这部分缓存的数据去哪里了)
     */
    override fun readTokens(): HashMap<String, String> {
        if (tokenMap == null) {
            initToken()
        }
        return HashMap(tokenMap!!.mapValues { it.value.first })
    }

    companion object {
        const val GET_PROJECT_URL = "/api/business/get"
        val NULL_PROJECT: JsonElement = JsonNull.INSTANCE
    }
}