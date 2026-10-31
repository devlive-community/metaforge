package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;

/**
 * One DDL statement a user generated for a table from the DDL tab. The user name is kept as it
 * was, so the record still reads correctly after the account is renamed or removed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("metaforge_source_ddl_history")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SourceDdlHistoryEntity
{
    @Id
    private Long id;

    @Column("source_code")
    private String sourceCode;

    @Column("database_name")
    private String databaseName;

    @Column("table_name")
    private String tableName;

    // create, structure, indexes, constraints, alter, alter-table or sample-data
    @Column("kind")
    private String kind;

    @Column("content")
    private String content;

    @Column("user_id")
    private Long userId;

    @Column("username")
    private String username;

    @Column("create_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
