package com.itangcent.idea.plugin.api.export.yapi

import com.intellij.openapi.ui.Messages
import com.intellij.util.containers.ContainerUtil
import com.itangcent.common.model.Doc
import com.itangcent.idea.plugin.api.export.Folder
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.set


class YapiApiDashBoardExporter : AbstractYapiApiExporter() {

    private var tryInputTokenOfModule: HashSet<String> = HashSet()

    override fun getTokenOfModule(module: String): String? {
        val privateToken = super.getTokenOfModule(module)
        if (!privateToken.isNullOrBlank()) {
            return privateToken
        }

        if (tryInputTokenOfModule.contains(module)) {
            return null
        } else {
            tryInputTokenOfModule.add(module)
            val modulePrivateToken = actionContext!!.callInSwingUI {
                return@callInSwingUI Messages.showInputDialog(
                    project, "Input Private Token Of Module:$module",
                    "Yapi Private Token", Messages.getInformationIcon()
                )
            }
            return if (modulePrivateToken.isNullOrBlank()) {
                null
            } else {
                yapiApiHelper!!.setToken(module, modulePrivateToken)
                modulePrivateToken
            }
        }
    }

    private var successExportedCarts: MutableSet<String> = ContainerUtil.newConcurrentSet<String>()

    //privateToken+folderName -> CartInfo
    private val folderNameCartMap: HashMap<String, CartInfo> = HashMap()

    @Synchronized
    override fun getCartForDoc(folder: Folder, privateToken: String): CartInfo? {
        var cartInfo = folderNameCartMap["$privateToken${folder.name}"]
        if (cartInfo != null) return cartInfo

        cartInfo = super.getCartForDoc(folder, privateToken)
        if (cartInfo != null) {
            folderNameCartMap["$privateToken${folder.name}"] = cartInfo
        }
        return cartInfo
    }

    fun exportDoc(doc: Doc, privateToken: String): Boolean {
        if (doc.resource == null) return false
        val cartInfo = getCartForDoc(doc.resource!!) ?: return false
        return exportDoc(doc, cartInfo)
    }

    override fun exportDoc(doc: Doc, cartInfo: CartInfo): Boolean {
        if (super.exportDoc(doc, cartInfo)) {
            if (successExportedCarts.add(cartInfo.cartId.toString())) {
                logger!!.info("${this.javaClass.simpleName}:Export to success")
            }
            return true
        }
        return false
    }
}