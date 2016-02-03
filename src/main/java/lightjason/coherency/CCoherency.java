/**
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the Light-Jason                                               #
 * # Copyright (c) 2015-16, Philipp Kraus (philipp.kraus@tu-clausthal.de)               #
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

package lightjason.coherency;

import cern.colt.function.DoubleFunction;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.EigenvalueDecomposition;
import cern.jet.math.Mult;
import lightjason.agent.IAgent;
import lightjason.coherency.metric.IMetric;
import lightjason.common.CCommon;
import lightjason.error.CIllegalStateException;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;


/**
 * layer with coherency data
 *
 * @see https://dst.lbl.gov/ACSSoftware/colt/
 */
public final class CCoherency implements Callable<CCoherency>
{
    /**
     * algebra object
     */
    private static final Algebra ALGEBRA = new Algebra();
    /**
     * algorithm to calculate stationary probability
     **/
    private final EAlgorithm m_algorithm;
    /**
     * map with object and coherency value
     **/
    private final Map<IAgent, Double> m_data = new ConcurrentHashMap<>();
    /**
     * epsilon value to create an aperiodic markow-chain
     **/
    private final double m_epsilon;
    /**
     * number of iterations of the stochastic algorithm
     **/
    private final int m_iteration;
    /**
     * metric object to create the value of two objects
     **/
    private IMetric m_metric;

    /**
     * ctor - use numeric algorithm
     *
     * @param p_metric object metric
     */
    public CCoherency( final IMetric p_metric )
    {
        m_metric = p_metric;
        m_algorithm = EAlgorithm.Numeric;
        m_iteration = 0;
        m_epsilon = 0.001;
    }

    /**
     * ctor - use stochastic algorithm
     *
     * @param p_metric object metric
     * @param p_iteration iterations
     * @param p_epsilon epsilon value
     */
    public CCoherency( final IMetric p_metric, final int p_iteration, final double p_epsilon )
    {
        m_metric = p_metric;
        m_algorithm = EAlgorithm.Iteration;
        m_iteration = p_iteration;
        m_epsilon = p_epsilon;
    }

    /**
     * returns the coherency value of an object
     *
     * @param p_object object
     * @return value
     */
    public final Double getValue( final IAgent p_object )
    {
        return m_data.get( p_object );
    }

    /**
     * adds a new object
     *
     * @param p_object new object
     */
    public final boolean add( final IAgent p_object )
    {
        return m_data.putIfAbsent( p_object, new Double( 0 ) ) == null;
    }

    @Override
    public final CCoherency call() throws Exception
    {
        if ( m_data.size() < 2 )
            return this;

        // get key list of map for addressing elements in the correct order
        final ArrayList<IAgent> l_keys = new ArrayList<>( m_data.keySet() );

        // calculate markov chain transition matrix
        final DoubleMatrix2D l_matrix = new DenseDoubleMatrix2D( m_data.size(), m_data.size() );
        IntStream.range( 0, l_keys.size() ).boxed().forEach( i -> {

            final IAgent l_item = l_keys.get( i );
            IntStream.range( i + 1, l_keys.size() ).boxed().forEach( j -> {

                final double l_value = this.getMetricValue( l_item, l_keys.get( j ) );
                l_matrix.setQuick( i, j, l_value );
                l_matrix.setQuick( j, i, l_value );

            } );

            // row-wise normalization for getting probabilities
            final double l_norm = ALGEBRA.norm1( l_matrix.viewRow( i ) );
            if ( l_norm != 0 )
                l_matrix.viewRow( i ).assign( Mult.div( l_norm ) );

            // set epsilon slope for preventing periodic markov chains
            l_matrix.setQuick( i, i, m_epsilon );
        } );

        final DoubleMatrix1D l_eigenvector;
        if ( l_matrix.zSum() <= m_data.size() * m_epsilon )
            l_eigenvector = new DenseDoubleMatrix1D( m_data.size() );
        else
            l_eigenvector = this.getStationaryDistribution( l_matrix );

        // set coherency value for each entry (it is the inverted probability)
        IntStream.range( 0, l_keys.size() ).boxed().forEach( i -> m_data.put( l_keys.get( i ), 1 - l_eigenvector.get( i ) ) );

        return this;
    }

    /**
     * gets the current metric
     *
     * @return get metric
     */
    public final IMetric getMetric()
    {
        return m_metric;
    }

    /**
     * sets the metric
     *
     * @param p_metric metric
     */
    public final void setMetric( final IMetric p_metric )
    {
        m_metric = p_metric;
    }

    /**
     * removes an object
     *
     * @param p_object removing object
     */
    public final boolean remove( final IAgent p_object )
    {
        return m_data.remove( p_object ) != null;
    }

    /**
     * get the largest eigen vector with QR decomposition
     *
     * @param p_matrix matrix
     * @return largest eigenvector (not normalized)
     */
    private static DoubleMatrix1D getLargestEigenvector( final DoubleMatrix2D p_matrix )
    {
        final EigenvalueDecomposition l_eigen = new EigenvalueDecomposition( p_matrix );

        // gets the position of the largest eigenvalue
        final DoubleMatrix1D l_eigenvalues = l_eigen.getRealEigenvalues();

        int l_position = 0;
        for ( int i = 0; i < l_eigenvalues.size(); ++i )
            if ( l_eigenvalues.get( i ) > l_eigenvalues.get( l_position ) )
                l_position = i;

        // gets the largest eigenvector
        return l_eigen.getV().viewColumn( l_position );
    }

    /**
     * get the largest eigen vector based on the perron-frobenius theorem
     *
     * @param p_matrix matrix
     * @param p_iteration number of iterations
     * @return largest eigenvector (not normalized)
     *
     * @see http://en.wikipedia.org/wiki/Perron%E2%80%93Frobenius_theorem
     */
    private static DoubleMatrix1D getLargestEigenvector( final DoubleMatrix2D p_matrix, final int p_iteration )
    {
        DoubleMatrix1D l_probability = DoubleFactory1D.dense.random( p_matrix.rows() );
        for ( int i = 0; i < p_iteration; ++i )
        {
            l_probability = ALGEBRA.mult( p_matrix, l_probability );
            l_probability.assign(
                    Mult.div(
                            ALGEBRA.norm2( l_probability )
                    )
            );
        }

        return l_probability;
    }

    /**
     * returns metric value
     *
     * @param p_first first element
     * @param p_second secend element
     * @return metric value
     */
    private final double getMetricValue( final IAgent p_first, final IAgent p_second )
    {
        if ( p_first.equals( p_second ) )
            return 0;

        return m_metric.calculate( p_first, p_second );
    }

    /**
     * calculates the stationary distribution
     *
     * @param p_matrix transition matrix
     * @return stationary distribution
     */
    private DoubleMatrix1D getStationaryDistribution( final DoubleMatrix2D p_matrix )
    {
        final DoubleMatrix1D l_eigenvector;

        switch ( m_algorithm )
        {
            case Iteration:
                l_eigenvector = getLargestEigenvector( p_matrix, m_iteration );
                break;

            case Numeric:
                l_eigenvector = getLargestEigenvector( p_matrix );
                break;

            default:
                throw new CIllegalStateException( CCommon.getLanguageLabel( this.getClass(), "algorithm" ) );
        }

        // normalize eigenvector and create positiv oriantation
        l_eigenvector.assign( Mult.div( ALGEBRA.norm1( l_eigenvector ) ) );
        l_eigenvector.assign(
                new DoubleFunction()
                {
                    @Override
                    public double apply( final double p_value )
                    {
                        return Math.abs( p_value );
                    }
                }
        );

        return l_eigenvector;
    }

    /**
     * numeric algorithm structure
     */
    private enum EAlgorithm
    {
        /**
         * use numeric algorithm (QR decomposition)
         **/
        Numeric,
        /**
         * use stochastic algorithm (fixpoint iteration)
         **/
        Iteration
    }

}