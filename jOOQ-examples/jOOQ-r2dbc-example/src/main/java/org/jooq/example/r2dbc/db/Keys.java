/*
 * This file is generated by jOOQ.
 */
package org.jooq.example.r2dbc.db;


import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.example.r2dbc.db.tables.Author;
import org.jooq.example.r2dbc.db.tables.Book;
import org.jooq.example.r2dbc.db.tables.records.AuthorRecord;
import org.jooq.example.r2dbc.db.tables.records.BookRecord;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * R2DBC_EXAMPLE.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<AuthorRecord> PK_AUTHOR = Internal.createUniqueKey(Author.AUTHOR, DSL.name("PK_AUTHOR"), new TableField[] { Author.AUTHOR.ID }, true);
    public static final UniqueKey<BookRecord> PK_BOOK = Internal.createUniqueKey(Book.BOOK, DSL.name("PK_BOOK"), new TableField[] { Book.BOOK.ID }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<BookRecord, AuthorRecord> FK_BOOK_AUTHOR = Internal.createForeignKey(Book.BOOK, DSL.name("FK_BOOK_AUTHOR"), new TableField[] { Book.BOOK.ID }, Keys.PK_AUTHOR, new TableField[] { Author.AUTHOR.ID }, true);
}
