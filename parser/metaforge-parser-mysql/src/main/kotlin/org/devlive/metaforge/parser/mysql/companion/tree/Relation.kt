package org.devlive.metaforge.parser.mysql.companion.tree

import org.devlive.metaforge.parser.mysql.companion.AstVisitor
import org.devlive.metaforge.parser.mysql.companion.ast.Node
import org.devlive.metaforge.parser.mysql.companion.ast.NodeLocation
import java.util.*

data class Relation(override val location: Optional<NodeLocation>,
                    val table: Table) : Node(location) {
    override fun <R, C> accept(visitor: AstVisitor<R, C>, context: C): R? {
        return visitor.visitRelation(this, context)
    }
}
