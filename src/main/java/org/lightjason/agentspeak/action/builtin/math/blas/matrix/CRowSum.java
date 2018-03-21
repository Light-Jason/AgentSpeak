/*
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the LightJason AgentSpeak(L++)                                #
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

package org.lightjason.agentspeak.action.builtin.math.blas.matrix;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.SparseDoubleMatrix1D;
import org.lightjason.agentspeak.action.builtin.math.blas.EType;
import org.lightjason.agentspeak.action.builtin.math.blas.IAlgebra;
import org.lightjason.agentspeak.language.CCommon;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.ITerm;
import org.lightjason.agentspeak.language.execution.IContext;
import org.lightjason.agentspeak.language.fuzzy.CFuzzyValue;
import org.lightjason.agentspeak.language.fuzzy.IFuzzyValue;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.IntStream;


/**
 * returns the row-sum of a matrix.
 * The action returns the row-sum of all matrix objects,
 * a string value defines a sparse or dense resulting vector,
 * the action never fails
 *
 * {@code
    [S1|S2] = math/blas/matrix/rowsum( Matrix1, Matrix2 );
    [S1|S2] = math/blas/matrix/rowsum( Matrix1, Matrix2, "sparse" );
 * }
 */
public final class CRowSum extends IAlgebra
{
    /**
     * serial id
     */
    private static final long serialVersionUID = -7454809931903398817L;

    @Nonnegative
    @Override
    public final int minimalArgumentNumber()
    {
        return 1;
    }

    @Nonnull
    @Override
    public final IFuzzyValue<Boolean> execute( final boolean p_parallel, @Nonnull final IContext p_context,
                                               @Nonnull final List<ITerm> p_argument, @Nonnull final List<ITerm> p_return )
    {
        final EType l_type = CCommon.flatten( p_argument )
                                    .parallel()
                                    .filter( i -> CCommon.rawvalueAssignableTo( i, String.class ) )
                                    .findFirst()
                                    .map( ITerm::<String>raw )
                                    .map( EType::of )
                                    .orElse( EType.DENSE );

        CCommon.flatten( p_argument )
               .filter( i -> CCommon.rawvalueAssignableTo( i, DoubleMatrix2D.class ) )
               .map( ITerm::<DoubleMatrix2D>raw )
               .map( i -> IntStream.range( 0, i.rows() ).boxed().map( i::viewRow ).mapToDouble( DoubleMatrix1D::zSum ).toArray() )
               .map( i -> generate( i, l_type ) )
               .map( CRawTerm::of )
               .forEach( p_return::add );

        return CFuzzyValue.of( true );
    }

    /**
     * generates a vector
     *
     * @param p_value values
     * @param p_type type
     * @return vector
     */
    @Nonnull
    private static DoubleMatrix1D generate( @Nonnull final double[] p_value, @Nonnull final EType p_type )
    {
        switch ( p_type )
        {
            case SPARSE: return new SparseDoubleMatrix1D( p_value );
            default : return new DenseDoubleMatrix1D( p_value );
        }
    }

}
