/*
 * This file is generated by jOOQ.
 */
package org.jooq.meta.clickhouse.information_schema;


import java.util.Arrays;
import java.util.List;

import org.jooq.Table;
import org.jooq.impl.SchemaImpl;
import org.jooq.meta.clickhouse.information_schema.tables.Columns;
import org.jooq.meta.clickhouse.information_schema.tables.KeyColumnUsage;
import org.jooq.meta.clickhouse.information_schema.tables.Schemata;
import org.jooq.meta.clickhouse.information_schema.tables.Tables;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class InformationSchema extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>information_schema</code>
     */
    public static final InformationSchema INFORMATION_SCHEMA = new InformationSchema();

    /**
     * The table <code>information_schema.columns</code>.
     */
    public final Columns COLUMNS = Columns.COLUMNS;

    /**
     * The table <code>information_schema.key_column_usage</code>.
     */
    public final KeyColumnUsage KEY_COLUMN_USAGE = KeyColumnUsage.KEY_COLUMN_USAGE;

    /**
     * The table <code>information_schema.schemata</code>.
     */
    public final Schemata SCHEMATA = Schemata.SCHEMATA;

    /**
     * The table <code>information_schema.tables</code>.
     */
    public final Tables TABLES = Tables.TABLES;

    /**
     * No further instances allowed
     */
    private InformationSchema() {
        super("information_schema", null);
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            Columns.COLUMNS,
            KeyColumnUsage.KEY_COLUMN_USAGE,
            Schemata.SCHEMATA,
            Tables.TABLES
        );
    }
}
