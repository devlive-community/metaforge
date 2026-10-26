package org.devlive.metaforge.parser.mysql.companion

import org.devlive.metaforge.parser.CaseInsensitiveStream
import org.devlive.metaforge.parser.mysql.companion.ast.ErrorHandler
import org.devlive.metaforge.parser.mysql.companion.ast.Node
import org.devlive.metaforge.parser.mysql.companion.ast.ParserOptions
import org.devlive.metaforge.parser.mysql.MySqlLexer
import org.devlive.metaforge.parser.mysql.MySqlParser
import org.devlive.metaforge.parser.mysql.MySqlParserBaseListener
import org.devlive.metaforge.parser.mysql.companion.tree.Statement
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Function

class MySQLEndpoint(options: ParserOptions = ParserOptions(), initializer: BiConsumer<MySqlLexer, MySqlParser> = DEFAULT_PARSER_INITIALIZER) {
    private val initializer: BiConsumer<MySqlLexer, MySqlParser> = Objects.requireNonNull(initializer, "initializer is null")
    private val enhancedErrorHandlerEnabled: Boolean = options.enhancedErrorHandlerEnabled

    fun createStatement(sql: String, parserOptions: ParserOptions.DecimalLiteralTreatment): Statement {
        return invokeParser("statement", sql, MySqlParser::sqlStatements, parserOptions) as Statement
    }

    private fun invokeParser(name: String, sql: String, parseFunction: Function<MySqlParser, ParserRuleContext>, parserOptions: ParserOptions.DecimalLiteralTreatment): Node {
        try {
            val lexer = MySqlLexer(CaseInsensitiveStream(CharStreams.fromString(sql)))
            val tokenStream = CommonTokenStream(lexer)
            val parser = MySqlParser(tokenStream)
            initializer.accept(lexer, parser)

            parser.addParseListener(PostProcessor(listOf(*parser.ruleNames), parser))

            lexer.removeErrorListeners()
            lexer.addErrorListener(LEXER_ERROR_LISTENER)

            parser.removeErrorListeners()

            if (enhancedErrorHandlerEnabled) {
                parser.addErrorListener(PARSER_ERROR_HANDLER)
            }
            else {
                parser.addErrorListener(LEXER_ERROR_LISTENER)
            }

            val tree: ParserRuleContext = try {
                // first, try parsing with potentially faster SLL mode
                parser.interpreter.predictionMode = PredictionMode.SLL
                parseFunction.apply(parser)
            }
            catch (ex: ParseCancellationException) {
                // if we fail, parse with LL mode
                tokenStream.seek(0) // rewind input stream
                parser.reset()
                parser.interpreter.predictionMode = PredictionMode.LL
                parseFunction.apply(parser)
            }

            return AstVisitorBuilder(parserOptions).visit(tree)
        }
        catch (e: StackOverflowError) {
            throw ParserException("$name is too large (stack overflow while parsing)")
        }
    }

    private class PostProcessor(private val ruleNames: List<String>, private val parser: MySqlParser) : MySqlParserBaseListener() {
    }

    companion object {
        private val LEXER_ERROR_LISTENER: BaseErrorListener = object : BaseErrorListener() {
            override fun syntaxError(
                    recognizer: Recognizer<*, *>?,
                    offendingSymbol: Any?,
                    line: Int,
                    charPositionInLine: Int,
                    message: String?,
                    e: RecognitionException?
            ) {
                throw ParserException(message.orEmpty(), e, line, charPositionInLine)
            }
        }

        private val DEFAULT_PARSER_INITIALIZER: BiConsumer<MySqlLexer, MySqlParser> = BiConsumer { _, _ -> }

        private val PARSER_ERROR_HANDLER: ErrorHandler = ErrorHandler.builder()
                .specialRule(MySqlParser.RULE_expression, "<expression>")
                .specialRule(MySqlParser.RULE_predicate, "<predicate>")
                .specialRule(MySqlParser.RULE_querySpecification, "<query>")
                .specialRule(MySqlParser.RULE_dataType, "<type>")
                .build()
    }
}

