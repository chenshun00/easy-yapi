package com.itangcent.common.model

import com.itangcent.common.utils.SimpleExtensible
import java.io.Serializable

open class FormParam : SimpleExtensible(), Serializable {

    var name: String? = null

    var value: String? = null

    var desc: String? = null

    var required: Boolean? = null

    /**
     * text/file
     */
    var type: String? = null

    var subType: String? = null

    var mock: String? = null

    fun setMockX(mock: String?) : FormParam{
        this.mock =mock
        return this
    }
}
