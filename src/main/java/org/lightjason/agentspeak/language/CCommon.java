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

package org.lightjason.agentspeak.language;

import com.codepoetics.protonpack.StreamUtils;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.rits.cloning.Cloner;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.common.IExceptionFunction;
import org.lightjason.agentspeak.error.CNoSuchElementException;
import org.lightjason.agentspeak.language.execution.CContext;
import org.lightjason.agentspeak.language.execution.IContext;
import org.lightjason.agentspeak.language.execution.IExecution;
import org.lightjason.agentspeak.language.execution.instantiable.IInstantiable;
import org.lightjason.agentspeak.language.execution.instantiable.plan.statistic.IPlanStatistic;
import org.lightjason.agentspeak.language.execution.instantiable.plan.trigger.ITrigger;
import org.lightjason.agentspeak.language.fuzzy.IFuzzyValue;
import org.lightjason.agentspeak.language.unifier.IUnifier;
import org.lightjason.agentspeak.language.variable.IVariable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * common structure for execution definition
 */
public final class CCommon
{
    /**
     * floating precision
     */
    public static final Number FLOATINGPRECISION = 1e-8;
    /**
     * numeric constant values - infinity is defined manually
     */
    public static final Map<String, Double> NUMERICCONSTANT = Collections.unmodifiableMap(
        StreamUtils.zip(
            Stream.of(
                "pi",
                "euler",
                "lightspeed",
                "avogadro",
                "boltzmann",
                "gravity",
                "electron",
                "neutron",
                "proton",
                "positiveinfinity",
                "negativeinfinity",
                "maximumvalue",
                "minimumvalue",
                "nan"
            ),

            Stream.of(
                Math.PI,
                Math.E,
                299792458.0,
                6.0221412927e23,
                8.617330350e-15,
                6.67408e-11,
                9.10938356e-31,
                1674927471214e-27,
                1.6726219e-27,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE,
                Double.MIN_VALUE,
                Double.NaN
            ),

            AbstractMap.SimpleImmutableEntry::new
        ).collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );

    /**
     * cloner
     */
    private static final Cloner CLONER = new Cloner();

    /**
     * private ctor - avoid instantiation
     */
    private CCommon()
    {
    }

    /**
     * modulo operator.
     * used n assignment and binary operator
     *
     * @param p_lhs left-hand-side
     * @param p_rhs right-hand-side
     * @return result
     */
    public static Number modulo( @Nonnull final Number p_lhs, @Nonnull final Number p_rhs )
    {
        return p_lhs.longValue() < 0
               ? Math.abs( ( p_rhs.longValue() + p_lhs.longValue() ) % p_rhs.longValue() )
               : p_lhs.longValue() % p_rhs.longValue();
    }

    /**
     * returns argument list for any operations
     *
     * @return list
     */
    public static List<ITerm> argumentlist()
    {
        return Collections.synchronizedList( new ArrayList<>() );
    }

    /**
     * equality of floating-point numbers
     *
     * @param p_lhs left-hand argument
     * @param p_rhs right-hand argument
     * @param p_precision precision
     * @return equality flag
     */
    public static boolean floatingequal( @NonNull final Number p_lhs, @NonNull final Number p_rhs, @NonNull final Number p_precision )
    {
        return Math.abs( p_lhs.doubleValue() - p_rhs.doubleValue() ) < p_precision.doubleValue();
    }

    //--- plan / rule instantiation ----------------------------------------------------------------------------------------------------------------------------

    /**
     * updates within an instance context all variables of the stream
     *
     * @param p_context context
     * @param p_unifiedvariables unified variables as stream
     * @return context reference
     */
    @Nonnull
    public static IContext updatecontext( @Nonnull final IContext p_context, @Nonnull final Stream<IVariable<?>> p_unifiedvariables )
    {
        p_unifiedvariables.parallel()
                          .map( i -> new ImmutablePair<>( i, p_context.instancevariables().get( i.fqnfunctor() ) ) )
                          .filter( i -> Objects.nonNull( i.right ) )
                          .forEach( i -> i.right.set( i.left.raw() ) );
        return p_context;
    }

    /**
     * creates the instantiate execution context with default variables
     *
     * @param p_instance instance object
     * @param p_agent agent
     * @param p_variable variable stream
     * @return context object
     */
    @Nonnull
    public static IContext instantiate( @Nonnull final IInstantiable p_instance, @Nonnull final IAgent<?> p_agent,
                                        @Nonnull final Stream<IVariable<?>> p_variable
    )
    {
        return new CContext(
            p_agent,
            p_instance,
            Collections.unmodifiableSet(
                CCommon.streamconcat(
                    p_variable,
                    p_agent.variablebuilder().apply( p_agent, p_instance ),
                    p_instance.variables().map( i -> i.shallowcopy() )
                ).collect( Collectors.toSet() )
            )
        );
    }


    /**
     * unifies trigger and creates the set of variables
     *
     * @param p_unifier unifier
     * @param p_source input trigger (with values)
     * @param p_target trigger (of a plan / rule)
     * @return pair of valid unification and unified variables
     *
     * @note target trigger literal must be cloned to avoid variable overwriting
     */
    @Nonnull
    public static Pair<Boolean, Set<IVariable<?>>> unifytrigger( @Nonnull final IUnifier p_unifier,
                                                                 @Nonnull final ITrigger p_source, @Nonnull final ITrigger p_target
    )
    {
        // filter for avoid duplicated instantiation on non-existing values
        if ( !( p_source.literal().emptyValues() == p_target.literal().emptyValues() ) )
            return new ImmutablePair<>( false, Collections.emptySet() );

        // unifier variables, source trigger literal must be copied
        final Set<IVariable<?>> l_variables = p_unifier.unify( p_source.literal(), p_target.literal().deepcopy().raw() );

        // check for completely unification (of all variables)
        return l_variables.size() == CCommon.variablefrequency( p_target.literal() ).size()
               ? new ImmutablePair<>( true, l_variables )
               : new ImmutablePair<>( false, Collections.emptySet() );
    }

    /**
     * instantiate a plan with context and plan-specific variables
     *
     * @param p_planstatistic plan statistic for instatiation
     * @param p_agent agent
     * @param p_variables instantiated variables
     * @return pair of planstatistic and context
     */
    @Nonnull
    public static Pair<IPlanStatistic, IContext> instantiateplan( @Nonnull final IPlanStatistic p_planstatistic,
                                                                  @Nonnull final IAgent<?> p_agent, @Nonnull final Set<IVariable<?>> p_variables
    )
    {
        return new ImmutablePair<>(
            p_planstatistic,
            p_planstatistic.plan().instantiate(
                p_agent,
                Stream.concat( p_variables.stream(), p_planstatistic.variables() )
            )
        );
    }

    // --- execution structure ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * sequential execute
     *
     * @param p_parallel parallel execution flag
     * @param p_context execution context
     * @param p_argument arguments
     * @param p_return return values
     * @param p_execution execution stream
     * @return list with execution results and successful execution
     *
     * @note stream is stopped iif an execution is failed
     */
    @Nonnull
    public static Pair<List<IFuzzyValue<?>>, Boolean> executesequential( final boolean p_parallel, @Nonnull final IContext p_context,
                                                                         @Nonnull final List<ITerm> p_argument, @Nonnull final List<ITerm> p_return,
                                                                         @Nonnull final Stream<IExecution> p_execution )
    {
        final List<IFuzzyValue<?>> l_result = new ArrayList<>();
        final AtomicBoolean l_success = new AtomicBoolean();

        return p_execution
            .peek( i -> i.execute( p_parallel, p_context, p_argument, p_return ).forEach( l_result::add ) )
            .filter( i ->
            {
                l_success.set( p_context.agent().fuzzy().defuzzification().success( p_context.agent().fuzzy().defuzzification().apply( l_result.stream() ) ) );
                return !l_success.get();
            } )
            .findFirst()
            .map( i -> new ImmutablePair<>( l_result, l_success.get() ) )
            .orElse( new ImmutablePair<>( l_result, l_success.get() ) );
    }

    /**
     * sequential execute
     *
     * @param p_context execution context
     * @param p_execution execution stream
     * @return list with execution results and successful execution
     *
     * @note stream is stopped iif an execution is failed
     */
    @Nonnull
    public static Pair<List<IFuzzyValue<?>>, Boolean> executesequential( @Nonnull final IContext p_context, @Nonnull final Stream<IExecution> p_execution )
    {
        return executesequential( false, p_context, Collections.emptyList(), Collections.emptyList(), p_execution );
    }

    /**
     * parallel execute
     *
     * @param p_context execution context
     * @param p_execution execution stream
     * @return list with execution results and successful execution
     *
     * @note each element is executed
     */
    @Nonnull
    public static Pair<List<IFuzzyValue<?>>, Boolean> executeparallel( @Nonnull final IContext p_context, @Nonnull final Stream<IExecution> p_execution
    )
    {
        final List<IFuzzyValue<?>> l_result = p_execution.parallel()
                                                         .flatMap( i -> i.execute(
                                                             false, p_context, Collections.emptyList(), Collections.emptyList() )
                                                         )
                                                         .collect( Collectors.toList() );

        return new ImmutablePair<>(
            l_result,
            p_context.agent().fuzzy().defuzzification().success(
                p_context.agent().fuzzy().defuzzification().apply( l_result.stream() )
            )
        );
    }

    // --- variable / term helpers -----------------------------------------------------------------------------------------------------------------------------

    /**
     * returns the class hierarchie of a given class
     *
     * @param p_class class
     * @return stream start with the input class and ascending super class elements
     */
    @NonNull
    public static Stream<Class<?>> classhierarchie( @NonNull final Class<?> p_class )
    {
        if ( Object.class.equals( p_class ) )
            return Stream.empty();

        return CCommon.streamconcatstrict(
            Stream.of( p_class ),

            Objects.isNull( p_class.getSuperclass() )
            ? Stream.empty()
            : classhierarchie( p_class.getSuperclass() ),

            Objects.isNull( p_class.getInterfaces() )
            ? Stream.empty()
            : Arrays.stream( p_class.getInterfaces() ).parallel().flatMap( CCommon::classhierarchie )
        );
    }

    /**
     * stream of all class fields
     *
     * @param p_class class or null
     * @return field stream
     */
    @Nonnull
    public static Stream<Field> classfields( @Nullable final Class<?> p_class )
    {
        if ( Objects.isNull( p_class ) )
            return Stream.empty();

        return Stream.concat(
            Arrays.stream( p_class.getDeclaredFields() ),
            classfields( p_class.getSuperclass() )
        );
    }

    /**
     * concat multiple streams
     *
     * @param p_streams streams
     * @return concated stream
     *
     * @tparam T any value type
     */
    @Nonnull
    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> Stream<? extends T> streamconcat( @Nonnull final Stream<? extends T>... p_streams )
    {
        return Arrays.stream( p_streams ).reduce( Stream::concat ).orElseGet( Stream::empty );
    }

    /**
     * concat multiple streams
     *
     * @param p_streams streams
     * @return concated stream
     *
     * @tparam T any value type
     */
    @Nonnull
    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> Stream<T> streamconcatstrict( @Nonnull final Stream<T>... p_streams )
    {
        return Arrays.stream( p_streams ).reduce( Stream::concat ).orElseGet( Stream::empty );
    }

    /**
     * execute stream in parallel
     *
     * @param p_stream stream
     * @param p_parallel parallel
     * @return modified stream
     *
     * @tparam T stream elements
     */
    public static <T> Stream<T> parallelstream( final Stream<T> p_stream, final boolean p_parallel )
    {
        return p_parallel ? p_stream.parallel() : p_stream;
    }

    /**
     * consts the variables within a literal
     *
     * @param p_literal literal
     * @return map with frequency
     */
    @Nonnull
    public static Map<IVariable<?>, Integer> variablefrequency( @Nonnull final ILiteral p_literal )
    {
        return Collections.unmodifiableMap(
            flattenrecursive( p_literal.orderedvalues() )
                .filter( i -> i instanceof IVariable<?> )
                .map( i -> (IVariable<?>) i )
                .collect( Collectors.toMap( i -> i, i -> 1, Integer::sum ) )
        );
    }

    /**
     * checks a term value for assignable class
     *
     * @param p_value any value type
     * @param p_class assignable class
     * @return boolean for assinable
     */
    @SuppressWarnings( "unchecked" )
    public static <T> boolean isssignableto( @Nullable final T p_value, @Nonnull final Class<?> p_class )
    {
        if ( Objects.isNull( p_value ) )
            return true;
        if ( p_value instanceof IAssignable<?> )
            return ( (IAssignable<?>) p_value ).valueassignableto( p_class );

        return p_class.isAssignableFrom( p_value.getClass() );
    }

    /**
     * bind term value on the current context
     *
     * @param p_term term / variable
     * @param p_context current context
     * @return bind term element
     */
    @Nonnull
    public static ITerm bindbycontext( @Nonnull final ITerm p_term, @Nonnull final IContext p_context )
    {
        if ( p_term instanceof IVariable<?> )
        {
            final IVariable<?> l_variable = p_context.instancevariables().get( p_term.fqnfunctor() );
            return Objects.isNull( l_variable ) ? IRawTerm.EMPTY : l_variable;
        }

        return p_term;
    }

    /**
     * replace variables with context variables
     *
     * @param p_context execution context
     * @param p_terms replacing term list
     * @return result term list
     */
    @Nonnull
    public static Stream<ITerm> replacebycontext( @Nonnull final IContext p_context, @Nonnull final Stream<? extends ITerm> p_terms )
    {
        return p_terms.map( i -> replacebycontext( p_context, i ) );
    }

    /**
     * replace variable with context variable
     * other values will be passed without context access
     *
     * @param p_context execution context
     * @param p_term term
     * @return replaces variable object
     */
    @Nonnull
    public static ITerm replacebycontext( @Nonnull final IContext p_context, @Nonnull final ITerm p_term )
    {
        if ( p_term instanceof IVariable<?> )
        {
            final ITerm l_variable = p_context.instancevariables().get( p_term.fqnfunctor() );
            if ( Objects.isNull( l_variable ) )
                throw new CNoSuchElementException(
                    org.lightjason.agentspeak.common.CCommon.languagestring( CCommon.class, "variablenotfoundincontext", p_term.fqnfunctor() )
                );

            return l_variable;
        }

        return p_term;
    }


    /**
     * flat term-in-term collection into
     * a straight term list
     *
     * @param p_terms term collection
     * @return flat term stream
     */
    @Nonnull
    public static Stream<ITerm> flatten( @Nonnull final Collection<? extends ITerm> p_terms )
    {
        return flattenstream( p_terms.stream() );
    }

    /**
     * flat term-in-term stream into
     * a straight term list
     *
     * @param p_terms term stream
     * @return flat term stream
     */
    @Nonnull
    public static Stream<ITerm> flatten( @Nonnull final Stream<? extends ITerm> p_terms )
    {
        return flattenstream( p_terms );
    }

    /**
     * recursive stream of term values
     *
     * @param p_input term stream
     * @return term stream
     */
    @Nonnull
    public static Stream<ITerm> flattenrecursive( @Nonnull final Stream<ITerm> p_input )
    {
        return p_input.flatMap( i -> i instanceof ILiteral ? flattenrecursive( i.<ILiteral>term().orderedvalues() ) : Stream.of( i ) );
    }

    /*
     * recursive flattering of a stream structure
     *
     * @param p_list any stream
     * @return term stream
     */
    @Nonnull
    @SuppressWarnings( "unchecked" )
    private static Stream<ITerm> flattenstream( @Nonnull final Stream<?> p_stream )
    {
        return p_stream.flatMap( i ->
        {
            final Object l_value = i instanceof ITerm ? ( (ITerm) i ).raw() : i;
            return l_value instanceof Collection<?>
                   ? flattenstream( ( (Collection<?>) l_value ).stream() )
                   : Stream.of( CRawTerm.of( l_value ) );
        } );
    }

    /**
     * returns the hasing function for term data
     *
     * @return hasher
     */
    @Nonnull
    public static Hasher termhashing()
    {
        return Hashing.sipHash24().newHasher();
    }

    /**
     * creates a deep-clone of an object
     *
     * @param p_object input object
     * @return deep-copy
     *
     * @tparam T object type
     */
    @Nullable
    @SuppressWarnings( "unchecked" )
    public static <T> T deepclone( @Nullable final T p_object )
    {
        return Objects.isNull( p_object ) ? null : CLONER.deepClone( p_object );
    }

    // --- compression algorithm -------------------------------------------------------------------------------------------------------------------------------

    /**
     * calculates the levenshtein distance
     *
     * @param p_first first string
     * @param p_second second string
     * @param p_insertweight inserting weight
     * @param p_replaceweight replace weight
     * @param p_deleteweight delete weight
     * @return distance
     *
     * @see <a href="https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java"></a>
     */
    public static double levenshtein( @Nonnull final String p_first, @Nonnull final String p_second, final double p_insertweight,
                                      final double p_replaceweight, final double p_deleteweight
    )
    {
        // the array of distances
        double[] l_cost = IntStream.range( 0, p_first.length() + 1 ).mapToDouble( i -> i ).toArray();
        double[] l_newcost = new double[l_cost.length];

        for ( int j = 1; j < p_second.length() + 1; j++ )
        {
            l_newcost[0] = j;

            // calculate cost of operation for all characters
            for ( int i = 1; i < l_cost.length; i++ )
                l_newcost[i] = min(
                    l_cost[i - 1] + ( p_first.charAt( i - 1 ) == p_second.charAt( j - 1 ) ? 0 : p_replaceweight ),
                    l_newcost[i - 1] + p_deleteweight,
                    l_cost[i] + p_insertweight
                );

            final double[] l_swap = l_cost;
            l_cost = l_newcost;
            l_newcost = l_swap;
        }

        return l_cost[p_first.length()];
    }


    /**
     * returns the minimum of three elemens
     *
     * @param p_first first value
     * @param p_second second value
     * @param p_third third value
     * @return minimum
     */
    public static double min( final double p_first, final double p_second, final double p_third )
    {
        return Math.min( Math.min( p_first, p_second ), p_third );
    }


    /**
     * normalized-compression-distance
     *
     * @param p_compression compression algorithm
     * @param p_first first string
     * @param p_second second string
     * @return distance in [0,1]
     */
    public static double ncd( @Nonnull final ECompression p_compression, @Nonnull final String p_first, @Nonnull final String p_second )
    {
        if ( p_first.equals( p_second ) )
            return 0;

        final double l_first = compress( p_compression, p_first );
        final double l_second = compress( p_compression, p_second );
        return ( compress( p_compression, p_first + p_second ) - Math.min( l_first, l_second ) ) / Math.max( l_first, l_second );
    }


    /**
     * compression algorithm
     *
     * @param p_compression compression algorithm
     * @param p_input input string
     * @return number of compression bytes
     *
     * @warning counting stream returns the correct number of bytes after flushing
     */
    private static double compress( @Nonnull final ECompression p_compression, @Nonnull final String p_input )
    {
        final DataOutputStream l_counting = new DataOutputStream( NullOutputStream.NULL_OUTPUT_STREAM );

        try (
            InputStream l_input = new ByteArrayInputStream( p_input.getBytes( StandardCharsets.UTF_8 ) );
            OutputStream l_compress = p_compression.apply( l_counting )
        )
        {
            IOUtils.copy( l_input, l_compress );
        }
        catch ( final CompressorException | IOException l_exception )
        {
            return 0;
        }

        return l_counting.size();
    }


    /**
     * compression algorithm
     */
    public enum ECompression implements IExceptionFunction<DataOutputStream, OutputStream, CompressorException>
    {
        BZIP
        {
            @Override
            public OutputStream apply( @Nonnull final DataOutputStream p_in ) throws CompressorException
            {
                return new CompressorStreamFactory().createCompressorOutputStream( CompressorStreamFactory.BZIP2, p_in );
            }
        },
        GZIP
        {
            @Override
            public OutputStream apply( @Nonnull final DataOutputStream p_in ) throws CompressorException
            {
                return new CompressorStreamFactory().createCompressorOutputStream( CompressorStreamFactory.GZIP, p_in );
            }
        },
        DEFLATE
        {
            @Override
            public OutputStream apply( @Nonnull final DataOutputStream p_in ) throws CompressorException
            {
                return new CompressorStreamFactory().createCompressorOutputStream( CompressorStreamFactory.DEFLATE, p_in );
            }
        },
        DEFLATE64
        {
            @Override
            public OutputStream apply( @Nonnull final DataOutputStream p_in ) throws CompressorException
            {
                return new CompressorStreamFactory().createCompressorOutputStream( CompressorStreamFactory.DEFLATE64, p_in );
            }
        };

        /**
         * enum names
         */
        private static final Set<String> ALGORITHMS = Collections.unmodifiableSet(
            Arrays.stream( ECompression.values() )
                  .map( i -> i.name().toUpperCase( Locale.ROOT ) )
                  .collect( Collectors.toSet() )
        );

        /**
         * returns a compression value
         *
         * @param p_value string name
         * @return compression value
         */
        @Nonnull
        public static ECompression of( @Nonnull final String p_value )
        {
            return ECompression.valueOf( p_value.toUpperCase( Locale.ROOT ) );
        }


        /**
         * checks if a compression exists
         *
         * @param p_value compression name
         * @return existance flag
         */
        public static boolean exist( @Nonnull final String p_value )
        {
            return ALGORITHMS.contains( p_value.toUpperCase( Locale.ROOT ) );
        }

    }

}
