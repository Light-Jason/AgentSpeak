/*
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the LightJason                                                #
 * # Copyright (c) 2015-19, LightJason (info@lightjason.org)                            #
 * # This program is free software: you can redistribute it and/or modify               #
 * # it under the terms of the GNU Lesser General Public License as                     #
 * # published by the Free Software Foundation, either version 3 of the                 #
 * # License, or (at your option) any later version.                                    #
 * #                                                                                    #
 * # This program is distributed in the hope that it will be useful,                    #
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of                     #
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                      #
 * # GNU Lesser General Public License for more details.                                #
 * #                                                                                    #
 * # You should have received a copy of the GNU Lesser General Public License           #
 * # along with this program. If not, see http://www.gnu.org/licenses/                  #
 * ######################################################################################
 * @endcond
 */

package org.lightjason.agentspeak.language.execution.expression;

import org.lightjason.agentspeak.error.context.CExecutionIllegalStateException;
import org.lightjason.agentspeak.language.CCommon;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.ITerm;
import org.lightjason.agentspeak.language.execution.IContext;
import org.lightjason.agentspeak.language.execution.IExecution;
import org.lightjason.agentspeak.language.fuzzy.IFuzzyValue;
import org.lightjason.agentspeak.language.variable.IVariable;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


/**
 * unary expression
 */
public final class CUnaryExpression implements IUnaryExpression
{
    /**
     * serial id
     */
    private static final long serialVersionUID = -2385154799234194316L;
    /**
     * operator
     */
    private final EUnaryOperator m_operator;
    /**
     * execution element
     */
    private final IExecution m_element;

    /**
     * ctor
     *
     * @param p_operator unary operator
     * @param p_element element
     */
    public CUnaryExpression( @Nonnull final EUnaryOperator p_operator, @Nonnull final IExecution p_element )
    {
        m_operator = p_operator;
        m_element = p_element;
    }

    @Nonnull
    @Override
    public Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context, @Nonnull final List<ITerm> p_argument,
                                           @Nonnull final List<ITerm> p_return
    )
    {
        final List<ITerm> l_return = CCommon.argumentlist();
        final IFuzzyValue<?>[] l_result =  m_element.execute( p_parallel, p_context, p_argument, l_return ).toArray( IFuzzyValue[]::new );
        if ( l_return.size() == 0 && l_result.length == 0 )
            throw new CExecutionIllegalStateException( p_context, org.lightjason.agentspeak.common.CCommon.languagestring( this, "incorrectreturnargument" ) );

        // if no return argument is set, defuzzyfication of result
        if ( l_return.size() == 0 )
            l_return.add(
                CRawTerm.of(
                    p_context.agent().fuzzy().defuzzification().success(
                        p_context.agent().fuzzy().defuzzification().apply(
                            Arrays.stream( l_result )
                        )
                    )
                )
            );

        p_return.add(
            CRawTerm.of(
                m_operator.apply( l_return.get( 0 ) )
            )
        );

        return Arrays.stream( l_result );
    }

    @Nonnull
    @Override
    public Stream<IVariable<?>> variables()
    {
        return m_element.variables();
    }

    @Override
    public String toString()
    {
        return MessageFormat.format( "{0}({1})", m_operator, m_element );
    }
}
