package com.itangcent.idea.plugin.api.export.yapi

class CartInfo {
    var cartId: String? = null
    var cartName: String? = null
    var privateToken: String? = null
    var projectId: String? = null

    constructor(cartId: String?, cartName: String?, privateToken: String?, projectId: String?) {
        this.cartId = cartId
        this.cartName = cartName
        this.privateToken = privateToken
        this.projectId = projectId
    }

    constructor()


}