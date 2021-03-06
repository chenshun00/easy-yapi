package com.itangcent.idea.plugin.api.export.yapi

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.containers.ContainerUtil
import com.itangcent.common.model.Doc
import com.itangcent.idea.plugin.Worker
import com.itangcent.idea.plugin.api.export.Folder
import com.itangcent.intellij.psi.SelectedHelper
import com.itangcent.intellij.util.ActionUtils
import com.itangcent.intellij.util.FileType
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.set


class YapiApiExporter : AbstractYapiApiExporter() {

    /***
     * 找到服务器地址，开始导出
     */
    fun export() {
        doExport()
    }

    private fun doExport() {

        logger!!.info("Start find apis...")
        //本质上还是一个foreach的遍历过程，区别在于它是面向过程的还是函数式的.
        //面向过程 foreach(it : Collection){handle(it)}
        //函数式: foreach(x,()->{handle(x)})
        SelectedHelper.Builder()
            //目录过滤
            //一定要明白这里的lambda表达式，不能陷入debug的汪洋大海
            //这里只是定义了lambda表达式，实际的调用过程中，callback是由外边传递进来的
            //它的格式是 (Boolean) -> Unit
            .dirFilter { dir, callBack ->
                //UI处理
                actionContext!!.runInSwingUI {
                    try {
                        val project = actionContext.instance(Project::class)
                        val yes = Messages.showYesNoDialog(
                            project,
                            "Export the model in directory [${ActionUtils.findCurrentPath(dir)}]?",
                            "Are you sure",
                            Messages.getQuestionIcon()
                        )
                        if (yes == Messages.YES) {
                            callBack(true)
                        } else {
                            logger.info("Cancel the operation export api from [${ActionUtils.findCurrentPath(dir)}]!")
                            callBack(false)
                        }
                    } catch (e: Exception) {
                        callBack(false)
                    }
                }
            }
            //过滤文件,仅处理.java/.kt后缀的文件
            .fileFilter { file -> FileType.acceptable(file.name) }
            //
            .classHandle {
                //真实的文档导出操作
                //这里不要直接就看exportDoc了，因为在exportDoc之前，还存在一个数据收集的阶段
                //ComboClassExporter
                //下边的这个写法等同于==> classExporter!!.export(it,{ doc -> exportDoc(doc) })
                // 惊不惊喜，意不意外
                //这里的 { doc -> exportDoc(doc) } 其实就等同于(new DocHandle的接口实现)
                //kotlin 语法有点飘
                classExporter!!.export(it) { doc -> exportDoc(doc) }
            }
            .onCompleted {
                //触发完成✅操作
                if (classExporter is Worker) {
                    classExporter.waitCompleted()
                }
            }
            //⚠️ 到这里为止都是定义了一些lambda表达式的使用，但是真实的调用尚未发起
            //而这里就是真实调用的发起点
            .traversal()
    }

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


    private var tryInputTokenOfModule: HashSet<String> = HashSet()

    /**
     * 获取这个module对应的token
     */
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
                    "Private Token", Messages.getInformationIcon()
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

    override fun exportDoc(doc: Doc, cartInfo: CartInfo): Boolean {
        if (super.exportDoc(doc, cartInfo)) {
            successExportedCarts.add(cartInfo.cartId.toString())
            return true
        }
        return false
    }
}