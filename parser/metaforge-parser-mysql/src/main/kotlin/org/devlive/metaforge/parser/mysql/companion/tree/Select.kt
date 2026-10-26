package org.devlive.metaforge.parser.mysql.companion.tree

import org.devlive.metaforge.parser.mysql.companion.AstVisitor
import org.devlive.metaforge.parser.mysql.companion.ast.Node
import org.devlive.metaforge.parser.mysql.companion.ast.NodeLocation
import java.util.*

data class Select(override val location: Optional<NodeLocation>,
                  val distinct: Boolean,
                  val children: List<SelectItem>) : Node(location) {
    override fun <R, C> accept(visitor: AstVisitor<R, C>, context: C): R? {
        return visitor.visitSelect(this, context)
    }
}
