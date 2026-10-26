package org.devlive.metaforge.parser.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.devlive.metaforge.parser.CaseInsensitiveStream;
import org.devlive.metaforge.parser.ParsingException;
import org.devlive.metaforge.parser.antlr.SqlBaseBaseListener;
import org.devlive.metaforge.parser.antlr.SqlBaseLexer;
import org.devlive.metaforge.parser.antlr.SqlBaseParser;
import org.devlive.metaforge.parser.ast.AstVisitorBuilder;
import org.devlive.metaforge.parser.node.Node;
import org.devlive.metaforge.parser.tree.DataType;
import org.devlive.metaforge.parser.tree.Expression;
import org.devlive.metaforge.parser.tree.PathSpecification;
import org.devlive.metaforge.parser.tree.Statement;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class SqlParser
{
    private static final BaseErrorListener LEXER_ERROR_LISTENER = new BaseErrorListener()
    {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String message, RecognitionException e)
        {
            throw new ParsingException(message, e, line, charPositionInLine);
        }
    };

    private static final BiConsumer<SqlBaseLexer, SqlBaseParser> DEFAULT_PARSER_INITIALIZER = (SqlBaseLexer lexer, SqlBaseParser parser) -> {
    };

    private static final ErrorHandler PARSER_ERROR_HANDLER = ErrorHandler.builder()
            .specialRule(SqlBaseParser.RULE_expression, "<expression>")
            .specialRule(SqlBaseParser.RULE_booleanExpression, "<expression>")
            .specialRule(SqlBaseParser.RULE_valueExpression, "<expression>")
            .specialRule(SqlBaseParser.RULE_primaryExpression, "<expression>")
            .specialRule(SqlBaseParser.RULE_predicate, "<predicate>")
            .specialRule(SqlBaseParser.RULE_identifier, "<identifier>")
            .specialRule(SqlBaseParser.RULE_string, "<string>")
            .specialRule(SqlBaseParser.RULE_query, "<query>")
            .specialRule(SqlBaseParser.RULE_type, "<type>")
            .specialToken(SqlBaseLexer.INTEGER_VALUE, "<integer>")
            .ignoredRule(SqlBaseParser.RULE_nonReserved)
            .build();

    private final BiConsumer<SqlBaseLexer, SqlBaseParser> initializer;
    private final boolean enhancedErrorHandlerEnabled;

    public SqlParser()
    {
        this(new SqlParserOptions());
    }

    public SqlParser(SqlParserOptions options)
    {
        this(options, DEFAULT_PARSER_INITIALIZER);
    }

    public SqlParser(SqlParserOptions options, BiConsumer<SqlBaseLexer, SqlBaseParser> initializer)
    {
        this.initializer = requireNonNull(initializer, "initializer is null");
        requireNonNull(options, "options is null");
        enhancedErrorHandlerEnabled = options.isEnhancedErrorHandlerEnabled();
    }

    public Statement createStatement(String sql, ParserOptions parserOptions)
    {
        return (Statement) invokeParser("statement", sql, SqlBaseParser::singleStatement, parserOptions);
    }

    public Expression createExpression(String expression, ParserOptions parserOptions)
    {
        return (Expression) invokeParser("expression", expression, SqlBaseParser::standaloneExpression, parserOptions);
    }

    public DataType createType(String expression)
    {
        return (DataType) invokeParser("type", expression, SqlBaseParser::standaloneType, new ParserOptions());
    }

    public PathSpecification createPathSpecification(String expression)
    {
        return (PathSpecification) invokeParser("path specification", expression, SqlBaseParser::standalonePathSpecification, new ParserOptions());
    }

    private Node invokeParser(String name, String sql, Function<SqlBaseParser, ParserRuleContext> parseFunction, ParserOptions parserOptions)
    {
        try {
            SqlBaseLexer lexer = new SqlBaseLexer(new CaseInsensitiveStream(CharStreams.fromString(sql)));
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            SqlBaseParser parser = new SqlBaseParser(tokenStream);
            initializer.accept(lexer, parser);

            parser.addParseListener(new PostProcessor(Arrays.asList(parser.getRuleNames()), parser));

            lexer.removeErrorListeners();
            lexer.addErrorListener(LEXER_ERROR_LISTENER);

            parser.removeErrorListeners();

            if (enhancedErrorHandlerEnabled) {
                parser.addErrorListener(PARSER_ERROR_HANDLER);
            }
            else {
                parser.addErrorListener(LEXER_ERROR_LISTENER);
            }

            ParserRuleContext tree;
            try {
                // first, try parsing with potentially faster SLL mode
                parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
                tree = parseFunction.apply(parser);
            }
            catch (ParseCancellationException ex) {
                // if we fail, parse with LL mode
                tokenStream.seek(0); // rewind input stream
                parser.reset();

                parser.getInterpreter().setPredictionMode(PredictionMode.LL);
                tree = parseFunction.apply(parser);
            }

            return new AstVisitorBuilder(parserOptions).visit(tree);
        }
        catch (StackOverflowError e) {
            throw new ParsingException(name + " is too large (stack overflow while parsing)");
        }
    }

    private static class PostProcessor
            extends SqlBaseBaseListener
    {
        private final List<String> ruleNames;
        private final SqlBaseParser parser;

        public PostProcessor(List<String> ruleNames, SqlBaseParser parser)
        {
            this.ruleNames = ruleNames;
            this.parser = parser;
        }

        @Override
        public void exitQuotedIdentifier(SqlBaseParser.QuotedIdentifierContext context)
        {
            Token token = context.QUOTED_IDENTIFIER().getSymbol();
            if (token.getText().length() == 2) { // empty identifier
                throw new ParsingException("Zero-length delimited identifier not allowed", null, token.getLine(), token.getCharPositionInLine());
            }
        }

        @Override
        public void exitBackQuotedIdentifier(SqlBaseParser.BackQuotedIdentifierContext context)
        {
            Token token = context.BACKQUOTED_IDENTIFIER().getSymbol();
            throw new ParsingException(
                    "backquoted identifiers are not supported; use double quotes to quote identifiers",
                    null,
                    token.getLine(),
                    token.getCharPositionInLine());
        }

        @Override
        public void exitDigitIdentifier(SqlBaseParser.DigitIdentifierContext context)
        {
            Token token = context.DIGIT_IDENTIFIER().getSymbol();
            throw new ParsingException(
                    "identifiers must not start with a digit; surround the identifier with double quotes",
                    null,
                    token.getLine(),
                    token.getCharPositionInLine());
        }

        @Override
        public void exitNonReserved(SqlBaseParser.NonReservedContext context)
        {
            // we can't modify the tree during rule enter/exit event handling unless we're dealing with a terminal.
            // Otherwise, ANTLR gets confused an fires spurious notifications.
            if (!(context.getChild(0) instanceof TerminalNode)) {
                int rule = ((ParserRuleContext) context.getChild(0)).getRuleIndex();
                throw new AssertionError("nonReserved can only contain tokens. Found nested rule: " + ruleNames.get(rule));
            }

            // replace nonReserved words with IDENT tokens
            context.getParent().removeLastChild();

            Token token = (Token) context.getChild(0).getPayload();
            Token newToken = new CommonToken(
                    new Pair<>(token.getTokenSource(), token.getInputStream()),
                    SqlBaseLexer.IDENTIFIER,
                    token.getChannel(),
                    token.getStartIndex(),
                    token.getStopIndex());

            context.getParent().addChild(parser.createTerminalNode(context.getParent(), newToken));
        }
    }
}
