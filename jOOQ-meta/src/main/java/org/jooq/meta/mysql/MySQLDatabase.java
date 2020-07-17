/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Other licenses:
 * -----------------------------------------------------------------------------
 * Commercial licenses for this work are available. These replace the above
 * ASL 2.0 and offer limited warranties, support, maintenance, and commercial
 * database integrations.
 *
 * For more information, please visit: http://www.jooq.org/licenses
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.jooq.meta.mysql;

import static org.jooq.impl.DSL.falseCondition;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.when;
import static org.jooq.meta.mysql.information_schema.Tables.CHECK_CONSTRAINTS;
import static org.jooq.meta.mysql.information_schema.Tables.COLUMNS;
import static org.jooq.meta.mysql.information_schema.Tables.KEY_COLUMN_USAGE;
import static org.jooq.meta.mysql.information_schema.Tables.REFERENTIAL_CONSTRAINTS;
import static org.jooq.meta.mysql.information_schema.Tables.ROUTINES;
import static org.jooq.meta.mysql.information_schema.Tables.SCHEMATA;
import static org.jooq.meta.mysql.information_schema.Tables.STATISTICS;
import static org.jooq.meta.mysql.information_schema.Tables.TABLES;
import static org.jooq.meta.mysql.information_schema.Tables.TABLE_CONSTRAINTS;
import static org.jooq.meta.mysql.information_schema.Tables.VIEWS;
import static org.jooq.meta.mysql.mysql.Tables.PROC;

import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record5;
import org.jooq.Record6;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SortOrder;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions.TableType;
import org.jooq.impl.DSL;
import org.jooq.meta.AbstractDatabase;
import org.jooq.meta.AbstractIndexDefinition;
import org.jooq.meta.ArrayDefinition;
import org.jooq.meta.CatalogDefinition;
import org.jooq.meta.ColumnDefinition;
import org.jooq.meta.DefaultCheckConstraintDefinition;
import org.jooq.meta.DefaultEnumDefinition;
import org.jooq.meta.DefaultIndexColumnDefinition;
import org.jooq.meta.DefaultRelations;
import org.jooq.meta.DomainDefinition;
import org.jooq.meta.EnumDefinition;
import org.jooq.meta.IndexColumnDefinition;
import org.jooq.meta.IndexDefinition;
import org.jooq.meta.PackageDefinition;
import org.jooq.meta.RoutineDefinition;
import org.jooq.meta.SchemaDefinition;
import org.jooq.meta.SequenceDefinition;
import org.jooq.meta.TableDefinition;
import org.jooq.meta.UDTDefinition;
import org.jooq.meta.mariadb.MariaDBDatabase;
import org.jooq.meta.mysql.mysql.enums.ProcType;
import org.jooq.tools.csv.CSVReader;

/**
 * @author Lukas Eder
 */
public class MySQLDatabase extends AbstractDatabase {

    private static Boolean is8;
    private static Boolean is8_0_16;

    @Override
    protected List<IndexDefinition> getIndexes0() throws SQLException {
        List<IndexDefinition> result = new ArrayList<>();

        // Workaround for MemSQL bug
        // https://www.memsql.com/forum/t/wrong-query-result-on-information-schema-query/1423/2
        Table<?> from = getIncludeSystemIndexes()
            ? STATISTICS
            : STATISTICS.leftJoin(TABLE_CONSTRAINTS)
                .on(STATISTICS.INDEX_SCHEMA.eq(TABLE_CONSTRAINTS.CONSTRAINT_SCHEMA))
                .and(STATISTICS.TABLE_NAME.eq(TABLE_CONSTRAINTS.TABLE_NAME))
                .and(STATISTICS.INDEX_NAME.eq(TABLE_CONSTRAINTS.CONSTRAINT_NAME));

        // Same implementation as in H2Database and HSQLDBDatabase
        Map<Record, Result<Record>> indexes = create()
            // [#2059] In MemSQL primary key indexes are typically duplicated
            // (once with INDEX_TYPE = 'SHARD' and once with INDEX_TYPE = 'BTREE)
            .selectDistinct(
                STATISTICS.TABLE_SCHEMA,
                STATISTICS.TABLE_NAME,
                STATISTICS.INDEX_NAME,
                STATISTICS.NON_UNIQUE,
                STATISTICS.COLUMN_NAME,
                STATISTICS.SEQ_IN_INDEX)
            .from(from)
            // [#5213] Duplicate schema value to work around MySQL issue https://bugs.mysql.com/bug.php?id=86022
            .where(STATISTICS.TABLE_SCHEMA.in(getInputSchemata()).or(
                  getInputSchemata().size() == 1
                ? STATISTICS.TABLE_SCHEMA.in(getInputSchemata())
                : falseCondition()))
            .and(getIncludeSystemIndexes()
                ? noCondition()
                : TABLE_CONSTRAINTS.CONSTRAINT_NAME.isNull()
            )
            .orderBy(
                STATISTICS.TABLE_SCHEMA,
                STATISTICS.TABLE_NAME,
                STATISTICS.INDEX_NAME,
                STATISTICS.SEQ_IN_INDEX)
            .fetchGroups(
                new Field[] {
                    STATISTICS.TABLE_SCHEMA,
                    STATISTICS.TABLE_NAME,
                    STATISTICS.INDEX_NAME,
                    STATISTICS.NON_UNIQUE
                },
                new Field[] {
                    STATISTICS.COLUMN_NAME,
                    STATISTICS.SEQ_IN_INDEX
                });

        indexLoop:
        for (Entry<Record, Result<Record>> entry : indexes.entrySet()) {
            final Record index = entry.getKey();
            final Result<Record> columns = entry.getValue();

            final SchemaDefinition tableSchema = getSchema(index.get(STATISTICS.TABLE_SCHEMA));
            if (tableSchema == null)
                continue indexLoop;

            final String indexName = index.get(STATISTICS.INDEX_NAME);
            final String tableName = index.get(STATISTICS.TABLE_NAME);
            final TableDefinition table = getTable(tableSchema, tableName);
            if (table == null)
                continue indexLoop;

            final boolean unique = !index.get(STATISTICS.NON_UNIQUE, boolean.class);

            // [#6310] [#6620] Function-based indexes are not yet supported
            for (Record column : columns)
                if (table.getColumn(column.get(STATISTICS.COLUMN_NAME)) == null)
                    continue indexLoop;

            result.add(new AbstractIndexDefinition(tableSchema, indexName, table, unique) {
                List<IndexColumnDefinition> indexColumns = new ArrayList<>();

                {
                    for (Record column : columns) {
                        indexColumns.add(new DefaultIndexColumnDefinition(
                            this,
                            table.getColumn(column.get(STATISTICS.COLUMN_NAME)),
                            SortOrder.ASC,
                            column.get(STATISTICS.SEQ_IN_INDEX, int.class)
                        ));
                    }
                }

                @Override
                protected List<IndexColumnDefinition> getIndexColumns0() {
                    return indexColumns;
                }
            });
        }

        return result;
    }

    @Override
    protected void loadPrimaryKeys(DefaultRelations relations) throws SQLException {
        for (Record record : fetchKeys(true)) {
            SchemaDefinition schema = getSchema(record.get(STATISTICS.TABLE_SCHEMA));
            String constraintName = record.get(STATISTICS.INDEX_NAME);
            String tableName = record.get(STATISTICS.TABLE_NAME);
            String columnName = record.get(STATISTICS.COLUMN_NAME);

            String key = getKeyName(tableName, constraintName);
            TableDefinition table = getTable(schema, tableName);

            if (table != null)
                relations.addPrimaryKey(key, table, table.getColumn(columnName));
        }
    }

    @Override
    protected void loadUniqueKeys(DefaultRelations relations) throws SQLException {
        for (Record record : fetchKeys(false)) {
            SchemaDefinition schema = getSchema(record.get(STATISTICS.TABLE_SCHEMA));
            String constraintName = record.get(STATISTICS.INDEX_NAME);
            String tableName = record.get(STATISTICS.TABLE_NAME);
            String columnName = record.get(STATISTICS.COLUMN_NAME);

            String key = getKeyName(tableName, constraintName);
            TableDefinition table = getTable(schema, tableName);

            if (table != null)
                relations.addUniqueKey(key, table, table.getColumn(columnName));
        }
    }

    private String getKeyName(String tableName, String keyName) {
        return "KEY_" + tableName + "_" + keyName;
    }

    protected boolean is8() {

        // [#6602] The mysql.proc table got removed in MySQL 8.0
        if (is8 == null)
            is8 = !exists(PROC);

        return is8;
    }

    protected boolean is8_0_16() {

        // [#7639] The information_schema.check_constraints table was added in MySQL 8.0.16 only
        if (is8_0_16 == null)
            is8_0_16 = exists(CHECK_CONSTRAINTS);

        return is8_0_16;
    }

    private Result<?> fetchKeys(boolean primary) {

        // [#3560] It has been shown that querying the STATISTICS table is much faster on
        // very large databases than going through TABLE_CONSTRAINTS and KEY_COLUMN_USAGE
        // [#2059] In MemSQL primary key indexes are typically duplicated
        // (once with INDEX_TYPE = 'SHARD' and once with INDEX_TYPE = 'BTREE)
        return create().selectDistinct(
                           STATISTICS.TABLE_SCHEMA,
                           STATISTICS.TABLE_NAME,
                           STATISTICS.COLUMN_NAME,
                           STATISTICS.INDEX_NAME,
                           STATISTICS.SEQ_IN_INDEX)
                       .from(STATISTICS)
                       // [#5213] Duplicate schema value to work around MySQL issue https://bugs.mysql.com/bug.php?id=86022
                       .where(STATISTICS.TABLE_SCHEMA.in(getInputSchemata()).or(
                             getInputSchemata().size() == 1
                           ? STATISTICS.TABLE_SCHEMA.in(getInputSchemata())
                           : falseCondition()))
                       .and(primary
                           ? STATISTICS.INDEX_NAME.eq(inline("PRIMARY"))
                           : STATISTICS.INDEX_NAME.ne(inline("PRIMARY")).and(STATISTICS.NON_UNIQUE.eq(inline(0))))
                       .orderBy(
                           STATISTICS.TABLE_SCHEMA,
                           STATISTICS.TABLE_NAME,
                           STATISTICS.INDEX_NAME,
                           STATISTICS.SEQ_IN_INDEX)
                       .fetch();
    }

    @Override
    protected void loadForeignKeys(DefaultRelations relations) throws SQLException {
        for (Record record : create().select(
                    REFERENTIAL_CONSTRAINTS.CONSTRAINT_SCHEMA,
                    REFERENTIAL_CONSTRAINTS.CONSTRAINT_NAME,
                    REFERENTIAL_CONSTRAINTS.TABLE_NAME,
                    REFERENTIAL_CONSTRAINTS.REFERENCED_TABLE_NAME,
                    REFERENTIAL_CONSTRAINTS.UNIQUE_CONSTRAINT_NAME,
                    REFERENTIAL_CONSTRAINTS.UNIQUE_CONSTRAINT_SCHEMA,
                    KEY_COLUMN_USAGE.COLUMN_NAME)
                .from(REFERENTIAL_CONSTRAINTS)
                .join(KEY_COLUMN_USAGE)
                .on(REFERENTIAL_CONSTRAINTS.CONSTRAINT_SCHEMA.equal(KEY_COLUMN_USAGE.CONSTRAINT_SCHEMA))
                .and(REFERENTIAL_CONSTRAINTS.CONSTRAINT_NAME.equal(KEY_COLUMN_USAGE.CONSTRAINT_NAME))
                .and(REFERENTIAL_CONSTRAINTS.TABLE_NAME.equal(KEY_COLUMN_USAGE.TABLE_NAME))
                // [#5213] Duplicate schema value to work around MySQL issue https://bugs.mysql.com/bug.php?id=86022
                .where(REFERENTIAL_CONSTRAINTS.CONSTRAINT_SCHEMA.in(getInputSchemata()).or(
                      getInputSchemata().size() == 1
                    ? REFERENTIAL_CONSTRAINTS.CONSTRAINT_SCHEMA.in(getInputSchemata())
                    : falseCondition()))
                .orderBy(
                    KEY_COLUMN_USAGE.CONSTRAINT_SCHEMA.asc(),
                    KEY_COLUMN_USAGE.CONSTRAINT_NAME.asc(),
                    KEY_COLUMN_USAGE.ORDINAL_POSITION.asc())
                .fetch()) {

            SchemaDefinition foreignKeySchema = getSchema(record.get(REFERENTIAL_CONSTRAINTS.CONSTRAINT_SCHEMA));
            SchemaDefinition uniqueKeySchema = getSchema(record.get(REFERENTIAL_CONSTRAINTS.UNIQUE_CONSTRAINT_SCHEMA));

            String foreignKey = record.get(REFERENTIAL_CONSTRAINTS.CONSTRAINT_NAME);
            String foreignKeyColumn = record.get(KEY_COLUMN_USAGE.COLUMN_NAME);
            String foreignKeyTableName = record.get(REFERENTIAL_CONSTRAINTS.TABLE_NAME);
            String uniqueKey = record.get(REFERENTIAL_CONSTRAINTS.UNIQUE_CONSTRAINT_NAME);
            String uniqueKeyTableName = record.get(REFERENTIAL_CONSTRAINTS.REFERENCED_TABLE_NAME);

            TableDefinition foreignKeyTable = getTable(foreignKeySchema, foreignKeyTableName);
            TableDefinition uniqueKeyTable = getTable(uniqueKeySchema, uniqueKeyTableName);

            if (foreignKeyTable != null)
                relations.addForeignKey(
                    foreignKey,
                    foreignKeyTable,
                    foreignKeyTable.getColumn(foreignKeyColumn),
                    getKeyName(uniqueKeyTableName, uniqueKey),
                    uniqueKeyTable
                );
        }
    }

    @Override
    protected void loadCheckConstraints(DefaultRelations relations) throws SQLException {
        if (is8_0_16()) {
            for (Record record : create()
                    .select(
                        TABLE_CONSTRAINTS.TABLE_SCHEMA,
                        TABLE_CONSTRAINTS.TABLE_NAME,
                        CHECK_CONSTRAINTS.CONSTRAINT_NAME,
                        CHECK_CONSTRAINTS.CHECK_CLAUSE,

                        // We need this additional, useless projection. See:
                        // https://jira.mariadb.org/browse/MDEV-21201
                        TABLE_CONSTRAINTS.CONSTRAINT_CATALOG,
                        TABLE_CONSTRAINTS.CONSTRAINT_SCHEMA
                     )
                    .from(TABLE_CONSTRAINTS)
                    .join(CHECK_CONSTRAINTS)
                    .using(this instanceof MariaDBDatabase
                        ? new Field[] {
                            TABLE_CONSTRAINTS.CONSTRAINT_CATALOG,
                            TABLE_CONSTRAINTS.CONSTRAINT_SCHEMA,
                            // MariaDB has this column, but not MySQL
                            TABLE_CONSTRAINTS.TABLE_NAME,
                            TABLE_CONSTRAINTS.CONSTRAINT_NAME
                        }
                        : new Field[] {
                            TABLE_CONSTRAINTS.CONSTRAINT_CATALOG,
                            TABLE_CONSTRAINTS.CONSTRAINT_SCHEMA,
                            TABLE_CONSTRAINTS.CONSTRAINT_NAME
                        }
                    )
                    .where(TABLE_CONSTRAINTS.TABLE_SCHEMA.in(getInputSchemata()))
                    .orderBy(
                        TABLE_CONSTRAINTS.TABLE_SCHEMA,
                        TABLE_CONSTRAINTS.TABLE_NAME,
                        TABLE_CONSTRAINTS.CONSTRAINT_NAME)) {

                SchemaDefinition schema = getSchema(record.get(TABLE_CONSTRAINTS.TABLE_SCHEMA));
                TableDefinition table = getTable(schema, record.get(TABLE_CONSTRAINTS.TABLE_NAME));

                if (table != null) {
                    relations.addCheckConstraint(table, new DefaultCheckConstraintDefinition(
                        schema,
                        table,
                        record.get(CHECK_CONSTRAINTS.CONSTRAINT_NAME),
                        record.get(CHECK_CONSTRAINTS.CHECK_CLAUSE)
                    ));
                }
            }
        }
    }

    @Override
    protected List<CatalogDefinition> getCatalogs0() throws SQLException {
        List<CatalogDefinition> result = new ArrayList<>();
        result.add(new CatalogDefinition(this, "", ""));
        return result;
    }

    @Override
    protected List<SchemaDefinition> getSchemata0() throws SQLException {
        List<SchemaDefinition> result = new ArrayList<>();

        for (String name : create()
                .select(SCHEMATA.SCHEMA_NAME)
                .from(SCHEMATA)
                .fetch(SCHEMATA.SCHEMA_NAME)) {

            result.add(new SchemaDefinition(this, name, ""));
        }

        return result;
    }

    @Override
    protected List<SequenceDefinition> getSequences0() throws SQLException {
        List<SequenceDefinition> result = new ArrayList<>();
        return result;
    }

    @Override
    protected List<TableDefinition> getTables0() throws SQLException {
        List<TableDefinition> result = new ArrayList<>();

        for (Record record : create().select(
                TABLES.TABLE_SCHEMA,
                TABLES.TABLE_NAME,
                TABLES.TABLE_COMMENT,
                when(TABLES.TABLE_TYPE.eq(inline("VIEW")), inline(TableType.VIEW.name()))
                    .else_(inline(TableType.TABLE.name())).as("table_type"),
                when(VIEWS.VIEW_DEFINITION.lower().like(inline("create%")), VIEWS.VIEW_DEFINITION)
                    .else_(inline("create view `").concat(TABLES.TABLE_NAME).concat("` as ").concat(VIEWS.VIEW_DEFINITION)).as(VIEWS.VIEW_DEFINITION))
            .from(TABLES)
            .leftJoin(VIEWS)
                .on(TABLES.TABLE_SCHEMA.eq(VIEWS.TABLE_SCHEMA))
                .and(TABLES.TABLE_NAME.eq(VIEWS.TABLE_NAME))

            // [#5213] Duplicate schema value to work around MySQL issue https://bugs.mysql.com/bug.php?id=86022
            .where(TABLES.TABLE_SCHEMA.in(getInputSchemata()).or(
                  getInputSchemata().size() == 1
                ? TABLES.TABLE_SCHEMA.in(getInputSchemata())
                : falseCondition()))

            // [#9291] MariaDB treats sequences as tables
            .and(TABLES.TABLE_TYPE.ne(inline("SEQUENCE")))
            .orderBy(
                TABLES.TABLE_SCHEMA,
                TABLES.TABLE_NAME)) {

            SchemaDefinition schema = getSchema(record.get(TABLES.TABLE_SCHEMA));
            String name = record.get(TABLES.TABLE_NAME);
            String comment = record.get(TABLES.TABLE_COMMENT);
            TableType tableType = record.get("table_type", TableType.class);
            String source = record.get(VIEWS.VIEW_DEFINITION);

            MySQLTableDefinition table = new MySQLTableDefinition(schema, name, comment, tableType, source);
            result.add(table);
        }

        return result;
    }

    @Override
    protected List<EnumDefinition> getEnums0() throws SQLException {
        List<EnumDefinition> result = new ArrayList<>();

        Result<Record5<String, String, String, String, String>> records = create()
            .select(
                COLUMNS.TABLE_SCHEMA,
                COLUMNS.COLUMN_COMMENT,
                COLUMNS.TABLE_NAME,
                COLUMNS.COLUMN_NAME,
                COLUMNS.COLUMN_TYPE)
            .from(COLUMNS)
            .where(
                COLUMNS.COLUMN_TYPE.like("enum(%)").and(
                // [#5213] Duplicate schema value to work around MySQL issue https://bugs.mysql.com/bug.php?id=86022
                COLUMNS.TABLE_SCHEMA.in(getInputSchemata()).or(
                      getInputSchemata().size() == 1
                    ? COLUMNS.TABLE_SCHEMA.in(getInputSchemata())
                    : falseCondition())))
            .orderBy(
                COLUMNS.TABLE_SCHEMA.asc(),
                COLUMNS.TABLE_NAME.asc(),
                COLUMNS.COLUMN_NAME.asc())
            .fetch();

        for (Record record : records) {
            SchemaDefinition schema = getSchema(record.get(COLUMNS.TABLE_SCHEMA));

            String comment = record.get(COLUMNS.COLUMN_COMMENT);
            String table = record.get(COLUMNS.TABLE_NAME);
            String column = record.get(COLUMNS.COLUMN_NAME);
            String name = table + "_" + column;
            String columnType = record.get(COLUMNS.COLUMN_TYPE);

            // [#1237] Don't generate enum classes for columns in MySQL tables
            // that are excluded from code generation
            TableDefinition tableDefinition = getTable(schema, table);
            if (tableDefinition != null) {
                ColumnDefinition columnDefinition = tableDefinition.getColumn(column);

                if (columnDefinition != null) {

                    // [#1137] Avoid generating enum classes for enum types that
                    // are explicitly forced to another type
                    if (getConfiguredForcedType(columnDefinition, columnDefinition.getType()) == null) {
                        DefaultEnumDefinition definition = new DefaultEnumDefinition(schema, name, comment);

                        CSVReader reader = new CSVReader(
                            new StringReader(columnType.replaceAll("(^enum\\()|(\\)$)", ""))
                           ,','  // Separator
                           ,'\'' // Quote character
                           ,true // Strict quotes
                        );

                        for (String string : reader.next()) {
                            definition.addLiteral(string);
                        }

                        result.add(definition);
                    }
                }
            }
        }

        return result;
    }

    @Override
    protected List<DomainDefinition> getDomains0() throws SQLException {
        List<DomainDefinition> result = new ArrayList<>();
        return result;
    }

    @Override
    protected List<UDTDefinition> getUDTs0() throws SQLException {
        List<UDTDefinition> result = new ArrayList<>();
        return result;
    }

    @Override
    protected List<ArrayDefinition> getArrays0() throws SQLException {
        List<ArrayDefinition> result = new ArrayList<>();
        return result;
    }

    @Override
    protected List<RoutineDefinition> getRoutines0() throws SQLException {
        List<RoutineDefinition> result = new ArrayList<>();

        Result<Record6<String, String, String, byte[], byte[], ProcType>> records = is8()

            ? create().select(
                    ROUTINES.ROUTINE_SCHEMA,
                    ROUTINES.ROUTINE_NAME,
                    ROUTINES.ROUTINE_COMMENT,
                    inline(new byte[0]).as(PROC.PARAM_LIST),
                    inline(new byte[0]).as(PROC.RETURNS),
                    ROUTINES.ROUTINE_TYPE.coerce(PROC.TYPE).as(ROUTINES.ROUTINE_TYPE))
                .from(ROUTINES)
                .where(ROUTINES.ROUTINE_SCHEMA.in(getInputSchemata()))
                .orderBy(1, 2, 6)
                .fetch()

            : create().select(
                    PROC.DB.as(ROUTINES.ROUTINE_SCHEMA),
                    PROC.NAME.as(ROUTINES.ROUTINE_NAME),
                    PROC.COMMENT.as(ROUTINES.ROUTINE_COMMENT),
                    PROC.PARAM_LIST,
                    PROC.RETURNS,
                    PROC.TYPE.as(ROUTINES.ROUTINE_TYPE))
                .from(PROC)
                .where(PROC.DB.in(getInputSchemata()))
                .orderBy(1, 2, 6)
                .fetch();

        Map<Record, Result<Record6<String, String, String, byte[], byte[], ProcType>>> groups =
            records.intoGroups(new Field[] { ROUTINES.ROUTINE_SCHEMA, ROUTINES.ROUTINE_NAME });

        // [#1908] This indirection is necessary as MySQL allows for overloading
        // procedures and functions with the same signature.
        for (Entry<Record, Result<Record6<String, String, String, byte[], byte[], ProcType>>> entry : groups.entrySet()) {
            Result<?> overloads = entry.getValue();

            for (int i = 0; i < overloads.size(); i++) {
                Record record = overloads.get(i);

                SchemaDefinition schema = getSchema(record.get(ROUTINES.ROUTINE_SCHEMA));
                String name = record.get(ROUTINES.ROUTINE_NAME);
                String comment = record.get(ROUTINES.ROUTINE_COMMENT);
                String params = is8() ? "" : new String(record.get(PROC.PARAM_LIST));
                String returns = is8() ? "" : new String(record.get(PROC.RETURNS));
                ProcType type = record.get(ROUTINES.ROUTINE_TYPE.coerce(PROC.TYPE).as(ROUTINES.ROUTINE_TYPE));

                if (overloads.size() > 1)
                    result.add(new MySQLRoutineDefinition(schema, name, comment, params, returns, type, "_" + type.name()));
                else
                    result.add(new MySQLRoutineDefinition(schema, name, comment, params, returns, type, null));
            }
        }

        return result;
    }

    @Override
    protected List<PackageDefinition> getPackages0() throws SQLException {
        List<PackageDefinition> result = new ArrayList<>();
        return result;
    }

    @Override
    protected DSLContext create0() {
        return DSL.using(getConnection(), SQLDialect.MYSQL);
    }

    @Override
    protected boolean exists0(TableField<?, ?> field) {
        return exists1(field, COLUMNS.COLUMNS, COLUMNS.TABLE_SCHEMA, COLUMNS.TABLE_NAME, COLUMNS.COLUMN_NAME);
    }

    @Override
    protected boolean exists0(Table<?> table) {
        return exists1(table, TABLES.TABLES, TABLES.TABLE_SCHEMA, TABLES.TABLE_NAME);
    }
}
