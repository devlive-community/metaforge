package org.devlive.metaforge.parser.mysql.companion.tree

import org.devlive.metaforge.parser.mysql.companion.ast.NodeLocation
import java.util.*

data class QuerySpecification(override val location: Optional<NodeLocation>,
                              val select: Select,
                              val from: Relation,
                              val limit: Limit) : Statement(location)
