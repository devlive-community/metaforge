package org.devlive.metaforge.sql;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class SQLParserErrorListener
        extends BaseErrorListener
{
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e)
    {
        throw new SQLParseException(String.format("line %d:%d %s", line, charPositionInLine, msg));
    }
}
