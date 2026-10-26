package org.devlive.metaforge.parser.mysql.companion.tree

import org.devlive.metaforge.parser.mysql.companion.ast.Node
import org.devlive.metaforge.parser.mysql.companion.ast.NodeLocation
import java.util.*

data class TableItem(override val location: Optional<NodeLocation>,
                     val name: String,
                     val alias: String) : Node(location)
