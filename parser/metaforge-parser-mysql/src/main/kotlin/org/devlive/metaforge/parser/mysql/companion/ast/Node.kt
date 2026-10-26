package org.devlive.metaforge.parser.mysql.companion.ast

import org.devlive.metaforge.parser.mysql.companion.AstVisitor
import java.util.*

abstract class Node(location: Optional<NodeLocation>) {
    open val location: Optional<NodeLocation> = Objects.requireNonNull(location, "location is null")

    protected open fun sameClass(left: Node, right: Node): Boolean {
        if (left == right) {
            return true
        }
        return left.javaClass == right.javaClass
    }

    open fun <R, C> accept(visitor: AstVisitor<R, C>, context: C): R? {
        return visitor.visitNode(this, context)
    }

    protected open fun shallowEquals(other: Node): Boolean {
        throw UnsupportedOperationException("not yet implemented: " + javaClass.name)
    }
}
