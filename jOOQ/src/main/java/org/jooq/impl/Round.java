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
import static org.jooq.impl.Tools.DataExtendedKey.*;
import static org.jooq.impl.Tools.DataKey.*;
import static org.jooq.SQLDialect.*;

import org.jooq.*;
import org.jooq.Function1;
import org.jooq.Record;
import org.jooq.conf.*;
import org.jooq.impl.*;
import org.jooq.impl.QOM.*;
import org.jooq.tools.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;


/**
 * The <code>ROUND</code> statement.
 */
@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
final class Round<T extends Number>
extends
    AbstractField<T>
implements
    QOM.Round<T>
{

    final Field<T>       value;
    final Field<Integer> decimals;

    Round(
        Field<T> value
    ) {
        super(
            N_ROUND,
            allNotNull((DataType) dataType(INTEGER, value, false), value)
        );

        this.value = nullSafeNotNull(value, INTEGER);
        this.decimals = null;
    }

    Round(
        Field<T> value,
        Field<Integer> decimals
    ) {
        super(
            N_ROUND,
            allNotNull((DataType) dataType(INTEGER, value, false), value, decimals)
        );

        this.value = nullSafeNotNull(value, INTEGER);
        this.decimals = nullSafeNotNull(decimals, INTEGER);
    }

    // -------------------------------------------------------------------------
    // XXX: QueryPart API
    // -------------------------------------------------------------------------



    @Override
    public final void accept(Context<?> ctx) {
        switch (ctx.family()) {

            // evaluate "round" if unavailable
            case DERBY: {
                if (decimals == null) {
                    ctx.visit(DSL
                        .when(isub(value, DSL.floor(value))
                        .lessThan((T) Double.valueOf(0.5)), DSL.floor(value))
                        .otherwise(DSL.ceil(value)));

                    return;
                }
                else if (decimals instanceof Param) { Param<Integer> p = (Param<Integer>) decimals;
                    Integer decimalsValue = p.getValue();
                    Field<?> factor = DSL.val(java.math.BigDecimal.ONE.movePointRight(decimalsValue));
                    Field<T> mul = imul(value, factor);

                    ctx.visit(DSL
                        .when(isub(mul, DSL.floor(mul))
                        .lessThan((T) Double.valueOf(0.5)), idiv(DSL.floor(mul), factor))
                        .otherwise(idiv(DSL.ceil(mul), factor)));

                    return;
                }
                // fall-through
            }



























            // There's no function round(double precision, integer) in Postgres
            case POSTGRES:
            case YUGABYTE:
                if (decimals == null)
                    ctx.visit(function(N_ROUND, getDataType(), value));
                else
                    ctx.visit(function(N_ROUND, getDataType(), castIfNeeded(value, NUMERIC), decimals));

                return;

            default:
                if (decimals == null)
                    ctx.visit(function(N_ROUND, getDataType(), value));
                else
                    ctx.visit(function(N_ROUND, getDataType(), value, decimals));

                return;
        }
    }
















    // -------------------------------------------------------------------------
    // XXX: Query Object Model
    // -------------------------------------------------------------------------

    @Override
    public final Field<T> $value() {
        return value;
    }

    @Override
    public final Field<Integer> $decimals() {
        return decimals;
    }

    @Override
    public final QOM.Round<T> $value(Field<T> newValue) {
        return constructor().apply(newValue, $decimals());
    }

    @Override
    public final QOM.Round<T> $decimals(Field<Integer> newValue) {
        return constructor().apply($value(), newValue);
    }

    public final Function2<? super Field<T>, ? super Field<Integer>, ? extends QOM.Round<T>> constructor() {
        return (a1, a2) -> new Round<>(a1, a2);
    }

    @Override
    public final QueryPart $replace(
        Predicate<? super QueryPart> recurse,
        Function1<? super QueryPart, ? extends QueryPart> replacement
    ) {
        return QOM.replace(
            this,
            $value(),
            $decimals(),
            (a1, a2) -> constructor().apply(a1, a2),
            recurse,
            replacement
        );
    }

    @Override
    public final <R> R $traverse(Traverser<?, R> traverser) {
        return QOM.traverse(traverser, this,
            $value(),
            $decimals()
        );
    }

    // -------------------------------------------------------------------------
    // XXX: The Object API
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object that) {
        if (that instanceof Round) { Round o = (Round) that;
            return
                StringUtils.equals($value(), o.$value()) &&
                StringUtils.equals($decimals(), o.$decimals())
            ;
        }
        else
            return super.equals(that);
    }
}
