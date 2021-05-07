package com.itangcent.idea.plugin.api.export.yapi

import com.google.inject.Inject
import com.intellij.openapi.project.Project
import com.itangcent.common.logger.traceError
import com.itangcent.common.model.Doc
import com.itangcent.common.utils.GsonUtils
import com.itangcent.common.utils.KV
import com.itangcent.idea.plugin.api.export.ClassExporter
import com.itangcent.idea.plugin.api.export.Folder
import com.itangcent.idea.plugin.api.export.FormatFolderHelper
import com.itangcent.idea.plugin.settings.SettingBinder
import com.itangcent.idea.plugin.utils.SpringClassName
import com.itangcent.idea.psi.PsiMethodResource
import com.itangcent.idea.utils.ModuleHelper
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.logger.Logger
import java.util.*


open class AbstractYapiApiExporter {

    @Inject
    protected val logger: Logger? = null

    @Inject
    protected val yapiApiHelper: YapiApiHelper? = null

    @Inject
    protected val actionContext: ActionContext? = null

    @Inject
    protected val classExporter: ClassExporter? = null

    @Inject
    protected val moduleHelper: ModuleHelper? = null

    @Inject
    protected val yapiFormatter: YapiFormatter? = null

    @Inject
    protected val project: Project? = null

    @Inject
    protected val settingBinder: SettingBinder? = null

    @Inject
    protected val formatFolderHelper: FormatFolderHelper? = null

    @Inject
    private val annotationHelper: AnnotationHelper? = null

    /**
     * Get the token of the special module.
     * see https://hellosean1025.github.io/yapi/documents/project.html#token
     * Used to request openapi.
     * see https://hellosean1025.github.io/yapi/openapi.html
     */
    protected open fun getTokenOfModule(module: String): String? {
        return yapiApiHelper!!.getPrivateToken(module)
    }

    protected open fun getCartForDoc(resource: Any): CartInfo? {
        val module = actionContext!!.callInReadUI { moduleHelper!!.findModule(resource) } ?: return null
        val privateToken = getTokenOfModule(module) ?: return null

        val value = annotationHelper!!.findAttr((resource as PsiMethodResource).resourceClass(), SpringClassName.API_CLASS_GROUP)
        return if (value != null && value != "group") {
            getCatForDocByAnnotation(value as String, privateToken);
        } else {
            val folder = formatFolderHelper!!.resolveFolder(resource)
            getCartForDoc(folder, privateToken)
        }

    }

    protected fun getCatForDocByAnnotation(name: String, privateToken: String): CartInfo? {
        var cartId: String?

        //try find existed cart.
        //根据名字尝试找到已经存在的类目ID
        try {
            cartId = yapiApiHelper!!.findCat(privateToken, name)
        } catch (e: Exception) {
            logger!!.traceError("error to find cart [$name]", e)
            return null
        }
        try {
            if (cartId == null) {
                cartId = yapiApiHelper.findCat(privateToken, "公共");
            }
        } catch (e: Exception) {

        }
        //create new cart.
        if (cartId == null) {
            throw RuntimeException("获取API分类失败")
        }
        return CartInfo(cartId, name, privateToken, yapiApiHelper.getProjectIdByToken(privateToken))
    }

    /**
     * folder代表的就是类目信息
     * @param folder 类目信息
     * @param privateToken token信息
     */
    protected open fun getCartForDoc(folder: Folder, privateToken: String): CartInfo? {

        val name: String = folder.name ?: "anonymous"

        if (name == "anonymous") {
            throw RuntimeException("获取API类目失败,请参考使用文档#注释部分后重试")
        }

        var cartId: String?

        //try find existed cart.
        //根据名字尝试找到已经存在的类目ID
        try {
            cartId = yapiApiHelper!!.findCat(privateToken, name)
        } catch (e: Exception) {
            logger!!.traceError("error to find cart [$name]", e)
            return null
        }

        try {
            if (cartId == null) {
                cartId = yapiApiHelper.findCat(privateToken, "公共");
            }
        } catch (e: Exception) {

        }

        //create new cart.
        if (cartId == null) {
            throw RuntimeException("根据【$name】获取API类目失败,请参考使用文档#注释部分后重试")
        }

        return CartInfo(cartId, name, privateToken, yapiApiHelper.getProjectIdByToken(privateToken))
    }

    /**
     * 正式导出文档
     */
    fun exportDoc(doc: Doc): Boolean {
        if (doc.resource == null) return false
        val cartInfo = getCartForDoc(doc.resource!!) ?: return false
        return exportDoc(doc, cartInfo)
    }

    /**
     * 获取Java文件中全部的API信息,然后遍历这个数据导出
     */
    open fun exportDoc(doc: Doc, cartInfo: CartInfo): Boolean {
        val apiInfos = yapiFormatter!!.doc2Item(doc)
        logger!!.info("api info:${GsonUtils.toJson(apiInfos)}")
        check(apiInfos)
        var ret = false
        apiInfos.forEach { apiInfo ->
            apiInfo["token"] = cartInfo.privateToken
            apiInfo["catid"] = cartInfo.cartId
            ret = ret or yapiApiHelper!!.saveApiInfoToApiDocPlatform(apiInfo)
            logger.info("API上传成功，访问地址====> http://api-inner.raycloud.com/#/?menuIdx=${cartInfo.projectId}&action=${apiInfo["action"]} ")
        }
        return ret
    }

    private fun check(apiInfos: List<HashMap<String, Any?>>) {
        val hashMap = apiInfos[0]
        val query = hashMap["req_query"]
        if (query is LinkedList<*>) {
            val or = query.stream()
                .filter { (it as KV<String, *>).getAs<String>("type") == "array" }
                .anyMatch { (it as KV<String, *>).getAs<String>("subType") == "map" }.or(false)
            if (or) {
                throw RuntimeException("GET请求不支持传递List<Object>的形式，仅支持,List<基本包装类型>,如果需要是使用，请修改为 @RequestBody的场景进行使用. 如果不明白,请仔细阅读.")
            }
        }

    }

}