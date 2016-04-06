import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter;
import mil.nga.giat.geowave.adapter.vector.plugin.GeoWaveGTDataStore;
import mil.nga.giat.geowave.core.geotime.GeometryUtils;
import mil.nga.giat.geowave.core.geotime.store.filter.SpatialQueryFilter;
import mil.nga.giat.geowave.core.geotime.store.query.SpatialQuery;
import mil.nga.giat.geowave.core.geotime.store.query.SpatialTemporalQuery;
import mil.nga.giat.geowave.core.index.ByteArrayId;
import mil.nga.giat.geowave.core.ingest.socket.JSONAggregation;
import mil.nga.giat.geowave.core.store.CloseableIterator;
import mil.nga.giat.geowave.core.store.DataStore;
import mil.nga.giat.geowave.core.store.adapter.AdapterStore;
import mil.nga.giat.geowave.core.store.query.QueryOptions;
import mil.nga.giat.geowave.datastore.accumulo.AccumuloDataStore;
import mil.nga.giat.geowave.datastore.accumulo.BasicAccumuloOperations;
import mil.nga.giat.geowave.datastore.accumulo.metadata.AccumuloAdapterStore;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.operation.TransformException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by cameron on 21/03/16.
 */
class DataHandler extends AbstractHandler {

    private static DataStore dataStore;
    private QueryOptions queryOptions;
    DataHandler() throws AccumuloSecurityException, AccumuloException {
        String zookeepers = "localhost:2181";
        String accumuloInstance = "geowave";
        String accumuloUser = "root";
        String accumuloPass = "password";
        String geowaveNamespace = "aisdata";

        final BasicAccumuloOperations operations = new BasicAccumuloOperations(
                zookeepers,
                accumuloInstance,
                accumuloUser,
                accumuloPass,
                geowaveNamespace);
        dataStore = new AccumuloDataStore(
                operations);
        AdapterStore adapterStore = new AccumuloAdapterStore(
                operations);
        ByteArrayId bfAdId = new ByteArrayId(
                "AIS_FEATURE");
        FeatureDataAdapter bfAdapter = (FeatureDataAdapter) adapterStore.getAdapter(bfAdId);
        queryOptions = new QueryOptions(bfAdapter);
        queryOptions.setAggregation(new JSONAggregation<SimpleFeature>(),
                bfAdapter);

    }


    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {
        String file = request.getPathInfo();
        if (file.contains("/geosearch")) {
            String args = request.getParameter("values");
            String values[] = args.split(",");
            String timeStart = request.getParameter("startTime");
            String timeEnd = request.getParameter("endTime");
            SpatialQuery query = null;

            try {
                if (values.length < 3) {
                    System.out.println("INVALID QUERY");
                    return;
                } else if (values.length == 3) {
                    System.out.println("RADIUS QUERY");
                    query = radiusQuery(values, timeStart, timeEnd);
                } else if ((values.length & 1) == 0) //only even numbers
                {
                    System.out.println("POLY QUERY");
                    query = polyQuery(values, timeStart, timeEnd);
                }
                CloseableIterator<?> iterator = dataStore.query(
                        queryOptions,
                        query);
                if (iterator != null) {
                    writeJSON(iterator, response.getWriter());
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                } else {
                    response.sendError(500);
                }
            } catch (ParseException | TransformException | com.vividsolutions.jts.io.ParseException e) {
                e.printStackTrace();
            }


        }
    }

    private void writeJSON(CloseableIterator<?> iterator, PrintWriter writer) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        while (iterator.hasNext()) {
            Object json = iterator.next();
            if (json instanceof JSONAggregation) {
                sb.append(json.toString());
                if (iterator.hasNext()) {
                    sb.append(",");
                }
            }
        }
        sb.append("}");
        writer.println(sb.toString());
    }

    /**
     * Searches for values in a radius around a point
     * @param values
     * @return
     * @throws TransformException
     * @throws ParseException
     */
    private SpatialQuery radiusQuery(String[] values) throws TransformException, ParseException {
        SpatialQuery query = new SpatialQuery(
                mil.nga.giat.geowave.adapter.vector.utils.GeometryUtils.buffer(
                        GeoWaveGTDataStore.DEFAULT_CRS,
                        GeometryUtils.GEOMETRY_FACTORY.createPoint(new Coordinate(
                                Double.parseDouble(values[1]),
                                Double.parseDouble(values[0]))),
                        "meter",
                        Double.parseDouble(values[2]) * 1000).getKey(),
                SpatialQueryFilter.CompareOperation.CONTAINS);
        return query;
    }

    /**
     * Searches for values in a radius around a point, between two times
     * @param values
     * @return
     * @throws TransformException
     * @throws ParseException
     */
    private SpatialQuery radiusQuery(String[] values, String timeStart, String timeEnd) throws TransformException, ParseException {
        Date startTime;
        Date endTime;
        if (timeStart == null && timeEnd == null) {
            return radiusQuery(values);
        }
        try {
            startTime = new Date(Long.parseLong(timeStart));
        }
        catch (Exception e) {
            startTime = new Date(0);
        }

        try {
            endTime = new Date(Long.parseLong(timeEnd));
        }
        catch (Exception e) {
            endTime = new Date();
        }
        SpatialQuery query = new SpatialTemporalQuery(
                startTime,
                endTime,
                mil.nga.giat.geowave.adapter.vector.utils.GeometryUtils.buffer(
                        GeoWaveGTDataStore.DEFAULT_CRS,
                        GeometryUtils.GEOMETRY_FACTORY.createPoint(new Coordinate(
                                Double.parseDouble(values[1]),
                                Double.parseDouble(values[0]))),
                        "meter",
                        Double.parseDouble(values[2]) * 1000).getKey(),
                SpatialQueryFilter.CompareOperation.CONTAINS);

        return query;
    }

    /**
     * Searches for results in a query
     * @param values
     * @return
     * @throws ParseException
     * @throws IOException
     * @throws com.vividsolutions.jts.io.ParseException
     */
    private SpatialQuery polyQuery(String values[])
            throws ParseException,
            IOException, com.vividsolutions.jts.io.ParseException {

        // Define the geometry to query. We'll find all points that fall inside
        // that geometry
        StringBuilder sb = new StringBuilder();
        sb.append("POLYGON (( ");
        for (int i = 0; i < values.length; i += 2) {
            sb.append(values[i + 1]);
            sb.append(" ");
            sb.append(values[i]);
            sb.append(",");
        }
        //repeat the first point to close the loop
        sb.append(values[1]);
        sb.append(" ");
        sb.append(values[0]);
        sb.append("))");
        String queryPolygonDefinition = sb.toString();
        Geometry queryPolygon = new WKTReader(
                JTSFactoryFinder.getGeometryFactory()).read(queryPolygonDefinition);

        SpatialQuery query = new SpatialQuery(
                queryPolygon);

        return query;
    }

    /**
     * polyQueryTime searches results in a polygon between two time periods
     * @param values
     * @return
     * @throws ParseException
     * @throws IOException
     * @throws com.vividsolutions.jts.io.ParseException
     */
    private SpatialQuery polyQuery(String values[], String timeStart, String timeEnd)
            throws ParseException,
            IOException, com.vividsolutions.jts.io.ParseException {
        Date startTime;
        Date endTime;
        if (timeStart == null && timeEnd == null) {
            return polyQuery(values);
        }
        try {
            startTime = new Date(Long.parseLong(timeStart));
        }
        catch (Exception e) {
            startTime = new Date(0);
        }

        try {
            endTime = new Date(Long.parseLong(timeEnd));
        }
        catch (Exception e) {
            endTime = new Date();
        }
        // Define the geometry to query. We'll find all points that fall inside
        // that geometry
        StringBuilder sb = new StringBuilder();
        sb.append("POLYGON (( ");
        for (int i = 0; i < values.length; i += 2) {
            sb.append(values[i + 1]);
            sb.append(" ");
            sb.append(values[i]);
            sb.append(",");
        }
        //repeat the first point to close the loop
        sb.append(values[1]);
        sb.append(" ");
        sb.append(values[0]);
        sb.append("))");
        String queryPolygonDefinition = sb.toString();
        Geometry queryPolygon = new WKTReader(
                JTSFactoryFinder.getGeometryFactory()).read(queryPolygonDefinition);

        SpatialQuery query = new SpatialTemporalQuery(
                startTime,
                endTime,
                queryPolygon);

        return query;
    }
}
