package org.devlive.metaforge.sql.processor;

import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.parser.SqlBaseBaseVisitor;
import org.devlive.metaforge.sql.parser.SqlBaseParser;

import java.util.ArrayList;
import java.util.List;

public class ExpressionProcessor
        extends SqlBaseBaseVisitor<Expression>
{
    @Override
    public Expression visitAndExpression(SqlBaseParser.AndExpressionContext ctx)
    {
        Expression expr = new Expression();
        expr.setType(Expression.ExpressionType.BINARY_OP);
        expr.setValue("AND");

        List<Expression> children = new ArrayList<>();
        children.add(visit(ctx.expression(0)));
        children.add(visit(ctx.expression(1)));
        expr.setChildren(children);

        return expr;
    }

    @Override
    public Expression visitOrExpression(SqlBaseParser.OrExpressionContext ctx)
    {
        Expression expr = new Expression();
        expr.setType(Expression.ExpressionType.BINARY_OP);
        expr.setValue("OR");

        List<Expression> children = new ArrayList<>();
        children.add(visit(ctx.expression(0)));
        children.add(visit(ctx.expression(1)));
        expr.setChildren(children);

        return expr;
    }

    @Override
    public Expression visitComparisonExpression(SqlBaseParser.ComparisonExpressionContext ctx)
    {
        Expression expr = new Expression();
        expr.setType(Expression.ExpressionType.BINARY_OP);
        expr.setValue(ctx.comparisonOperator().getText());

        List<Expression> children = new ArrayList<>();
        children.add(visit(ctx.expression(0)));
        children.add(visit(ctx.expression(1)));
        expr.setChildren(children);

        return expr;
    }

    @Override
    public Expression visitColumnReferencePrimary(SqlBaseParser.ColumnReferencePrimaryContext ctx)
    {
        Expression expr = new Expression();
        expr.setType(Expression.ExpressionType.COLUMN_REFERENCE);
        expr.setValue(ctx.columnReference().getText());
        return expr;
    }

    @Override
    public Expression visitLiteralPrimary(SqlBaseParser.LiteralPrimaryContext ctx)
    {
        Expression expr = new Expression();
        expr.setType(Expression.ExpressionType.LITERAL);

        if (ctx.literal().INTEGER_VALUE() != null) {
            expr.setValue(Integer.valueOf(ctx.literal().INTEGER_VALUE().getText()));
        }
        else if (ctx.literal().DECIMAL_VALUE() != null) {
            expr.setValue(Double.valueOf(ctx.literal().DECIMAL_VALUE().getText()));
        }
        else if (ctx.literal().STRING() != null) {
            expr.setValue(ctx.literal().getText());
        }
        else if (ctx.literal().TRUE() != null) {
            expr.setValue(true);
        }
        else if (ctx.literal().FALSE() != null) {
            expr.setValue(false);
        }
        else if (ctx.literal().NULL() != null) {
            expr.setValue(null);
        }
        return expr;
    }

    @Override
    public Expression visitParenExpression(SqlBaseParser.ParenExpressionContext ctx)
    {
        return visit(ctx.expression());
    }

    @Override
    public Expression visitFunctionCallPrimary(SqlBaseParser.FunctionCallPrimaryContext ctx)
    {
        Expression expr = new Expression();

        // 检查是否是 VERSION 函数
        // Check if it is a VERSION function
        if (ctx.functionCall().VERSION() != null) {
            expr.setType(Expression.ExpressionType.FUNCTION);
            expr.setValue("VERSION");
            return expr;
        }

        expr.setType(Expression.ExpressionType.FUNCTION);
        expr.setValue(ctx.functionCall().functionName().getText());

        // 直接获取函数参数的文本表示，而不是创建子表达式
        // Directly get the text representation of function parameters, instead of creating child expressions
        if (ctx.functionCall().expression() != null && !ctx.functionCall().expression().isEmpty()) {
            SqlBaseParser.ExpressionContext firstArg = ctx.functionCall().expression(0);
            String columnRef = firstArg.getText();

            // 创建一个单独的 COLUMN_REFERENCE 表达式
            // Create a separate COLUMN_REFERENCE expression
            Expression columnExpr = new Expression();
            columnExpr.setType(Expression.ExpressionType.COLUMN_REFERENCE);
            columnExpr.setValue(columnRef);

            List<Expression> args = new ArrayList<>();
            args.add(columnExpr);
            expr.setChildren(args);
        }

        return expr;
    }
}
