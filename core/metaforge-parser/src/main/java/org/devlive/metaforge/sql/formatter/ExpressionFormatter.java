package org.devlive.metaforge.sql.formatter;

import org.devlive.metaforge.sql.node.Expression;

import static org.devlive.metaforge.sql.formatter.SQLFormatter.INDENT;

public class ExpressionFormatter
{
    public ExpressionFormatter()
    {}

    public static String formatExpression(Expression expr, int depth)
    {
        if (expr == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        String currentIndent = INDENT.repeat(depth);

        // Start expression with current depth indentation
        sb.append(currentIndent).append("Expression {\n");

        // Content should be indented one more level
        String contentIndent = INDENT.repeat(depth + 1);
        sb.append(contentIndent).append("type: ").append(expr.getType()).append(",\n");
        sb.append(contentIndent).append("value: \"").append(expr.getValue()).append("\"");

        if (expr.getChildren() != null && !expr.getChildren().isEmpty()) {
            sb.append(",\n");
            sb.append(contentIndent).append("children: [\n");

            for (int i = 0; i < expr.getChildren().size(); i++) {
                Expression child = expr.getChildren().get(i);
                // Children should be indented one more level than the children array
                sb.append(formatExpression(child, depth + 2));

                if (i < expr.getChildren().size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }

            sb.append(contentIndent).append("]");
        }

        sb.append("\n").append(currentIndent).append("}");
        return sb.toString();
    }
}