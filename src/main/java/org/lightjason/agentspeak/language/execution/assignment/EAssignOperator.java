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

package org.lightjason.agentspeak.language.execution.assignment;

import org.lightjason.agentspeak.common.CCommon;
import org.lightjason.agentspeak.error.CNoSuchElementException;
import org.lightjason.agentspeak.language.ITerm;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.BiFunction;


/**
 * binary operator
 */
public enum EAssignOperator implements BiFunction<ITerm, ITerm, Object>
{
    INCREMENT( "+=" )
    {
        @Override
        public Object apply( @Nonnull final ITerm p_lhs, @Nonnull final ITerm p_rhs )
        {
            return p_lhs.<Number>raw().doubleValue() + p_rhs.<Number>raw().doubleValue();
        }
    },
    DECREMENT( "-=" )
    {
        @Override
        public Object apply( @Nonnull final ITerm p_lhs, @Nonnull final ITerm p_rhs )
        {
            return p_lhs.<Number>raw().doubleValue() - p_rhs.<Number>raw().doubleValue();
        }
    },

    MULTIPLY( "*=" )
    {
        @Override
        public Object apply( @Nonnull final ITerm p_lhs, @Nonnull final ITerm p_rhs )
        {
            return p_lhs.<Number>raw().doubleValue() * p_rhs.<Number>raw().doubleValue();
        }
    },
    DIVIDE( "/=" )
    {
        @Override
        public Object apply( @Nonnull final ITerm p_lhs, @Nonnull final ITerm p_rhs )
        {
            return p_lhs.<Number>raw().doubleValue() / p_rhs.<Number>raw().doubleValue();
        }
    },
    MODULO( "%=" )
    {
        @Override
        public Object apply( @Nonnull final ITerm p_lhs, @Nonnull final ITerm p_rhs )
        {
            return org.lightjason.agentspeak.language.CCommon.modulo( p_lhs.raw(), p_rhs.raw() );
        }
    },

    POWER( "^=" )
    {
        @Override
        public Object apply( @Nonnull final ITerm p_lhs, @Nonnull final ITerm p_rhs )
        {
            return Math.pow( p_lhs.<Number>raw().doubleValue(), p_rhs.<Number>raw().doubleValue() );
        }
    },

    ASSIGN( "=" )
    {
        @Override
        public Object apply( @Nonnull final ITerm p_lhs, @Nonnull final ITerm p_rhs )
        {
            return p_rhs.raw();
        }
    };

    /**
     * operator
     */
    private final String m_operator;

    /**
     * ctor
     *
     * @param p_operator operator string
     */
    EAssignOperator( final String p_operator )
    {
        m_operator = p_operator;
    }

    @Override
    public String toString()
    {
        return m_operator;
    }

    /**
     * returns operator of a string
     *
     * @param p_value string
     * @return operator
     */
    @Nonnull
    public static EAssignOperator of( @Nonnull final String p_value )
    {
        return Arrays.stream( EAssignOperator.values() )
                     .filter( i -> i.m_operator.equals( p_value ) )
                     .findFirst()
                     .orElseThrow( () -> new CNoSuchElementException( CCommon.languagestring( EAssignOperator.class, "unknown", p_value ) ) );
    }
}
