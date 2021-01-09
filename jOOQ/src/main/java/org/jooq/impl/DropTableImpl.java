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
package org.jooq.impl;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.Internal.*;
import static org.jooq.impl.Keywords.*;
import static org.jooq.impl.Names.*;
import static org.jooq.impl.SQLDataType.*;
import static org.jooq.impl.Tools.*;
import static org.jooq.impl.Tools.BooleanDataKey.*;
import static org.jooq.SQLDialect.*;

import org.jooq.*;
import org.jooq.impl.*;
import org.jooq.tools.*;

import java.util.*;


/**
 * The <code>DROP TABLE</code> statement.
 */
@SuppressWarnings({ "rawtypes", "unused" })
final class DropTableImpl
extends
    AbstractRowCountQuery
implements
    DropTableStep,
    DropTableFinalStep
{

    private static final long serialVersionUID = 1L;

    private final Boolean  temporary;
    private final Table<?> table;
    private final boolean  dropTableIfExists;
    private       Boolean  cascade;

    DropTableImpl(
        Configuration configuration,
        Boolean temporary,
        Table<?> table,
        boolean dropTableIfExists
    ) {
        this(
            configuration,
            temporary,
            table,
            dropTableIfExists,
            null
        );
    }

    DropTableImpl(
        Configuration configuration,
        Boolean temporary,
        Table<?> table,
        boolean dropTableIfExists,
        Boolean cascade
    ) {
        super(configuration);

        this.temporary = temporary;
        this.table = table;
        this.dropTableIfExists = dropTableIfExists;
        this.cascade = cascade;
    }

    final Boolean  $temporary()         { return temporary; }
    final Table<?> $table()             { return table; }
    final boolean  $dropTableIfExists() { return dropTableIfExists; }
    final Boolean  $cascade()           { return cascade; }

    // -------------------------------------------------------------------------
    // XXX: DSL API
    // -------------------------------------------------------------------------
    
    @Override
    public final DropTableImpl cascade() {
        this.cascade = true;
        return this;
    }

    @Override
    public final DropTableImpl restrict() {
        this.cascade = false;
        return this;
    }

    // -------------------------------------------------------------------------
    // XXX: QueryPart API
    // -------------------------------------------------------------------------



    private static final Clause[]        CLAUSES              = { Clause.DROP_TABLE };
    private static final Set<SQLDialect> NO_SUPPORT_IF_EXISTS = SQLDialect.supportedBy(DERBY, FIREBIRD);
    private static final Set<SQLDialect> TEMPORARY_SEMANTIC   = SQLDialect.supportedBy(MYSQL);

    private final boolean supportsIfExists(Context<?> ctx) {
        return !NO_SUPPORT_IF_EXISTS.contains(ctx.dialect());
    }

    @Override
    public final void accept(Context<?> ctx) {
        if (dropTableIfExists && !supportsIfExists(ctx)) {
            Tools.beginTryCatch(ctx, DDLStatementType.DROP_TABLE);
            accept0(ctx);
            Tools.endTryCatch(ctx, DDLStatementType.DROP_TABLE);
        }
        else
            accept0(ctx);
    }

    private void accept0(Context<?> ctx) {
        ctx.start(Clause.DROP_TABLE_TABLE);

        // [#6371] [#9019] While many dialects do not require this keyword, in
        //                 some dialects (e.g. MySQL), there is a semantic
        //                 difference, e.g. with respect to transactions.
        if (temporary && TEMPORARY_SEMANTIC.contains(ctx.dialect()))
            ctx.visit(K_DROP).sql(' ').visit(K_TEMPORARY).sql(' ').visit(K_TABLE).sql(' ');
        else
            ctx.visit(K_DROP_TABLE).sql(' ');


        if (dropTableIfExists && supportsIfExists(ctx))
            ctx.visit(K_IF_EXISTS).sql(' ');

        ctx.visit(table);

        if (Boolean.TRUE.equals(cascade))
            ctx.sql(' ').visit(K_CASCADE);
        else if (Boolean.FALSE.equals(cascade))
            ctx.sql(' ').visit(K_RESTRICT);

        ctx.end(Clause.DROP_TABLE_TABLE);
    }


    @Override
    public final Clause[] clauses(Context<?> ctx) {
        return CLAUSES;
    }


}
