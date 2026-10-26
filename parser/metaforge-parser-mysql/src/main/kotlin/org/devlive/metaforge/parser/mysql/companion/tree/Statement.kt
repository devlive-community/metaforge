package org.devlive.metaforge.parser.mysql.companion.tree

import org.devlive.metaforge.parser.mysql.companion.AstVisitor
import org.devlive.metaforge.parser.mysql.companion.ast.Node
import org.devlive.metaforge.parser.mysql.companion.ast.NodeLocation
import java.util.*

abstract class Statement internal constructor(location: Optional<NodeLocation>) : Node(location) {
    override fun <R, C> accept(visitor: AstVisitor<R, C>, context: C): R? {
        return visitor.visitStatement(this, context)
    }
}

