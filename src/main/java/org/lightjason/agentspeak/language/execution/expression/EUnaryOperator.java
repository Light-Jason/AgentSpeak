/*
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the LightJason AgentSpeak(L++)                                #
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

import org.lightjason.agentspeak.common.CCommon;
import org.lightjason.agentspeak.error.CIllegalArgumentException;
import org.lightjason.agentspeak.language.ITerm;

import javax.annotation.Nonnull;
import java.util.function.Function;


/**
 * expression unary operator
 */
public enum EUnaryOperator implements Function<ITerm, Object>
{
    NEGATION( "~" );

    /**
     * text name of the enum
     */
    private final String m_name;

    /**
     * ctor
     *
     * @param p_name text name
     */
    EUnaryOperator( final String p_name )
    {
        m_name = p_name;
    }

    @Override
    public final String toString()
    {
        return m_name;
    }

    @Override
    public final Object apply( @Nonnull final ITerm p_term )
    {
        switch ( this )
        {
            case NEGATION:
                return !p_term.<Boolean>raw();

            default:
                throw new CIllegalArgumentException( CCommon.languagestring( this, "unknownoperator", this ) );
        }
    }
}
