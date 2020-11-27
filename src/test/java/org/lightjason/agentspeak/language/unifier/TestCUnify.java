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

package org.lightjason.agentspeak.language.unifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.variable.CMutexVariable;
import org.lightjason.agentspeak.language.variable.CVariable;
import org.lightjason.agentspeak.language.variable.IRelocateVariable;
import org.lightjason.agentspeak.language.variable.IVariable;
import org.lightjason.agentspeak.testing.IBaseTest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;


/**
 * test unification
 */
public final class TestCUnify extends IBaseTest
{
    /**
     * hash unification with variables
     */
    @Test
    public void hashvariables()
    {
        final Set<IVariable<?>> l_variables = new HashSet<>();

        Assertions.assertTrue(
            new CHashUnifyAlgorithm().apply(
                l_variables,
                Stream.of( new CVariable<>( "foo", 1 ) ),
                Stream.of( new CVariable<>( "foo" ) )
            )
        );

        final IVariable<?> l_variable = l_variables.stream().findFirst().orElseThrow();
        Assertions.assertTrue( l_variable instanceof IRelocateVariable<?> );
        Assertions.assertFalse( l_variable.mutex() );
        Assertions.assertEquals( "foo", l_variable.functor() );
        Assertions.assertEquals( Integer.valueOf( 1 ), l_variable.raw() );

        l_variables.clear();

        Assertions.assertTrue(
            new CHashUnifyAlgorithm().apply(
                l_variables,
                Stream.of( new CVariable<>( "bar", 2 ) ),
                Stream.of( new CMutexVariable<>( "bar" ) )
            )
        );

        final IVariable<?> l_variablemutex = l_variables.stream().findFirst().orElseThrow();
        Assertions.assertTrue( l_variablemutex instanceof IRelocateVariable<?> );
        Assertions.assertTrue( l_variablemutex.mutex() );
        Assertions.assertEquals( "bar", l_variablemutex.functor() );
        Assertions.assertEquals( Integer.valueOf( 2 ), l_variablemutex.raw() );
    }

    /**
     * hash unification with term and variable
     */
    @Test
    public void hashterm()
    {
        final Set<IVariable<?>> l_variables = new HashSet<>();

        Assertions.assertTrue(
            new CHashUnifyAlgorithm().apply(
                l_variables,
                Stream.of( CRawTerm.of( 123 ) ),
                Stream.of( new CVariable<>( "val" ) )
            )
        );

        final IVariable<?> l_variable = l_variables.stream().findFirst().orElseThrow();
        Assertions.assertEquals( Integer.valueOf( 123 ), l_variable.raw() );
    }

    /**
     * hash unification with objects
     */
    @Test
    public void hashobject()
    {
        final Set<IVariable<?>> l_variables = new HashSet<>();

        Assertions.assertFalse(
            new CHashUnifyAlgorithm().apply(
                l_variables,
                Stream.of( CRawTerm.of( new Object() ) ),
                Stream.of( CRawTerm.of( new Object() ) )
            )
        );

        Assertions.assertTrue(
            new CHashUnifyAlgorithm().apply(
                l_variables,
                Stream.of( CRawTerm.of( "foobar" ) ),
                Stream.of( CRawTerm.of( "foobar" ) )
            )
        );
    }



    /**
     * recursive unification with variables
     */
    @Test
    public void recursivevariables()
    {
        final Set<IVariable<?>> l_variables = new HashSet<>();

        Assertions.assertTrue(
            new CRecursiveUnifyAlgorithm().apply(
                l_variables,
                Stream.of( new CVariable<>( "rfoo", 1 ) ),
                Stream.of( new CVariable<>( "rfoo" ) )
            )
        );

        final IVariable<?> l_variable = l_variables.stream().findFirst().orElseThrow();
        Assertions.assertTrue( l_variable instanceof IRelocateVariable<?> );
        Assertions.assertFalse( l_variable.mutex() );
        Assertions.assertEquals( "rfoo", l_variable.functor() );
        Assertions.assertEquals( Integer.valueOf( 1 ), l_variable.raw() );

        l_variables.clear();

        Assertions.assertTrue(
            new CRecursiveUnifyAlgorithm().apply(
                l_variables,
                Stream.of( new CVariable<>( "rbar", 2 ) ),
                Stream.of( new CMutexVariable<>( "rbar" ) )
            )
        );

        final IVariable<?> l_variablemutex = l_variables.stream().findFirst().orElseThrow();
        Assertions.assertTrue( l_variablemutex instanceof IRelocateVariable<?> );
        Assertions.assertTrue( l_variablemutex.mutex() );
        Assertions.assertEquals( "rbar", l_variablemutex.functor() );
        Assertions.assertEquals( Integer.valueOf( 2 ), l_variablemutex.raw() );
    }

    /**
     * recursive unification with term and variable
     */
    @Test
    public void recursiveterm()
    {
        final Set<IVariable<?>> l_variables = new HashSet<>();

        Assertions.assertTrue(
            new CRecursiveUnifyAlgorithm().apply(
                l_variables,
                Stream.of( CRawTerm.of( 123 ) ),
                Stream.of( new CVariable<>( "rval" ) )
            )
        );

        final IVariable<?> l_variable = l_variables.stream().findFirst().orElseThrow();
        Assertions.assertEquals( Integer.valueOf( 123 ), l_variable.raw() );
    }

    /**
     * recursive unification with objects
     */
    @Test
    public void recursiveobject()
    {
        final Set<IVariable<?>> l_variables = new HashSet<>();

        Assertions.assertFalse(
            new CRecursiveUnifyAlgorithm().apply(
                l_variables,
                Stream.of( CRawTerm.of( new Object() ) ),
                Stream.of( CRawTerm.of( new Object() ) )
            )
        );

        Assertions.assertTrue(
            new CRecursiveUnifyAlgorithm().apply(
                l_variables,
                Stream.of( CRawTerm.of( "foobar" ) ),
                Stream.of( CRawTerm.of( "foobar" ) )
            )
        );
    }

    /**
     * recursive unification with empty data
     */
    @Test
    public void recursionempty()
    {
        Assertions.assertTrue(
            new CRecursiveUnifyAlgorithm().apply(
                Collections.emptySet(),
                Stream.empty(),
                Stream.empty()
            )
        );
    }

    /**
     * recursive unification with unequal size
     */
    @Test
    public void recursionunequal()
    {
        Assertions.assertTrue(
            new CRecursiveUnifyAlgorithm().apply(
                Collections.emptySet(),
                Stream.of( CRawTerm.of( new Object() ) ),
                Stream.empty()
            )
        );
    }

    /**
     * recurive unification with literal
     */
    @Test
    public void recursionliteral()
    {
        final Set<IVariable<?>> l_variables = new HashSet<>();

        Assertions.assertTrue(
            new CRecursiveUnifyAlgorithm().apply(
                l_variables,
                Stream.of( CLiteral.parse( "literal(bar(5))" ) ),
                Stream.of( CLiteral.parse( "literal(bar(X))" ) )
            )
        );

        final IVariable<?> l_variable = l_variables.stream().findFirst().orElseThrow();
        Assertions.assertEquals( Double.valueOf( 5 ), l_variable.raw() );


        Assertions.assertFalse(
            new CRecursiveUnifyAlgorithm().apply(
                l_variables,
                Stream.of( CLiteral.parse( "literal(xbar(5))" ) ),
                Stream.of( CLiteral.parse( "literal(bar(X))" ) )
            )
        );
    }
}
