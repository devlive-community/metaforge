package org.devlive.metaforge.parser.mysql.companion.tree

import org.devlive.metaforge.parser.mysql.companion.ast.Node
import org.devlive.metaforge.parser.mysql.companion.ast.NodeLocation

import java.util.*

data class SelectItem(
        override val location: Optional<NodeLocation>,
        val name: String
) : Node(location)
