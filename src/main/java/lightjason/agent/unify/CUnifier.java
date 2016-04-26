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

package lightjason.agent.unify;

import lightjason.agent.IAgent;
import lightjason.language.CCommon;
import lightjason.language.ILiteral;
import lightjason.language.ITerm;
import lightjason.language.execution.IContext;
import lightjason.language.execution.action.unify.IUnifier;
import lightjason.language.execution.expression.IExpression;
import lightjason.language.execution.fuzzy.CFuzzyValue;
import lightjason.language.execution.fuzzy.IFuzzyValue;
import lightjason.language.variable.IVariable;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * unification algorithm
 */
public final class CUnifier implements IUnifier
{
    /**
     * hash-based unify algorithm
     */
    private final IAlgorithm m_hashbased;
    /**
     * recursive unify algorithm
     */
    private final IAlgorithm m_recursive;

    /**
     * ctor
     */
    public CUnifier()
    {
        this( new CHash(), new CRecursive() );
    }

    /**
     * ctor
     *
     * @param p_hashbased hash-based unification algorithm
     * @param p_recursive recursive-based unification algorithm
     */
    public CUnifier( final IAlgorithm p_hashbased, final IAlgorithm p_recursive )
    {
        m_hashbased = p_hashbased;
        m_recursive = p_recursive;
    }


    // --- inheritance & context modification ------------------------------------------------------------------------------------------------------------------

    @Override
    public final IFuzzyValue<Boolean> unify( final IContext p_context, final ILiteral p_literal, final long p_variablenumber )
    {
        // get all possible variables
        final List<Set<IVariable<?>>> l_variables = this.unify( p_context.getAgent(), p_literal, p_variablenumber );
        if ( l_variables.isEmpty() )
            return CFuzzyValue.from( false );

        CCommon.updatecontext( p_context, l_variables.get( 0 ).parallelStream() );
        return CFuzzyValue.from( true );
    }

    @Override
    public final IFuzzyValue<Boolean> parallel( final IContext p_context, final ILiteral p_literal, final long p_variablenumber,
                                                final IExpression p_expression
    )
    {
        // get all possible variables
        final List<Set<IVariable<?>>> l_variables = this.unify( p_context.getAgent(), p_literal, p_variablenumber );
        if ( l_variables.isEmpty() )
            return CFuzzyValue.from( false );

        // otherwise the expression must be checked, first match will be used
        final Set<IVariable<?>> l_result = l_variables.parallelStream()
                                                      .filter( i -> {
                                                          final List<ITerm> l_return = new LinkedList<>();
                                                          p_expression.execute(
                                                                  CCommon.updatecontext(
                                                                          p_context.duplicate(),
                                                                          i.parallelStream()
                                                                  ),
                                                                  false,
                                                                  Collections.<ITerm>emptyList(),
                                                                  l_return,
                                                                  Collections.<ITerm>emptyList()
                                                          );
                                                          return ( l_return.size() == 1 ) && ( CCommon.<Boolean, ITerm>getRawValue( l_return.get( 0 ) ) );
                                                      } )
                                                      .findFirst()
                                                      .orElse( Collections.<IVariable<?>>emptySet() );

        // if no match
        if ( l_result.isEmpty() )
            return CFuzzyValue.from( false );

        CCommon.updatecontext( p_context, l_result.parallelStream() );
        return CFuzzyValue.from( true );
    }

    @Override
    public final IFuzzyValue<Boolean> sequential( final IContext p_context, final ILiteral p_literal, final long p_variablenumber,
                                                  final IExpression p_expression
    )
    {
        // get all possible variables
        final List<Set<IVariable<?>>> l_variables = this.unify( p_context.getAgent(), p_literal, p_variablenumber );
        if ( l_variables.isEmpty() )
            return CFuzzyValue.from( false );

        // otherwise the expression must be checked, first match will be used
        final Set<IVariable<?>> l_result = l_variables.stream()
                                                      .filter( i -> {
                                                          final List<ITerm> l_return = new LinkedList<>();
                                                          p_expression.execute(
                                                                  CCommon.updatecontext(
                                                                          p_context.duplicate(),
                                                                          i.parallelStream()
                                                                  ),
                                                                  false,
                                                                  Collections.<ITerm>emptyList(),
                                                                  l_return,
                                                                  Collections.<ITerm>emptyList()
                                                          );
                                                          return ( l_return.size() == 1 ) && ( CCommon.<Boolean, ITerm>getRawValue( l_return.get( 0 ) ) );
                                                      } )
                                                      .findFirst()
                                                      .orElse( Collections.<IVariable<?>>emptySet() );

        // if no match
        if ( l_result.isEmpty() )
            return CFuzzyValue.from( false );

        CCommon.updatecontext( p_context, l_result.parallelStream() );
        return CFuzzyValue.from( true );
    }

    @Override
    public final Set<IVariable<?>> literal( final ILiteral p_literal, final ILiteral p_value )
    {
        final Set<IVariable<?>> l_result = new HashSet<>();
        final ILiteral l_literal = (ILiteral) p_literal.deepcopy();

        // try to unify exact or if not possible by recursive on the value set
        boolean l_succeed = l_literal.valuehash() == p_value.valuehash()
                            ? m_hashbased.unify(
                l_result, CCommon.recursiveterm( p_value.orderedvalues() ), CCommon.recursiveterm( l_literal.orderedvalues() ) )
                            : m_recursive.unify( l_result, p_value.orderedvalues(), l_literal.orderedvalues() );
        if ( !l_succeed )
            return Collections.<IVariable<?>>emptySet();

        // try to unify exact or if not possible by recursive on the annotation set
        l_succeed = l_literal.annotationhash() == p_value.annotationhash()
                    ? m_hashbased.unify(
                l_result, CCommon.recursiveliteral( p_value.annotations() ), CCommon.recursiveliteral( l_literal.annotations() ) )
                    : m_recursive.unify( l_result, p_value.annotations(), l_literal.annotations() );

        if ( !l_succeed )
            return Collections.<IVariable<?>>emptySet();

        return l_result;
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------------------------


    // --- unifying algorithm of a literal ---------------------------------------------------------------------------------------------------------------------

    @Override
    public final int hashCode()
    {
        return m_hashbased.hashCode() + m_recursive.hashCode();
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public final boolean equals( final Object p_object )
    {
        return this.hashCode() == p_object.hashCode();
    }

    @Override
    public final String toString()
    {
        return MessageFormat.format( "hash-based unification: {0} / recursive unification: {1}", m_hashbased, m_recursive );
    }

    /**
     * search all relevant literals within the agent beliefbase and unifies the variables
     *
     * @param p_agent agent
     * @param p_literal literal search
     * @param p_variablenumber number of unified variables
     * @return list of literal sets
     **/
    private List<Set<IVariable<?>>> unify( final IAgent p_agent, final ILiteral p_literal, final long p_variablenumber )
    {
        return p_agent.getBeliefBase()
                      .parallelStream( p_literal.isNegated(), p_literal.getFQNFunctor() )
                      .filter( i -> ( i.emptyValues() == p_literal.emptyValues() ) && ( i.emptyAnnotations() == p_literal.emptyAnnotations() ) )
                      .map( i -> this.literal( p_literal, i ) )
                      .filter( i -> p_variablenumber == i.size() )
                      .collect( Collectors.toList() );
    }
}
