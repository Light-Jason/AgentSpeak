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

package org.lightjason.agentspeak.grammar;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.lightjason.agentspeak.error.parser.CParserInitializationError;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * generic default parser
 */
public abstract class IBaseParser<T extends IASTVisitor, L extends Lexer, P extends Parser> implements IParser<T>
{
    /**
     * error listener
     */
    private final ANTLRErrorListener m_errorlistener;
    /**
     * ctor lexer reference
     */
    private final Constructor<L> m_ctorlexer;
    /**
     * ctor parser reference
     */
    private final Constructor<P> m_ctorparser;


    /**
     * ctor
     *
     * @param p_errorlistener listener instance
     */
    protected IBaseParser( @Nonnull final ANTLRErrorListener p_errorlistener )
    {
        m_errorlistener = p_errorlistener;
        try
        {
            m_ctorlexer = this.lexerclass().getConstructor( CharStream.class );
            m_ctorparser = this.parserclass().getConstructor( TokenStream.class );
        }
        catch ( final NoSuchMethodException l_exception )
        {
            throw new CParserInitializationError( l_exception );
        }

    }

    /**
     * returns a parser component
     *
     * @param p_stream input stream
     * @return parser (for using in visitor interface)
     */
    protected final P parser( @Nonnull final InputStream p_stream )
    {
        final L l_lexer;
        try
        {
            l_lexer = m_ctorlexer.newInstance( CharStreams.fromStream( p_stream ) );
            l_lexer.removeErrorListeners();
            l_lexer.addErrorListener( m_errorlistener );

            final P l_parser = m_ctorparser.newInstance( new CommonTokenStream( l_lexer ) );
            l_parser.removeErrorListeners();
            l_parser.addErrorListener( m_errorlistener );

            return l_parser;
        }
        catch ( final InstantiationException | IllegalAccessException | InvocationTargetException | IOException l_exception )
        {
            throw new CParserInitializationError( l_exception );
        }
    }

    /**
     * returns the lexer class reference
     *
     * @return class of lexer
     */
    protected abstract Class<L> lexerclass();

    /**
     * returns the parser class reference
     *
     * @return class of parser
     */
    protected abstract Class<P> parserclass();
}
