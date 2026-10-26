package org.devlive.metaforge.driver.parser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.sql.statement.ShowStatement;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
public class RedisShowParser
        extends RedisParser
{
    private String pattern;

    public RedisShowParser(ShowStatement statement)
    {
        parseShowStatement(statement);
    }

    public void parseShowStatement(ShowStatement show)
    {
        this.showType = show.getShowType();
        switch (show.getShowType()) {
            case DATABASES:
                this.command = "listDatabases";
                if (show.getPattern() != null) {
                    this.pattern = convertLikeToPattern(show.getPattern());
                }
                break;

            case TABLES:
                this.command = "listKeys";
                if (show.getDatabaseName() != null) {
                    this.database = show.getDatabaseName();
                }
                if (show.getPattern() != null) {
                    this.pattern = convertLikeToPattern(show.getPattern());
                }
                else {
                    this.pattern = "*";
                }
                break;

            case COLUMNS:
                this.command = "listFields";
                if (show.getDatabaseName() != null) {
                    this.database = show.getDatabaseName();
                }
                if (show.getTableName() != null) {
                    this.collection = show.getTableName();
                }
                if (show.getPattern() != null) {
                    this.pattern = convertLikeToPattern(show.getPattern());
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported SHOW type: " + show.getShowType());
        }
    }

    private String convertLikeToPattern(String likePattern)
    {
        if (likePattern.startsWith("'") && likePattern.endsWith("'")) {
            likePattern = likePattern.substring(1, likePattern.length() - 1);
        }
        else if (likePattern.startsWith("\"") && likePattern.endsWith("\"")) {
            likePattern = likePattern.substring(1, likePattern.length() - 1);
        }

        return likePattern
                .replace("%", "*")
                .replace("_", "?");
    }

    public String getPattern()
    {
        return pattern;
    }
}
