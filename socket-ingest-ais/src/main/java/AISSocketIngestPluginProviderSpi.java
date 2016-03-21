import mil.nga.giat.geowave.core.ingest.CompoundIngestFormatOptionProvider;
import mil.nga.giat.geowave.core.ingest.IngestFormatOptionProvider;
import mil.nga.giat.geowave.core.ingest.IngestFormatPluginProviderSpi;
import mil.nga.giat.geowave.core.ingest.avro.AvroFormatPlugin;
import mil.nga.giat.geowave.core.ingest.hdfs.mapreduce.IngestFromHdfsPlugin;
import mil.nga.giat.geowave.core.ingest.local.LocalFileIngestPlugin;
import mil.nga.giat.geowave.core.ingest.socket.AISFilterOptionProvider;
import mil.nga.giat.geowave.format.geotools.vector.retyping.date.DateFieldOptionProvider;

/**
 * Created by cameron on 7/03/16.
 */
public class AISSocketIngestPluginProviderSpi implements
		IngestFormatPluginProviderSpi
{

	protected final AISFilterOptionProvider aisFilterOptionProvider = new AISFilterOptionProvider();
	protected final DateFieldOptionProvider dateFieldOptionProvider = new DateFieldOptionProvider();

	@Override
	public IngestFromHdfsPlugin getIngestFromHdfsPlugin()
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public LocalFileIngestPlugin getLocalFileIngestPlugin()
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getIngestFormatName() {
		return "socket";
	}

	@Override
	public IngestFormatOptionProvider getIngestFormatOptionProvider() {
		return new CompoundIngestFormatOptionProvider().add(
				aisFilterOptionProvider).add(
				dateFieldOptionProvider);
	}

	@Override
	public String getIngestFormatDescription() {
		return "Ingest AIS data from a socket";
	}

	@Override
	public AvroFormatPlugin getAvroFormatPlugin()
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
}
