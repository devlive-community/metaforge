package org.devlive.metaforge.parser.mysql.companion.tree

import org.devlive.metaforge.parser.mysql.companion.ast.Node
import org.devlive.metaforge.parser.mysql.companion.ast.NodeLocation
import java.util.*

class Limit(override val location: Optional<NodeLocation>,
            val children: Set<LimitItem>) : Node(location)
