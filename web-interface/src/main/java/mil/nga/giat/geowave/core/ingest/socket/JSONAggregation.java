package mil.nga.giat.geowave.core.ingest.socket;

import com.vividsolutions.jts.geom.Point;
import mil.nga.giat.geowave.core.index.Mergeable;
import mil.nga.giat.geowave.core.store.query.aggregate.Aggregation;
import org.opengis.feature.simple.SimpleFeature;

import java.nio.charset.StandardCharsets;

/**
 * Created by cameron on 5/04/16.
 */
public class JSONAggregation<T> implements
        Aggregation<T> {
    private StringBuilder json = new StringBuilder();

    @Override
    public void aggregate(T entry) {
        if (entry instanceof SimpleFeature) {
            SimpleFeature sf = (SimpleFeature) entry;
            if (json.length() != 0) {
                json.append(",");
            }
            json.append("\"");
            json.append(sf.getAttribute("data"));
            json.append("\":");
            json.append("{\"name\":\"");
            json.append(sf.getAttribute("ShipID"));
            json.append("\",\"pointCoords\":\"");
            Object geometry = sf.getDefaultGeometry();
            if (geometry instanceof Point) {
                json.append(((Point) geometry).getCoordinate().y);
                json.append(" ");
                json.append(((Point) geometry).getCoordinate().x);
            } else {
                json.append(sf.getDefaultGeometry());
            }
            json.append("\",\"time\":\"");
            json.append(sf.getAttribute("TimeStamp"));
            json.append("\"}");
        }

    }

    @Override
    public void merge(Mergeable merge) {
        if ((merge != null) && (merge instanceof JSONAggregation)) {
            if (json.length() != 0) {
                json.append(",");
            }
            json.append(merge.toString());
        }
    }

    @Override
    public String toString() {
        return json.toString();
    }

    @Override
    public byte[] toBinary() {
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void fromBinary(byte[] bytes) {
        String str = new String(bytes, StandardCharsets.UTF_8);
        json.append(str);

    }
}
