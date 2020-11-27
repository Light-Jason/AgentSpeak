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

package org.lightjason.agentspeak.action;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lightjason.agentspeak.common.IPath;
import org.lightjason.agentspeak.language.ITerm;
import org.lightjason.agentspeak.language.execution.IContext;
import org.lightjason.agentspeak.language.execution.lambda.ILambdaStreaming;
import org.lightjason.agentspeak.language.fuzzy.IFuzzyValue;
import org.lightjason.agentspeak.testing.IBaseTest;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;


/**
 * test action structure and lambda-streaming
 */
public final class TestCActionLambda extends IBaseTest
{
    /**
     * test action name-by-class
     */
    @Test
    public void namebyclass()
    {
        Assertions.assertEquals( "test/testaction", new CTestAction().name().toString() );
    }

    /**
     * test lambda-number-streaming
     */
    @Test
    public void lambdanumber()
    {
        final ILambdaStreaming<Number> l_stream = new CTestLambdaNumber();
        final Object[] l_result = Stream.of( 0L, 1L, 2L, 3L ).toArray();

        Assertions.assertArrayEquals( l_result, l_stream.apply( 4 ).toArray() );
        Assertions.assertArrayEquals( l_result, l_stream.apply( 4.3 ).toArray() );
        Assertions.assertArrayEquals( l_result, l_stream.apply( 4.7F ).toArray() );
        Assertions.assertArrayEquals( l_result, l_stream.apply( 4L ).toArray() );
    }

    /**
     * test lambda equality
     */
    @Test
    public void lambdaequal()
    {
        Assertions.assertEquals( ILambdaStreaming.EMPTY, ILambdaStreaming.EMPTY );
        Assertions.assertEquals( new CTestLambdaNumber(), new CTestLambdaNumber() );
        Assertions.assertNotEquals( ILambdaStreaming.EMPTY, new CTestLambdaNumber() );
    }

    /**
     * test action
     */
    private static final class CTestAction extends IBaseAction
    {
        /**
         * serial id
         */
        private static final long serialVersionUID = 8011510086352157445L;

        @Nonnull
        @Override
        public IPath name()
        {
            return namebyclass( CTestAction.class, "test" );
        }

        @Nonnull
        @Override
        public Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context,
                                               @Nonnull final List<ITerm> p_argument, @Nonnull final List<ITerm> p_return )
        {
            return Stream.empty();
        }
    }

    /**
     * test number stream lambda
     */
    private static final class CTestLambdaNumber extends IBaseLambdaStreaming<Number>
    {
        /**
         * serial id
         */
        private static final long serialVersionUID = 1114943250673664117L;

        @NonNull
        @Override
        public Stream<Class<?>> assignable()
        {
            return Stream.of( Number.class );
        }

        @Override
        public Stream<?> apply( final Number p_number )
        {
            return LongStream.range( 0, Math.abs( p_number.longValue() ) ).boxed();
        }
    }
}
