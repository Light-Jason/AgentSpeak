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

package org.lightjason.agentspeak.language.variable;

import org.lightjason.agentspeak.common.IPath;
import org.lightjason.agentspeak.language.CCommon;
import org.lightjason.agentspeak.language.ITerm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


/**
 * class for a mutex relocated variable
 *
 * @tparam T variable type
 */
public final class CRelocateMutexVariable<T> extends IBaseVariable<T> implements IRelocateVariable<T>
{
    /**
     * serial id
     */
    private static final long serialVersionUID = 7607406945094631524L;
    /**
     * reference to relocated variable
     */
    private final IVariable<?> m_relocate;
    /**
     * thread-safe value
     */
    private final AtomicReference<T> m_value = new AtomicReference<>();

    /**
     * ctor
     *
     * @param p_variable variable which should be reloacted
     */
    public CRelocateMutexVariable( final IVariable<?> p_variable )
    {
        super( p_variable.fqnfunctor() );
        this.setvalue( p_variable.raw() );
        m_relocate = p_variable;
    }

    /**
     * ctor
     *
     * @param p_functor variable name
     * @param p_relocate variable which should be relocated
     */
    public CRelocateMutexVariable( @Nonnull final IPath p_functor, @Nonnull final IVariable<?> p_relocate )
    {
        super( p_functor );
        this.setvalue( p_relocate.raw() );
        m_relocate = p_relocate;
    }

    /**
     * private ctor for creating object-copy
     *
     * @param p_functor functor
     * @param p_variable referenced variable
     * @param p_value value
     */
    private CRelocateMutexVariable( @Nonnull final IPath p_functor, @Nonnull final IVariable<?> p_variable, @Nullable final T p_value )
    {
        super( p_functor );
        this.setvalue( p_value );
        m_relocate = p_variable;
    }

    /**
     * private ctor for creating object-copy
     *
     * @param p_functor functor
     * @param p_variable referenced variable
     * @param p_value value
     */
    private CRelocateMutexVariable( @Nonnull final String p_functor, @Nonnull final IVariable<?> p_variable, @Nullable final T p_value )
    {
        super( p_functor );
        this.setvalue( p_value );
        m_relocate = p_variable;
    }

    @Nonnull
    @Override
    public IVariable<?> relocate()
    {
        return m_relocate instanceof CConstant<?>
               ? m_relocate
               : m_relocate.set( this.raw() );
    }

    @Override
    public boolean mutex()
    {
        return true;
    }

    @Nonnull
    @Override
    public IVariable<T> shallowcopy( @Nullable final IPath... p_prefix )
    {
        return Objects.isNull( p_prefix ) || p_prefix.length == 0
               ? new CRelocateMutexVariable<>( m_functor, m_relocate, m_value.get() )
               : new CRelocateMutexVariable<>( p_prefix[0].append( m_functor ), m_relocate, m_value.get() );
    }

    @Nonnull
    @Override
    public IVariable<T> shallowcopysuffix()
    {
        return new CRelocateMutexVariable<>( m_functor.suffix(), m_relocate, m_value.get() );
    }

    @Nonnull
    @Override
    public IVariable<T> shallowcopywithoutsuffix()
    {
        return new CRelocateMutexVariable<>( m_functor.subpath( 0, m_functor.size() - 1 ), m_relocate, m_value.get() );
    }

    @Nonnull
    @Override
    public ITerm deepcopy( @Nullable final IPath... p_prefix )
    {
        return new CRelocateMutexVariable<>(
            Objects.isNull( p_prefix ) || p_prefix.length == 0
            ? m_functor
            : p_prefix[0].append( m_functor ),
            m_relocate,
            CCommon.deepclone( m_value.get() )
        );
    }

    @Nonnull
    @Override
    public ITerm deepcopysuffix()
    {
        return new CRelocateMutexVariable<>( m_functor.suffix(), m_relocate, CCommon.deepclone( m_value.get() ) );
    }


    @Nonnull
    @Override
    protected IVariable<T> setvalue( @Nullable final T p_value )
    {
        m_value.set( p_value );
        return this;
    }

    @Nullable
    @Override
    protected T getvalue()
    {
        return m_value.get();
    }

    @Override
    public String toString()
    {
        return MessageFormat.format( "{0}({1})>{2}", m_functor, Objects.isNull( m_value ) ? "" : m_value, m_relocate );
    }

}
