package com.itangcent.idea.plugin.api.call

import com.google.inject.Inject
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.itangcent.idea.plugin.api.export.ClassExporter
import com.itangcent.idea.plugin.dialog.ApiCallDialog
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger
import java.lang.ref.WeakReference

class ApiCaller {

    @Inject
    private val logger: Logger? = null

    @Inject
    private val actionContext: ActionContext? = null

    @Inject
    private val project: Project? = null

    @Inject
    private val classExporter: ClassExporter? = null

    companion object {
        private val API_CALL_DIALOG = Key.create<WeakReference<ApiCallDialog>>("API_CALL_DIALOG")
    }
}