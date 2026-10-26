package org.devlive.metaforge.notify.dingtalk.model

import com.fasterxml.jackson.annotation.JsonProperty

class ReturnModel
{
    @JsonProperty(value = "errcode")
    var code: Number? = null

    @JsonProperty(value = "errmsg")
    var message: String? = null
}