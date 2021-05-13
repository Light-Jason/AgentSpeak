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

package org.lightjason.agentspeak.beliefbase.view;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.beliefbase.IBeliefbase;
import org.lightjason.agentspeak.common.CPath;
import org.lightjason.agentspeak.common.IPath;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.ILiteral;
import org.lightjason.agentspeak.language.ITerm;
import org.lightjason.agentspeak.language.execution.instantiable.plan.trigger.ITrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * view which can use a map of maps to represent
 * the hierarchical beliefbase structure
 *
 * @note given map should be thread-safe
 */
public final class CViewMap implements IView
{
    /**
     * view name
     */
    private final String m_name;
    /**
     * parent name
     */
    private final IView m_parent;
    /**
     * beliefbase
     */
    private final IBeliefbase m_beliefbase = new CWrapperBeliefbase();
    /**
     * root map
     */
    private final Map<String, Object> m_data;
    /**
     * path to key converting
     */
    private final Function<String, String> m_literaltokey;
    /**
     * key to literal converting
     */
    private final Function<String, String> m_keytoliteral;
    /**
     * clear consumer
     */
    private final Consumer<Map<String, Object>> m_clearconsumer;
    /**
     * add-view consumer
     */
    private final BiConsumer<String, Map<String, Object>> m_addviewconsumer;
    /**
     * add-literal consumer
     */
    private final BiConsumer<Pair<String, Stream<ITerm>>, Map<String, Object>> m_addliteralconsumer;
    /**
     * remove-view consumer
     */
    private final BiConsumer<String, Map<String, Object>> m_removeviewconsumer;
    /**
     * remove-literal consumer
     */
    private final BiConsumer<String, Map<String, Object>> m_removeliteralconsumer;

    /**
     * ctor
     *
     * @param p_name name of the view
     * @param p_map map reference
     */
    public CViewMap( @Nonnull final String p_name, @Nonnull final Map<String, Object> p_map )
    {
        this( p_name, p_map, null );
    }

    /**
     * ctor
     *
     * @param p_name view name
     * @param p_map map reference
     * @param p_parent parent view
     */
    public CViewMap( @Nonnull final String p_name, @Nonnull final Map<String, Object> p_map, @Nullable final IView p_parent )
    {
        this(
            p_name, p_map, p_parent,
            // add view
            ( i, j ) -> j.putIfAbsent( i, new ConcurrentHashMap<>() ),
            // add literal
            ( i, j ) -> i.getValue().limit( 1 ).forEach( n -> j.put( i.getKey(), n.raw() ) ),
            // remove view
            ( i, j ) ->
            {
                final Object l_data = j.get( i );
                if ( l_data instanceof Map<?, ?> )
                    j.remove( i, l_data );
            },
            // remove literal
            ( i, j ) ->
            {
                final Object l_data = j.get( i );
                if ( !( l_data instanceof Map<?, ?> ) )
                    j.remove( i, l_data );
            },
            Map::clear,
            i -> i.toLowerCase( Locale.ROOT ),
            i -> i.toLowerCase( Locale.ROOT )
        );
    }

    /**
     * ctor
     *
     * @param p_name view name
     * @param p_map map reference
     * @param p_parent parent view
     * @param p_addviewconsumer add-view consumer
     * @param p_addliteralconsumer add-literal consumer
     * @param p_removeviewconsumer remove-view consumer
     * @param p_removeliteralconsumer remove-view consumer
     * @param p_clearconsumer clear consumer
     * @param p_literaltokey converts a path ( functor item to a map key
     * @param p_keytoliteral converts a map key to literal path
     */
    public CViewMap( @Nonnull final String p_name, @Nonnull final Map<String, Object> p_map, @Nullable final IView p_parent,
                     @Nonnull final BiConsumer<String, Map<String, Object>> p_addviewconsumer,
                     @Nonnull final BiConsumer<Pair<String, Stream<ITerm>>, Map<String, Object>> p_addliteralconsumer,
                     @Nonnull final BiConsumer<String, Map<String, Object>> p_removeviewconsumer,
                     @Nonnull final BiConsumer<String, Map<String, Object>> p_removeliteralconsumer,
                     @Nonnull final Consumer<Map<String, Object>> p_clearconsumer,
                     @Nonnull final Function<String, String> p_literaltokey, @Nonnull final Function<String, String> p_keytoliteral
    )
    {
        m_name = p_name;
        m_parent = p_parent;
        m_data = p_map;
        m_literaltokey = p_literaltokey;
        m_keytoliteral = p_keytoliteral;
        m_clearconsumer = p_clearconsumer;
        m_addviewconsumer = p_addviewconsumer;
        m_addliteralconsumer = p_addliteralconsumer;
        m_removeviewconsumer = p_removeviewconsumer;
        m_removeliteralconsumer = p_removeliteralconsumer;
    }

    @Nonnull
    @Override
    public Stream<IView> walk( @Nonnull final IPath p_path, @Nullable final IViewGenerator... p_generator )
    {
        return this.walkdown( p_path, p_generator );
    }

    @Nonnull
    @Override
    public IView generate( @Nonnull final IViewGenerator p_generator, @Nonnull final IPath... p_paths )
    {
        return this;
    }

    @Nonnull
    @Override
    public Stream<IView> root()
    {
        return this.hasparent()
               ? Stream.concat( Stream.of( this ), Stream.of( this.parent() ).flatMap( IView::root ) )
               : Stream.empty();
    }

    @Nonnull
    @Override
    public IBeliefbase beliefbase()
    {
        return m_beliefbase;
    }

    @Nonnull
    @Override
    public IPath path()
    {
        return this.root().map( IView::name ).collect( CPath.collect() ).reverse();
    }

    @Nonnull
    @Override
    public String name()
    {
        return m_name;
    }

    @Nullable
    @Override
    public IView parent()
    {
        return m_parent;
    }

    @Override
    public boolean hasparent()
    {
        return Objects.nonNull( m_parent );
    }

    @Nonnull
    @Override
    public Stream<ITrigger> trigger()
    {
        return m_beliefbase.trigger( this );
    }

    @Nonnull
    @Override
    @SuppressWarnings( "unchecked" )
    public Stream<ILiteral> stream( @Nullable final IPath... p_path )
    {
        // build path relative to this view
        final IPath l_path = this.path();
        return ( Objects.isNull( p_path ) || p_path.length == 0
                 ? Stream.concat( m_beliefbase.streamliteral(), m_beliefbase.streamview().flatMap( IView::stream ) )
                 : Arrays.stream( p_path )
                         .flatMap( i -> this.leafview( this.walk( i.subpath( 0, -1 ) ) ).beliefbase().literal( i.suffix() ).stream() )
        ).map( i -> i.shallowcopy( l_path ) );
    }

    @Nonnull
    @Override
    public Stream<ILiteral> stream( final boolean p_negated, @Nullable final IPath... p_path )
    {
        return p_negated ? Stream.empty() : this.stream( p_path );
    }

    @Nonnull
    @Override
    public IView clear( @Nullable final IPath... p_path )
    {
        if ( Objects.isNull( p_path ) || p_path.length == 0 )
            m_beliefbase.clear();
        else
            Arrays.stream( p_path ).flatMap( i -> this.walkdown( i ) ).forEach( i -> i.clear() );

        return this;
    }

    @Nonnull
    @Override
    public IView add( @Nonnull final Stream<ILiteral> p_literal )
    {
        p_literal.forEach( m_beliefbase::add );
        return this;
    }

    @Nonnull
    @Override
    public IView add( @Nonnull final ILiteral... p_literal )
    {
        return this.add( Arrays.stream( p_literal ) );
    }

    @Nonnull
    @Override
    @SuppressWarnings( "varargs" )
    public IView add( @Nonnull final IView... p_view )
    {
        Arrays.stream( p_view ).forEach( m_beliefbase::add );
        return this;
    }

    @Nonnull
    @Override
    public IView remove( @Nonnull final Stream<ILiteral> p_literal )
    {
        p_literal.forEach( m_beliefbase::remove );
        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings( "varargs" )
    public IView remove( @Nonnull final ILiteral... p_literal )
    {
        return this.remove( Arrays.stream( p_literal ) );
    }

    @Nonnull
    @Override
    @SuppressWarnings( "varargs" )
    public IView remove( @Nonnull final IView... p_view )
    {
        Arrays.stream( p_view ).forEach( m_beliefbase::remove );
        return this;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public boolean containsliteral( @Nonnull final IPath p_path )
    {
        return !p_path.empty()
               && ( p_path.size() == 1
                    ? m_beliefbase.containsliteral( p_path.get( 0 ) )
                    : this.leafview( this.walk( p_path.subpath( 0, p_path.size() - 1 ) ) )
                          .containsliteral( p_path.subpath( p_path.size() - 1, p_path.size() ) )
               );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public boolean containsview( @Nonnull final IPath p_path )
    {
        return !p_path.empty()
               && ( p_path.size() == 1
                    ? m_beliefbase.containsview( p_path.get( 0 ) )
                    : this.leafview( this.walk( p_path.subpath( 0, p_path.size() - 1 ) ) )
                          .containsview( p_path.subpath( p_path.size() - 1, p_path.size() ) )
               );
    }

    @Override
    public boolean isempty()
    {
        return m_beliefbase.isempty();
    }

    @Override
    public int size()
    {
        return m_beliefbase.size();
    }

    @Nonnull
    @Override
    public IAgent<?> update( @Nonnull final IAgent<?> p_agent )
    {
        return m_beliefbase.update( p_agent );
    }

    /**
     * returns the leaf of a view path
     *
     * @param p_stream stream of views
     * @return last / leaf view
     */
    @Nonnull
    private IView leafview( @Nonnull final Stream<IView> p_stream )
    {
        return p_stream
            .reduce( ( i, j ) -> j )
            .orElse( this );
    }

    @SuppressWarnings( "unchecked" )
    private Stream<IView> walkdown( final IPath p_path, @Nullable final IViewGenerator... p_generator )
    {
        if ( p_path.empty() )
            return Stream.of( this );

        final String l_key = m_literaltokey.apply( p_path.get( 0 ) );
        final Object l_data = m_data.get( l_key );
        return l_data instanceof Map<?, ?>
               ? Stream.concat(
            Stream.of( this ),
            new CViewMap(
                l_key, (Map<String, Object>) l_data, this,
                m_addviewconsumer, m_addliteralconsumer, m_removeviewconsumer, m_removeliteralconsumer, m_clearconsumer, m_literaltokey, m_keytoliteral
            ).walk( p_path.subpath( 1 ), p_generator )
        )
               : Stream.of( this );
    }


    /**
     * wrapper beliefbase
     */
    private final class CWrapperBeliefbase implements IBeliefbase
    {

        @Override
        public boolean isempty()
        {
            return m_data.isEmpty();
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public int size()
        {
            return (int) m_data.values()
                               .stream()
                               .filter( i -> !( i instanceof Map<?, ?> ) )
                               .count()
                   + m_data.entrySet()
                           .stream()
                           .filter( i -> i instanceof Map<?, ?> )
                           .mapToInt( i -> new CViewMap( i.getKey(), (Map<String, Object>) i.getValue() ).size() )
                           .sum();
        }

        @Nonnull
        @Override
        public IAgent<?> update( @Nonnull final IAgent<?> p_agent )
        {
            return p_agent;
        }

        @Nonnull
        @Override
        public Stream<ITrigger> trigger( @Nonnull final IView p_view )
        {
            return Stream.empty();
        }

        @Nonnull
        @Override
        @SuppressWarnings( "unchecked" )
        public Stream<ILiteral> streamliteral()
        {
            return m_data.entrySet()
                         .stream()
                         .filter( i -> !( i.getValue() instanceof Map<?, ?> ) )
                         .map( i -> CLiteral.of( m_keytoliteral.apply( i.getKey() ), this.toterm( i.getValue() ) ) );
        }

        @Nonnull
        @Override
        @SuppressWarnings( "unchecked" )
        public Stream<IView> streamview()
        {
            return m_data.entrySet()
                         .stream()
                         .filter( i -> i.getValue() instanceof Map<?, ?> )
                         .map( i -> new CViewMap( m_keytoliteral.apply( i.getKey() ), (Map<String, Object>) i.getValue() ) );
        }

        @Nonnull
        @Override
        public IBeliefbase clear()
        {
            m_clearconsumer.accept( m_data );
            return this;
        }

        @Nonnull
        @Override
        public ILiteral add( @Nonnull final ILiteral p_literal )
        {
            m_addliteralconsumer.accept( new ImmutablePair<>( m_literaltokey.apply( p_literal.functor() ), p_literal.orderedvalues() ), m_data );
            return p_literal;
        }

        @Nonnull
        @Override
        public IView add( @Nonnull final IView p_view )
        {
            m_addviewconsumer.accept( m_keytoliteral.apply( p_view.name() ), m_data );
            return p_view;
        }

        @Nonnull
        @Override
        public ILiteral remove( @Nonnull final ILiteral p_literal )
        {
            m_removeliteralconsumer.accept( m_literaltokey.apply( p_literal.functor() ), m_data );
            return p_literal;
        }

        @Nonnull
        @Override
        public IView remove( @Nonnull final IView p_view )
        {
            m_removeviewconsumer.accept( m_literaltokey.apply( p_view.name() ), m_data );
            return p_view;
        }

        @Override
        public boolean containsliteral( @Nonnull final String p_key )
        {
            final String l_key = m_literaltokey.apply( p_key );
            return m_data.containsKey( l_key ) && !( m_data.get( l_key ) instanceof Map<?, ?> );
        }

        @Override
        public boolean containsview( @Nonnull final String p_key )
        {
            final String l_key = m_literaltokey.apply( p_key );
            return m_data.containsKey( l_key ) && m_data.get( l_key ) instanceof Map<?, ?>;
        }

        @Nullable
        @Override
        public IView view( @Nonnull final String p_key )
        {
            return CViewMap.this;
        }

        @Nonnull
        @Override
        public Collection<ILiteral> literal( @Nonnull final String p_key )
        {
            final String l_key = m_literaltokey.apply( p_key );
            if ( !m_data.containsKey( l_key ) )
                return Collections.emptySet();

            final Object l_data = m_data.get( l_key );
            if ( m_data.get( l_key ) instanceof Map<?, ?> )
                return Collections.emptySet();

            return Stream.of( CLiteral.of( p_key, this.toterm( l_data ) ) ).collect( Collectors.toSet() );
        }

        @Nullable
        @Override
        public IView viewordefault( @Nonnull final String p_key, @Nullable final IView p_default )
        {
            return CViewMap.this;
        }

        @Nonnull
        @Override
        public IView create( @Nonnull final String p_name )
        {
            return CViewMap.this;
        }

        @Nonnull
        @Override
        public IView create( @Nonnull final String p_name, @Nullable final IView p_parent )
        {
            return CViewMap.this;
        }

        @SuppressWarnings( "unchecked" )
        private Stream<ITerm> toterm( final Object p_value )
        {
            if ( p_value instanceof Collection<?> )
                return ( (Collection<Object>) p_value ).stream().flatMap( this::toterm );

            if ( p_value instanceof Map<?, ?> )
                return ( (Map<String, Object>) p_value ).entrySet().stream()
                                                        .map( i -> CLiteral.of( m_keytoliteral.apply( i.getKey() ), this.toterm( i.getValue() ) ) );

            if ( p_value instanceof Integer )
                return Stream.of( CRawTerm.of( ( (Number) p_value ).longValue() ) );

            return Stream.of( CRawTerm.of( p_value ) );
        }
    }
}
