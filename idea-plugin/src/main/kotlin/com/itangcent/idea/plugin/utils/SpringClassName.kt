package com.itangcent.idea.plugin.utils


class SpringClassName {

    companion object {

        val SPRING_REQUEST_RESPONSE: Array<String> = arrayOf("HttpServletRequest", "HttpServletResponse")

        var SPRING_CONTROLLER_ANNOTATION: Set<String> =
            mutableSetOf(
                "org.springframework.stereotype.Controller",
                "org.springframework.web.bind.annotation.RestController",
                "com.raycloud.yapi.api.Path",
                "com.raycloud.qnee.sdk.api.internal.annotation.ServiceMethodBean",
                "com.taobao.miniapp.function.anotation.RequestHandler"
            )

        //file
        const val MULTI_PART_FILE = "org.springframework.web.multipart.MultipartFile"

        //custom annotations
        const val API_ACTION = "com.raycloud.yapi.api.Action"
        const val API_CLASS_GROUP = "com.raycloud.yapi.api.ClassGroup"
        const val API_SESSION = "com.raycloud.yapi.api.Session"

        //annotations
        const val REQUEST_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.RequestMapping"
        const val REQUEST_BODY_ANNOTATION = "org.springframework.web.bind.annotation.RequestBody"
        const val TAOBAO_MINI_BODY_ANNOTATION = "com.taobao.miniapp.function.anotation.DataCast"
        const val CUSTOM_MINI_BODY_ANNOTATION = "com.raycloud.yapi.api.Body"
        const val REQUEST_PARAM_ANNOTATION = "org.springframework.web.bind.annotation.RequestParam"
        const val MODEL_ATTRIBUTE_ANNOTATION = "org.springframework.web.bind.annotation.ModelAttribute"
        const val PATH_VARIABLE_ANNOTATION = "org.springframework.web.bind.annotation.PathVariable"
        const val COOKIE_VALUE_ANNOTATION = "org.springframework.web.bind.annotation.CookieValue"

        const val MOCK_ANNOTATION = "com.raycloud.yapi.api.Mock";

        const val REQUEST_HEADER = "org.springframework.web.bind.annotation.RequestHeader"

        const val GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping"
        const val POST_MAPPING = "org.springframework.web.bind.annotation.PostMapping"
        const val PUT_MAPPING = "org.springframework.web.bind.annotation.PutMapping"
        const val DELETE_MAPPING = "org.springframework.web.bind.annotation.DeleteMapping"
        const val PATCH_MAPPING = "org.springframework.web.bind.annotation.PatchMapping"
        const val ROUTER_MAPPING = "com.raycloud.yapi.api.Router"
        const val PATH_MAPPING = "com.raycloud.yapi.api.Path"
        const val TAOBAO_MAPPING = "com.taobao.miniapp.function.anotation.RequestHandler"
        const val ServiceMethod_MAPPING = "com.raycloud.qnee.sdk.api.internal.annotation.ServiceMethod"
        const val SERVICE_METHOD_MAPPING = "com.raycloud.qnee.sdk.api.internal.annotation.ServiceMethodBean"

        val SPRING_REQUEST_MAPPING_ANNOTATIONS: Set<String> = setOf(
            REQUEST_MAPPING_ANNOTATION,
            GET_MAPPING,
            DELETE_MAPPING,
            PATCH_MAPPING,
            POST_MAPPING,
            PUT_MAPPING,
            ROUTER_MAPPING,
            PATH_MAPPING,
            TAOBAO_MAPPING,
            SERVICE_METHOD_MAPPING,
            ServiceMethod_MAPPING
        )

        val ACTION_MAPPING: Set<String> = setOf(
            API_ACTION
        )

        const val REQUEST_HEADER_DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n"

        const val ESCAPE_REQUEST_HEADER_DEFAULT_NONE = "\\n\\t\\t\\n\\t\\t\\n\\uE000\\uE001\\uE002\\n\\t\\t\\t\\t\\n"

    }
}