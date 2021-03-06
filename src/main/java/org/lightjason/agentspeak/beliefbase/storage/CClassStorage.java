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

package org.lightjason.agentspeak.beliefbase.storage;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.lightjason.agentspeak.language.CCommon;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.ILiteral;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * belief storage to get access to all class attributes
 *
 * @note all object attributes which are not transient will be read
 */
public final class CClassStorage<M> extends IBaseStorage<ILiteral, M>
{
    /**
     * object instance
     */
    private final Object m_instance;
    /**
     * map with functor and field reference
     */
    private final Map<String, Field> m_fields;

    /**
     * ctor
     *
     * @param p_instance object
     */
    public CClassStorage( @Nonnull final Object p_instance )
    {
        this( p_instance, i -> i.getName().toLowerCase( Locale.ROOT ).replace( "\\s+", "" ) );
    }

    /**
     * ctor
     *
     * @param p_instance object
     * @param p_fieldnameformater function to reformat field names
     */
    public CClassStorage( @Nonnull final Object p_instance, @Nonnull final Function<Field, String> p_fieldnameformater )
    {
        m_instance = p_instance;
        m_fields = Collections.unmodifiableMap(
            CCommon.classfields( m_instance.getClass() )
                   .peek( i -> i.setAccessible( true ) )
                   .filter( i -> !Modifier.isTransient( i.getModifiers() ) )
                   .filter( i -> !Modifier.isStatic( i.getModifiers() ) )
                   .map( i -> new ImmutablePair<>( i, p_fieldnameformater.apply( i ) ) )
                   .filter( i -> Objects.nonNull( i.right ) && !i.right.isEmpty() )
                   .collect( Collectors.toMap( i -> i.right, i -> i.left, ( i, j ) -> i ) )
        );
    }

    @Nonnull
    @Override
    public Stream<ILiteral> streammulti()
    {
        return m_fields.entrySet().stream()
                       .map( i -> this.literal( i.getKey(), i.getValue() ) )
                       .filter( Objects::nonNull );
    }

    @Nonnull
    @Override
    public Stream<M> streamsingle()
    {
        return Stream.empty();
    }

    @Override
    public boolean containsmulti( @Nonnull final String p_key )
    {
        return m_fields.containsKey( p_key );
    }

    @Override
    public boolean containssingle( @Nonnull final String p_key )
    {
        return false;
    }

    @Override
    public boolean putmulti( @Nonnull final String p_key, final ILiteral p_value )
    {
        final Field l_field = m_fields.get( p_key );
        if ( Objects.isNull( l_field ) || p_value.emptyValues() || Modifier.isFinal( l_field.getModifiers() ) )
            return false;

        try
        {
            l_field.set( m_instance, p_value.values().findFirst().orElseGet( () -> CRawTerm.of( null ) ).raw() );
            return true;
        }
        catch ( final IllegalAccessException l_exception )
        {
            return false;
        }
    }

    @Override
    public boolean putsingle( @Nonnull final String p_key, final M p_value )
    {
        return false;
    }

    @Override
    public boolean removemulti( @Nonnull final String p_key, final ILiteral p_value )
    {
        return false;
    }

    @Override
    public boolean removesingle( @Nonnull final String p_key )
    {
        return false;
    }

    @Override
    public M single( @Nonnull final String p_key )
    {
        return null;
    }

    @Override
    public M singleordefault( @Nonnull final String p_key, final M p_default )
    {
        return p_default;
    }

    @Nonnull
    @Override
    public Collection<ILiteral> multi( @Nonnull final String p_key )
    {
        final Field l_field = m_fields.get( p_key );
        return Objects.isNull( l_field ) ? Collections.emptySet() : Stream.of( this.literal( p_key, l_field ) ).collect( Collectors.toSet() );
    }

    @Override
    public IStorage<ILiteral, M> clear()
    {
        return this;
    }

    @Override
    public boolean isempty()
    {
        return m_fields.isEmpty();
    }

    @Override
    public int size()
    {
        return m_fields.size();
    }

    /**
     * returns a literal definition of the a class field
     *
     * @param p_name literal functor
     * @param p_field field reference
     * @return null or literal
     */
    @Nullable
    private ILiteral literal( @Nonnull final String p_name, @Nonnull final Field p_field )
    {
        try
        {
            final Object l_value = p_field.get( m_instance );
            return Objects.isNull( l_value ) ? CLiteral.of( p_name ) : CLiteral.of( p_name, CRawTerm.of( l_value ) );
        }
        catch ( final IllegalAccessException l_exception )
        {
            return null;
        }
    }

}
