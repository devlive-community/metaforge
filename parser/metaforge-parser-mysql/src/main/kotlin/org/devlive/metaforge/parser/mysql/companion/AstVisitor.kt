package org.devlive.metaforge.parser.mysql.companion

import org.devlive.metaforge.parser.mysql.companion.ast.Node
import org.devlive.metaforge.parser.mysql.companion.tree.Relation
import org.devlive.metaforge.parser.mysql.companion.tree.Select
import org.devlive.metaforge.parser.mysql.companion.tree.Statement

abstract class AstVisitor<R, C>
{
    fun visitNode(node: Node, context: C): R?
    {
        return null
    }

    fun visitStatement(node: Statement, context: C): R?
    {
        return visitNode(node, context)
    }

    fun visitSelect(node: Select, context: C): R?
    {
        return visitNode(node, context)
    }

    fun visitRelation(node: Relation, context: C): R?
    {
        return visitNode(node, context)
    }
}
