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

package org.lightjason.agentspeak.language.execution.lambda;

import org.apache.commons.lang3.tuple.Pair;
import org.lightjason.agentspeak.error.context.CExecutionIllegalStateException;
import org.lightjason.agentspeak.language.CCommon;
import org.lightjason.agentspeak.language.ITerm;
import org.lightjason.agentspeak.language.execution.IBaseExecution;
import org.lightjason.agentspeak.language.execution.IContext;
import org.lightjason.agentspeak.language.execution.IExecution;
import org.lightjason.agentspeak.language.fuzzy.IFuzzyValue;
import org.lightjason.agentspeak.language.variable.CMutexVariable;
import org.lightjason.agentspeak.language.variable.IVariable;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * lambda expression
 */
public final class CLambda extends IBaseExecution<IExecution[]>
{
    /**
     * serial id
     */
    private static final long serialVersionUID = -7221932027365007256L;
    /**
     * parallel execution
     */
    private final boolean m_parallel;
    /**
     * data stream
     */
    private final IExecution m_stream;
    /**
     * iterator variable
     */
    private final IVariable<Object> m_iterator;
    /**
     * return variable
     */
    private final IVariable<?> m_return;


    /**
     * ctor
     *
     * @param p_parallel parallel execution
     * @param p_stream stream
     * @param p_iterator p_iterator
     * @param p_body body definition
     * @param p_return return variable
     */
    public CLambda( final boolean p_parallel, @Nonnull final IExecution p_stream, @Nonnull final IVariable<?> p_iterator,
                    @Nonnull final Stream<IExecution> p_body, @Nonnull final IVariable<?> p_return
    )
    {
        super( p_body.toArray( IExecution[]::new ) );
        m_return = p_return;
        m_stream = p_stream;
        m_iterator = p_iterator.term();
        m_parallel = p_parallel;
    }

    @Nonnull
    @Override
    public Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context, @Nonnull final List<ITerm> p_argument,
                                           @Nonnull final List<ITerm> p_return )
    {
        final List<ITerm> l_init = CCommon.argumentlist();
        if ( !p_context.agent().fuzzy().defuzzification().success(
                p_context.agent().fuzzy().defuzzification().apply(
                    m_stream.execute( p_parallel, p_context, p_argument, l_init )
                )
             )
             || l_init.size() != 1
        )
            throw new CExecutionIllegalStateException(
                p_context,
                org.lightjason.agentspeak.common.CCommon.languagestring( this, "initializationerror" )
            );

        return m_parallel ? this.parallel( p_context, l_init.get( 0 ).raw() ) : this.sequential( p_context, l_init.get( 0 ).raw() );
    }

    /**
     * execute sequential
     *
     * @param p_context execution context
     * @param p_iterator iterator stream
     * @return execution result
     */
    private Stream<IFuzzyValue<?>> sequential( @Nonnull final IContext p_context, @Nonnull final Stream<?> p_iterator )
    {
        // build iterator variable
        final IVariable<Object> l_iterator = m_iterator.shallowcopy();

        // duplicate context, but don't use new variable instances, so use first existing variables
        final IContext l_context = p_context.duplicate(
            CCommon.streamconcatstrict(
                Stream.of( l_iterator ),
                p_context.instancevariables().values().stream(),
                Arrays.stream( m_value ).flatMap( IExecution::variables )
            )
        );

        // execute lambda body and calculate result over all loops
        return p_iterator
            .peek( l_iterator::set )
            .allMatch( i -> CCommon.executesequential( l_context, Arrays.stream( m_value ) ).getValue() )
            ? p_context.agent().fuzzy().membership().success()
            : p_context.agent().fuzzy().membership().fail();

    }

    /**
     * execute parallel
     *
     * @param p_context execution context
     * @param p_iterator iterator stream
     * @return execution result
     */
    private Stream<IFuzzyValue<?>> parallel( @Nonnull final IContext p_context, @Nonnull final Stream<?> p_iterator )
    {
        return p_iterator.parallel()
                         .map( i -> m_iterator.shallowcopy().set( i ) )
                         .map( i -> p_context.duplicate(
                             CCommon.streamconcatstrict(
                                 Stream.of( i ),
                                 p_context.instancevariables().values().stream(),
                                 Arrays.stream( m_value ).flatMap( IExecution::variables )
                             )
                         ) )
                         .map( i -> CCommon.executesequential( i, Arrays.stream( m_value ) ) )
                         .allMatch( Pair::getValue )
                         ? p_context.agent().fuzzy().membership().success()
                         : p_context.agent().fuzzy().membership().fail();
    }


    @Nonnull
    @Override
    public Stream<IVariable<?>> variables()
    {
        return Stream.concat(
            m_stream.variables(),
            m_return.equals( IVariable.EMPTY )
            ? Stream.empty()
            : Stream.of(
                m_parallel
                ? new CMutexVariable<>( m_return.fqnfunctor() )
                : m_return.shallowcopy()
            )
        );
    }

    @Override
    public String toString()
    {
        return MessageFormat.format(
            "{0}({1}) -> {2}{3} : {4}",
            m_parallel ? "@" : "",
            m_stream,
            m_iterator,
            m_return.equals( IVariable.EMPTY ) ? "" : " | " + m_return,
            MessageFormat.format(
                "{0}{2}{1}",
                m_value.length > 1 ? "{ " : "",
                m_value.length > 1 ? " }" : "",
                Arrays.stream( m_value ).map( Object::toString ).collect( Collectors.joining( "; " ) )
            )
        );
    }

}
