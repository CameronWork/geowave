package mil.nga.giat.geowave.core.ingest.socket;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import dk.tbsalling.aismessages.AISInputStreamReader;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.PositionReportClassAScheduled;
import dk.tbsalling.aismessages.nmea.exceptions.InvalidMessage;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import mil.nga.giat.geowave.core.geotime.GeometryUtils;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Created by cameron on 8/03/16.
 */
public class AISData
{

	private PositionReportClassAScheduled aisMessage;

	public AISData(
			String line )
			throws InvalidMessage {
		AISMessage temp = AISMessage.create(NMEAMessage.fromString(line));
		if (temp instanceof PositionReportClassAScheduled) {
			aisMessage = (PositionReportClassAScheduled) temp;
		}
		else {
			throw new InvalidMessage(
					"Not a position message");
		}
	}

	public long getTime() {
		return System.currentTimeMillis();
	}

	public float getLatitude() {
		return aisMessage.getLatitude();
	}

	public float getLongitude() {
		return aisMessage.getLongitude();
	}

	public int getHeading() {
		return aisMessage.getTrueHeading();
	}

	public int getShipID() {
		return aisMessage.getSourceMmsi().getMMSI();
	}
}
