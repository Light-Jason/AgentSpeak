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

package org.lightjason.agentspeak.beliefbase;

import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.beliefbase.storage.IStorage;
import org.lightjason.agentspeak.beliefbase.view.IView;
import org.lightjason.agentspeak.language.ILiteral;
import org.lightjason.agentspeak.language.execution.instantiable.plan.trigger.ITrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Stream;


/**
 * beliefbase to generate any event-based data by reference counting
 */
public final class CBeliefbase extends IBaseBeliefbase
{
    /**
     * storage with data
     */
    private final IStorage<ILiteral, IView> m_storage;

    /**
     * ctor
     *
     * @param p_storage storage
     */
    public CBeliefbase( @Nonnull final IStorage<ILiteral, IView> p_storage )
    {
        m_storage = p_storage;
    }

    @Override
    public int hashCode()
    {
        return m_storage.hashCode();
    }

    @Override
    public boolean equals( final Object p_object )
    {
        return p_object instanceof IBeliefbase && this.hashCode() == p_object.hashCode();
    }

    @Nonnull
    @Override
    public ILiteral add( @Nonnull final ILiteral p_literal )
    {
        return m_storage.putmulti( p_literal.functor(), p_literal )
               ? super.add( p_literal )
               : p_literal;
    }

    @Nonnull
    @Override
    public IView add( @Nonnull final IView p_view )
    {
        m_storage.putsingle( p_view.name(), p_view );
        return p_view;
    }

    @Nonnull
    @Override
    public IView remove( @Nonnull final IView p_view )
    {
        m_storage.removesingle( this.internalremove( p_view ).name() );
        return p_view;
    }

    @Nonnull
    @Override
    public ILiteral remove( @Nonnull final ILiteral p_literal )
    {
        return m_storage.removemulti( p_literal.functor(), p_literal )
               ? super.remove( p_literal )
               : p_literal;
    }

    @Override
    public boolean containsliteral( @Nonnull final String p_key )
    {
        return m_storage.containsmulti( p_key );
    }

    @Override
    public boolean containsview( @Nonnull final String p_key )
    {
        return m_storage.containssingle( p_key );
    }

    @Nonnull
    @Override
    public IView view( @Nonnull final String p_key )
    {
        return m_storage.single( p_key );
    }

    @Nonnull
    @Override
    public IView viewordefault( @Nonnull final String p_key, @Nullable final IView p_default )
    {
        return m_storage.singleordefault( p_key, p_default );
    }

    @Nonnull
    @Override
    public Collection<ILiteral> literal( @Nonnull final String p_key )
    {
        return m_storage.multi( p_key );
    }

    @Nonnull
    @Override
    public IAgent<?> update( @Nonnull final IAgent<?> p_agent )
    {
        super.update( p_agent );
        m_storage.streamsingle().parallel().forEach( i -> i.update( p_agent ) );
        return m_storage.update( p_agent );
    }

    @Nonnull
    @Override
    public IBeliefbase clear()
    {
        // create delete-event for all literals
        m_storage
            .streammulti()
            .parallel()
            .forEach( i -> this.event( ITrigger.EType.DELETEBELIEF, i ) );

        m_storage.streamsingle().parallel().forEach( i -> i.clear() );
        m_storage.clear();

        return this;
    }

    @Override
    public boolean isempty()
    {
        return m_storage.isempty();
    }

    @Override
    public int size()
    {
        return m_storage.size() + m_storage.streamsingle().parallel().mapToInt( IStructure::size ).sum();
    }

    @Nonnull
    @Override
    public Stream<ITrigger> trigger( @Nonnull final IView p_view )
    {
        return Stream.concat(
            super.trigger( p_view ).parallel(),
            m_storage.streamsingle().parallel().flatMap( IView::trigger )
        );
    }

    @Nonnull
    @Override
    public Stream<ILiteral> streamliteral()
    {
        return m_storage.streammulti();
    }

    @Nonnull
    @Override
    public Stream<IView> streamview()
    {
        return m_storage.streamsingle();
    }

    @Override
    public String toString()
    {
        return m_storage.toString();
    }

}
