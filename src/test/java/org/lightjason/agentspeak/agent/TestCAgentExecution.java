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

package org.lightjason.agentspeak.agent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lightjason.agentspeak.action.IBaseAction;
import org.lightjason.agentspeak.common.CCommon;
import org.lightjason.agentspeak.common.CPath;
import org.lightjason.agentspeak.common.IPath;
import org.lightjason.agentspeak.configuration.IAgentConfiguration;
import org.lightjason.agentspeak.generator.CActionStaticGenerator;
import org.lightjason.agentspeak.generator.CLambdaStreamingStaticGenerator;
import org.lightjason.agentspeak.generator.IBaseAgentGenerator;
import org.lightjason.agentspeak.language.ITerm;
import org.lightjason.agentspeak.language.execution.IContext;
import org.lightjason.agentspeak.language.fuzzy.IFuzzyValue;
import org.lightjason.agentspeak.testing.IBaseTest;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.LogManager;
import java.util.stream.LongStream;
import java.util.stream.Stream;


/**
 * test for agent execute ordering
 */
public final class TestCAgentExecution extends IBaseTest
{
    /**
     * number of maximum cycles
     */
    private static final int MAXIMUMCYCLES = 1000;
    /**
     * asl source
     */
    private static final String ASL = TestCAgentExecution.class.getClassLoader().getResource( "execution.asl" ).getPath();
    /**
     * agent reference
     */
    private IAgent<?> m_agent;
    /**
     * running flag (agent can disable execute)
     */
    private AtomicBoolean m_running;
    /**
     * logs for plan execute
     */
    private final Multimap<Long, String> m_log = Multimaps.synchronizedMultimap( HashMultimap.create() );
    /**
     * log results
     *
     */
    private final Multimap<Long, String> m_result = HashMultimap.create();


    static
    {
        // disable logger
        LogManager.getLogManager().reset();
    }


    /**
     * initializing
     */
    @BeforeEach
    public void initialize()
    {
        m_running = new AtomicBoolean( true );
        try
            (
                final InputStream l_asl = new FileInputStream( ASL )
            )
        {
            m_agent = new CGenerator( l_asl ).generatesingle();
        }
        catch ( final Exception l_exception )
        {
            l_exception.printStackTrace();
            Assertions.fail( MessageFormat.format( "asl [{0}] could not be read", ASL ) );
        }


        // define execute results
        m_result.put( 0L, "main" );
        m_result.put( 1L, "single run" );
        m_result.put( 1L, "first" );
        m_result.put( 1L, "second" );
        m_result.put( 1L, "twovalues equal type" );
        m_result.put( 1L, "twovalues different type" );
        m_result.put( 1L, "twovalues with literal" );
        m_result.put( 2L, "single" );
    }

    /**
     * execute ordering test
     *
     * @throws Exception is thrown on agent execute error
     */
    @Test
    public void executionorder() throws Exception
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agent ) );
        Assumptions.assumeTrue( Objects.nonNull( m_running ) );

        int l_cycles = MAXIMUMCYCLES;
        while ( ( m_running.get() ) && ( l_cycles > 0 ) )
        {
            l_cycles--;
            m_agent.call();
        }

        Assertions.assertTrue( l_cycles > 0, "agent did not terminate" );

        // check execute results
        Assertions.assertTrue(
            LongStream.range( 0, m_result.asMap().size() ).allMatch( m_log::containsKey ),
            MessageFormat.format(  "number of cycles are incorrect, excpected [{0}] contains [{1}]", m_result.asMap().size(), m_log.asMap().size() )
        );

        Assertions.assertTrue(
            LongStream.range( 0, m_result.asMap().size() )
                      .allMatch( i -> m_result.get( i ).size() == m_log.asMap().getOrDefault( i, Collections.emptyList() ).size() ),
            MessageFormat.format( "number of log elements during execute are incorrect, expected {0} result {1}", m_result.asMap(), m_log.asMap() )
        );

        LongStream.range( 0, m_result.asMap().size() ).forEach( i -> Assertions.assertTrue(
            m_log.get( i ).containsAll( m_result.get( i ) ),
            MessageFormat.format( "expected result {0} for index {2} is not equal to log {1}", m_result.get( i ), m_log.get( i ), i )
        ) );

    }


    /**
     * agent generator
     */
    private final class CGenerator extends IBaseAgentGenerator<CAgent>
    {
        /**
         * ctor
         *
         * @param p_stream asl stream
         * @throws Exception on any error
         */
        CGenerator( final InputStream p_stream ) throws Exception
        {
            super(
                p_stream,

                new CActionStaticGenerator(
                    Stream.concat(
                        CCommon.actionsFromPackage(),
                        Stream.of(
                            new CStop(),
                            new CLog()
                        )
                    )
                ),
                new CLambdaStreamingStaticGenerator( CCommon.lambdastreamingFromPackage() )
            );
        }

        @Nonnull
        @Override
        public CAgent generatesingle( final Object... p_data )
        {
            return new CAgent( m_configuration );
        }
    }


    /**
     * agent class
     */
    private static class CAgent extends IBaseAgent<CAgent>
    {
        /**
         * serial id
         */
        private static final long serialVersionUID = -7467073439000881088L;
        /**
         * cycle counter
         */
        private final AtomicLong m_cycle = new AtomicLong();

        /**
         * ctor
         *
         * @param p_configuration agent configuration
         */
        CAgent( final IAgentConfiguration<CAgent> p_configuration )
        {
            super( p_configuration );
        }

        @Override
        public CAgent call() throws Exception
        {
            super.call();
            m_cycle.incrementAndGet();
            return this;
        }

        /**
         * returns the cycle
         *
         * @return cycle
         */
        final long cycle()
        {
            return m_cycle.get();
        }
    }


    /**
     * stop action
     */
    private final class CStop extends IBaseAction
    {
        /**
         * serial id
         */
        private static final long serialVersionUID = 5466369414656444520L;

        @Nonnull
        @Override
        public IPath name()
        {
            return CPath.of( "stop" );
        }

        @Nonnegative
        @Override
        public int minimalArgumentNumber()
        {
            return 0;
        }

        @Nonnull
        @Override
        public Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context,
                                               @Nonnull final List<ITerm> p_argument, @Nonnull final List<ITerm> p_return )
        {
            m_running.set( false );
            return Stream.empty();
        }
    }

    /**
     * log action
     */
    private final class CLog extends IBaseAction
    {
        /**
         * serial id
         */
        private static final long serialVersionUID = 4536335097194230205L;

        @Nonnull
        @Override
        public IPath name()
        {
            return CPath.of( "test/log" );
        }

        @Nonnegative
        @Override
        public int minimalArgumentNumber()
        {
            return 1;
        }

        @Nonnull
        @Override
        public Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context,
                                               @Nonnull final List<ITerm> p_argument, @Nonnull final List<ITerm> p_return )
        {
            m_log.put( p_context.agent().<CAgent>raw().cycle(), p_argument.get( 0 ).raw()  );
            return Stream.empty();
        }
    }
}
