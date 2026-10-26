package org.devlive.metaforge.parser.mysql.companion.ast

import java.util.Objects.requireNonNull

class ParserOptions(val decimalLiteralTreatment: DecimalLiteralTreatment = DecimalLiteralTreatment.REJECT,
                    val enhancedErrorHandlerEnabled: Boolean = true) {

    enum class DecimalLiteralTreatment {
        AS_DOUBLE,
        AS_DECIMAL,
        REJECT
    }

    init {
        requireNonNull(decimalLiteralTreatment, "decimalLiteralTreatment is null")
    }
}

