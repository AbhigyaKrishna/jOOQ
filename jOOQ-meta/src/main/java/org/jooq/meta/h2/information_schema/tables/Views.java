/*
 * This file is generated by jOOQ.
 */
package org.jooq.meta.h2.information_schema.tables;


import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.jooq.meta.h2.information_schema.InformationSchema;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Views extends TableImpl<Record> {

    private static final long serialVersionUID = 1909623426;

    /**
     * The reference instance of <code>INFORMATION_SCHEMA.VIEWS</code>
     */
    public static final Views VIEWS = new Views();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<Record> getRecordType() {
        return Record.class;
    }

    /**
     * The column <code>INFORMATION_SCHEMA.VIEWS.TABLE_CATALOG</code>.
     */
    public final TableField<Record, String> TABLE_CATALOG = createField(DSL.name("TABLE_CATALOG"), org.jooq.impl.SQLDataType.VARCHAR(2147483647), this, "");

    /**
     * The column <code>INFORMATION_SCHEMA.VIEWS.TABLE_SCHEMA</code>.
     */
    public final TableField<Record, String> TABLE_SCHEMA = createField(DSL.name("TABLE_SCHEMA"), org.jooq.impl.SQLDataType.VARCHAR(2147483647), this, "");

    /**
     * The column <code>INFORMATION_SCHEMA.VIEWS.TABLE_NAME</code>.
     */
    public final TableField<Record, String> TABLE_NAME = createField(DSL.name("TABLE_NAME"), org.jooq.impl.SQLDataType.VARCHAR(2147483647), this, "");

    /**
     * The column <code>INFORMATION_SCHEMA.VIEWS.VIEW_DEFINITION</code>.
     */
    public final TableField<Record, String> VIEW_DEFINITION = createField(DSL.name("VIEW_DEFINITION"), org.jooq.impl.SQLDataType.VARCHAR(2147483647), this, "");

    /**
     * The column <code>INFORMATION_SCHEMA.VIEWS.CHECK_OPTION</code>.
     */
    public final TableField<Record, String> CHECK_OPTION = createField(DSL.name("CHECK_OPTION"), org.jooq.impl.SQLDataType.VARCHAR(2147483647), this, "");

    /**
     * The column <code>INFORMATION_SCHEMA.VIEWS.IS_UPDATABLE</code>.
     */
    public final TableField<Record, String> IS_UPDATABLE = createField(DSL.name("IS_UPDATABLE"), org.jooq.impl.SQLDataType.VARCHAR(2147483647), this, "");

    /**
     * The column <code>INFORMATION_SCHEMA.VIEWS.STATUS</code>.
     */
    public final TableField<Record, String> STATUS = createField(DSL.name("STATUS"), org.jooq.impl.SQLDataType.VARCHAR(2147483647), this, "");

    /**
     * The column <code>INFORMATION_SCHEMA.VIEWS.REMARKS</code>.
     */
    public final TableField<Record, String> REMARKS = createField(DSL.name("REMARKS"), org.jooq.impl.SQLDataType.VARCHAR(2147483647), this, "");

    /**
     * The column <code>INFORMATION_SCHEMA.VIEWS.ID</code>.
     */
    public final TableField<Record, Integer> ID = createField(DSL.name("ID"), org.jooq.impl.SQLDataType.INTEGER, this, "");

    private Views(Name alias, Table<Record> aliased) {
        this(alias, aliased, null);
    }

    private Views(Name alias, Table<Record> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>INFORMATION_SCHEMA.VIEWS</code> table reference
     */
    public Views(String alias) {
        this(DSL.name(alias), VIEWS);
    }

    /**
     * Create an aliased <code>INFORMATION_SCHEMA.VIEWS</code> table reference
     */
    public Views(Name alias) {
        this(alias, VIEWS);
    }

    /**
     * Create a <code>INFORMATION_SCHEMA.VIEWS</code> table reference
     */
    public Views() {
        this(DSL.name("VIEWS"), null);
    }

    public <O extends Record> Views(Table<O> child, ForeignKey<O, Record> key) {
        super(child, key, VIEWS);
    }

    @Override
    public Schema getSchema() {
        return InformationSchema.INFORMATION_SCHEMA;
    }

    @Override
    public Views as(String alias) {
        return new Views(DSL.name(alias), this);
    }

    @Override
    public Views as(Name alias) {
        return new Views(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Views rename(String name) {
        return new Views(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Views rename(Name name) {
        return new Views(name, null);
    }
}
