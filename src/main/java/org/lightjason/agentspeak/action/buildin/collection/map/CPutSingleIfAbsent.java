/*
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the LightJason AgentSpeak(L++)                                #
 * # Copyright (c) 2015-17, LightJason (info@lightjason.org)                            #
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

package org.lightjason.agentspeak.action.buildin.collection.map;

import org.lightjason.agentspeak.action.buildin.collection.IMapApplySingle;

import java.util.Map;


/**
 * adds an single element pair to all map iif not exists.
 * First and second argument is a key-value pair, all
 * other values are map references, the key-value pair
 * is added to all maps iif not exists and the action never fails
 *
 * @code collection/map/putsingleifabsent( "key", "value", Map1, Map2 ); @endcode
 */
public final class CPutSingleIfAbsent extends IMapApplySingle<Map<Object, Object>>
{

    @Override
    protected final void apply( final Map<Object, Object> p_instance, final Object p_key, final Object p_value )
    {
        p_instance.putIfAbsent( p_key, p_value );
    }

}