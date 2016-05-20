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

package lightjason.agentspeak.language.execution.action.achievement_test;

import lightjason.agentspeak.agent.IAgent;
import lightjason.agentspeak.language.ITerm;
import lightjason.agentspeak.language.execution.IContext;
import lightjason.agentspeak.language.execution.fuzzy.IFuzzyValue;
import lightjason.agentspeak.language.variable.IVariable;
import lightjason.agentspeak.language.variable.IVariableEvaluate;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Stream;


/**
 * achievement for rule-variable execution
 */
public final class CAchievementRuleVariable extends IAchievementRule<IVariableEvaluate>
{
    /**
     * ctor
     *
     * @param p_type value of the rule
     */
    public CAchievementRuleVariable( final IVariableEvaluate p_type )
    {
        super( p_type );
    }

    @Override
    public IFuzzyValue<Boolean> execute( final IContext p_context, final boolean p_parallel, final List<ITerm> p_argument, final List<ITerm> p_return,
                                         final List<ITerm> p_annotation
    )
    {
        return CAchievementRuleVariable.execute( p_context, m_value.evaluate( p_context ), m_value.hasMutex() );
    }

    @Override
    public final String toString()
    {
        return MessageFormat.format( "${0}", m_value );
    }

    @Override
    public final double score( final IAgent p_agent )
    {
        return 0;
    }

    @Override
    public Stream<IVariable<?>> getVariables()
    {
        return m_value.getVariables();
    }
}