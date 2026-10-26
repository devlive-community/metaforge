package org.devlive.metaforge.parser;

import org.antlr.v4.runtime.RecognitionException;
import org.devlive.metaforge.parser.node.NodeLocation;

public class ParsingException
        extends RuntimeException
{
    private final int line;
    private final int charPositionInLine;

    public ParsingException(String message, RecognitionException cause, int line, int charPositionInLine)
    {
        super(message, cause);
        this.line = line;
        this.charPositionInLine = charPositionInLine;
    }

    public ParsingException(String message)
    {
        this(message, null, 1, 0);
    }

    public ParsingException(String message, NodeLocation nodeLocation)
    {
        this(message, null, nodeLocation.getLineNumber(), nodeLocation.getColumnNumber());
    }

    public int getLineNumber()
    {
        return line;
    }

    public int getColumnNumber()
    {
        // 由于索引是0所以需要在此处进行+1操作
        return charPositionInLine + 1;
    }

    public String getErrorMessage()
    {
        return super.getMessage();
    }

    @Override
    public String getMessage()
    {
        return String.format("line %s:%s: %s", getLineNumber(), getColumnNumber(), getErrorMessage());
    }
}
