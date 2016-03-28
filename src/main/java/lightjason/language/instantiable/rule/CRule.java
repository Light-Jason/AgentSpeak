/**
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the Light-Jason                                               #
 * # Copyright (c) 2015-16, Philipp Kraus (philipp.kraus@tu-clausthal.de)               #
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

package lightjason.language.instantiable.rule;

import lightjason.agent.IAgent;
import lightjason.language.CCommon;
import lightjason.language.ILiteral;
import lightjason.language.ITerm;
import lightjason.language.IVariable;
import lightjason.language.execution.IContext;
import lightjason.language.execution.IExecution;
import lightjason.language.execution.IVariableBuilder;
import lightjason.language.execution.fuzzy.CBoolean;
import lightjason.language.execution.fuzzy.IFuzzyValue;
import lightjason.language.score.IAggregation;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * rule structure
 *
 * @bug incomplete
 */
public final class CRule implements IRule
{
    /**
     * identifier of the rule
     */
    protected final ILiteral m_id;
    /**
     * action list
     */
    protected final List<List<IExecution>> m_action;

    /**
     * ctor
     *
     * @param p_id literal with signature
     * @param p_action action list
     */
    public CRule( final ILiteral p_id, final List<List<IExecution>> p_action )
    {
        m_id = p_id;
        m_action = Collections.unmodifiableList( p_action.stream().map( i -> Collections.unmodifiableList( i ) ).collect( Collectors.toList() ) );
    }

    @Override
    public final ILiteral getIdentifier()
    {
        return m_id;
    }

    @Override
    public final IFuzzyValue<Boolean> execute( final IContext p_context, final boolean p_parallel, final List<ITerm> p_argument, final List<ITerm> p_return,
                                               final List<ITerm> p_annotation
    )
    {
        return CBoolean.from( false );
    }

    @Override
    public final double score( final IAggregation p_aggregate, final IAgent p_agent )
    {
        return 0;
    }

    @Override
    @SuppressWarnings( "serial" )
    public final Set<IVariable<?>> getVariables()
    {
        return new HashSet<IVariable<?>>()
        {{

            CCommon.recursiveterm( m_id.orderedvalues() ).filter( i -> i instanceof IVariable<?> ).forEach( i -> add( ( (IVariable<?>) i ).shallowcopy() ) );
            CCommon.recursiveliteral( m_id.annotations() ).filter( i -> i instanceof IVariable<?> ).forEach( i -> add( ( (IVariable<?>) i ).shallowcopy() ) );

            m_action.parallelStream().flatMap( i -> i.stream().flatMap( j -> j.getVariables().stream() ) ).forEach( i -> add( i ) );

        }};
    }

    @Override
    public final int hashCode()
    {
        return m_id.hashCode();
    }

    @Override
    public final boolean equals( final Object p_object )
    {
        return m_id.hashCode() == p_object.hashCode();
    }

    @Override
    public final String toString()
    {
        return MessageFormat.format(
                "{0} ({1} ==>> {2})",
                super.toString(),
                m_id,
                m_action
        );
    }

    @Override
    public final IContext getContext( final IAgent p_agent, final IAggregation p_aggregation, final IVariable<?>... p_variables )
    {
        return this.getContext( p_agent, p_aggregation, null, p_variables );
    }

    @Override
    public final IContext getContext( final IAgent p_agent, final IAggregation p_aggregation, final IVariableBuilder p_variablebuilder,
                                      final IVariable<?>... p_variables
    )
    {
        return CCommon.getContext( this, p_agent, p_aggregation, p_variablebuilder, p_variables );
    }

}