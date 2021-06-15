package com.itangcent.common.model

import com.itangcent.common.utils.SimpleExtensible
import java.io.Serializable

class Param : SimpleExtensible(), Serializable {
    var name: String? = null

    var value: Any? = null

    var desc: String? = null

    var required: Boolean? = null


    /**
     * text/file
     */
    var type: String? = null

    var subType: String? = null

    var mock: String? = null
}
