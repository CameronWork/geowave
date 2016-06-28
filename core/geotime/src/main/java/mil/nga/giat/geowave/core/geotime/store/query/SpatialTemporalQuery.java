package mil.nga.giat.geowave.core.geotime.store.query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

import mil.nga.giat.geowave.core.geotime.GeometryUtils;
import mil.nga.giat.geowave.core.geotime.index.dimension.TimeDefinition;
import mil.nga.giat.geowave.core.geotime.store.filter.SpatialQueryFilter.CompareOperation;
import mil.nga.giat.geowave.core.index.sfc.data.NumericRange;

/**
 * The Spatial Temporal Query class represents a query in three dimensions. The
 * constraint that is applied represents an intersection operation on the query
 * geometry AND a date range intersection based on startTime and endTime.
 * 
 * 
 */
public class SpatialTemporalQuery extends
		SpatialQuery
{
	protected SpatialTemporalQuery() {}

	public SpatialTemporalQuery(
			final Date startTime,
			final Date endTime,
			final Geometry queryGeometry ) {
		super(
				createSpatialTemporalConstraints(
						startTime,
						endTime,
						queryGeometry),
				queryGeometry);
	}

	public SpatialTemporalQuery(
			final TemporalConstraints constraints,
			final Geometry queryGeometry ) {
		super(
				createSpatialTemporalConstraints(
						constraints,
						queryGeometry),
				queryGeometry);
	}

	/**
	 * If more then on polygon is supplied in the geometry, then the range of
	 * time is partnered with each polygon constraint.
	 * 
	 * @param startTime
	 * @param endTime
	 * @param queryGeometry
	 * @param compareOp
	 */
	public SpatialTemporalQuery(
			final Date startTime,
			final Date endTime,
			final Geometry queryGeometry,
			final CompareOperation compareOp ) {
		super(
				createSpatialTemporalConstraints(
						startTime,
						endTime,
						queryGeometry),
				queryGeometry,
				compareOp);
	}

	/**
	 * Applies the set of temporal constraints to the boundaries of the provided
	 * polygon. If a multi-polygon is provided, then all matching combinations
	 * between temporal ranges and polygons are explored.
	 * 
	 * @param constraints
	 * @param queryGeometry
	 * @param compareOp
	 */

	public SpatialTemporalQuery(
			final TemporalConstraints constraints,
			final Geometry queryGeometry,
			final CompareOperation compareOp ) {
		super(
				createSpatialTemporalConstraints(
						constraints,
						queryGeometry),
				queryGeometry,
				compareOp);
	}

	public static ConstraintSet createConstraints(
			final TemporalRange temporalRange,
			final boolean isDefault ) {
		return new ConstraintSet(
				TimeDefinition.class,
				new ConstraintData(
						new NumericRange(
								temporalRange.getStartTime().getTime() + 1,
								temporalRange.getEndTime().getTime() - 1),
						isDefault));

	}

	public static Constraints createConstraints(
			final TemporalConstraints temporalConstraints,
			final boolean isDefault ) {
		final List<ConstraintSet> constraints = new ArrayList<ConstraintSet>();
		for (final TemporalRange range : temporalConstraints.getRanges()) {
			constraints.add(new ConstraintSet(
					TimeDefinition.class,
					new ConstraintData(
							new NumericRange(
									range.getStartTime().getTime()+1,
									range.getEndTime().getTime()-1),
							isDefault)));
		}
		return new Constraints(
				constraints);
	}

	/**
	 * Supports multi-polygons and multiple temporal bounds. Creates all
	 * matchings between polygon and temporal bounds.
	 * 
	 * @param startTime
	 * @param endTime
	 * @param queryGeometry
	 * @return
	 */
	private static Constraints createSpatialTemporalConstraints(
			final TemporalConstraints temporalConstraints,
			final Geometry queryGeometry ) {
		final Constraints geoConstraints = GeometryUtils.basicConstraintsFromGeometry(queryGeometry);
		final Constraints timeConstraints = createConstraints(
				temporalConstraints,
				false);
		return geoConstraints.merge(timeConstraints);
	}

	/**
	 * Supports multi-polygons. Applies 'temporal bounds' to each geometric
	 * constraint.
	 * 
	 * @param startTime
	 * @param endTime
	 * @param queryGeometry
	 * @return
	 */
	private static Constraints createSpatialTemporalConstraints(
			final Date startTime,
			final Date endTime,
			final Geometry queryGeometry ) {
		final Constraints geoConstraints = GeometryUtils.basicConstraintsFromGeometry(queryGeometry);
		return geoConstraints.merge(new Constraints(
				new ConstraintSet(
						TimeDefinition.class,
						new ConstraintData(
								new NumericRange(
										startTime.getTime()+1,
										endTime.getTime()-1),
								false))));
	}

}
