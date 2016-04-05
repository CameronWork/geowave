package mil.nga.giat.geowave.core.ingest.socket;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import dk.tbsalling.aismessages.nmea.exceptions.InvalidMessage;
import dk.tbsalling.aismessages.nmea.exceptions.UnsupportedMessageType;
import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter;
import mil.nga.giat.geowave.core.cli.CommandLineResult;
import mil.nga.giat.geowave.core.cli.DataStoreCommandLineOptions;
import mil.nga.giat.geowave.core.geotime.GeometryUtils;
import mil.nga.giat.geowave.core.geotime.ingest.SpatialDimensionalityTypeProvider;
import mil.nga.giat.geowave.core.ingest.AbstractIngestCommandLineDriver;
import mil.nga.giat.geowave.core.ingest.IngestCommandLineOptions;
import mil.nga.giat.geowave.core.ingest.IngestFormatPluginProviderSpi;
import mil.nga.giat.geowave.core.store.IndexWriter;
import mil.nga.giat.geowave.core.store.index.PrimaryIndex;
import mil.nga.giat.geowave.core.store.memory.DataStoreUtils;
import mil.nga.giat.geowave.datastore.accumulo.AccumuloDataStore;
import mil.nga.giat.geowave.datastore.accumulo.BasicAccumuloOperations;
import mil.nga.giat.geowave.datastore.accumulo.index.secondary.AccumuloSecondaryIndexDataStore;
import mil.nga.giat.geowave.datastore.accumulo.metadata.AccumuloAdapterStore;
import mil.nga.giat.geowave.datastore.accumulo.metadata.AccumuloDataStatisticsStore;
import mil.nga.giat.geowave.datastore.accumulo.metadata.AccumuloIndexStore;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by cameron on 3/03/16.
 */
class SocketIngestPlugin extends
        AbstractIngestCommandLineDriver {
    private DataStoreCommandLineOptions dataStoreOptions;
    private IngestCommandLineOptions ingestOptions;
    private final static Logger LOGGER = Logger.getLogger(SocketIngestPlugin.class);

    private String serverAddr = null;
    private int serverPort = 0;
    private String zookeepers = "localhost:2181";
    private String accumuloInstance = "geowave";
    private String accumuloUser = "root";
    private String accumuloPass = "password";
    private AccumuloDataStore geowaveDataStore;

    private IndexWriter indexWriter = null;
    private FeatureDataAdapter adapter = null;
    private SimpleFeatureBuilder pointBuilder = null;

    private final Object lock = new Object();


    SocketIngestPlugin(
            final String operation) {
        super(
                operation);
    }

    @Override
    protected void parseOptionsInternal(
            final Options options,
            CommandLine commandLine)
            throws ParseException {
        final CommandLineResult<DataStoreCommandLineOptions> dataStoreOptionsResult = DataStoreCommandLineOptions.parseOptions(
                options,
                commandLine);
        dataStoreOptions = dataStoreOptionsResult.getResult();
        if (commandLine.getArgs().length == 6) {
            serverAddr = commandLine.getArgs()[0];
            serverPort = Integer.parseInt(commandLine.getArgs()[1]);
            zookeepers = commandLine.getArgs()[2];
            accumuloInstance = commandLine.getArgs()[3];
            accumuloUser = commandLine.getArgs()[4];
            accumuloPass = commandLine.getArgs()[5];

        }
        if (dataStoreOptionsResult.isCommandLineChange()) {
            commandLine = dataStoreOptionsResult.getCommandLine();
        }
        ingestOptions = IngestCommandLineOptions.parseOptions(commandLine);
    }

    @Override
    protected void applyOptionsInternal(
            final Options allOptions) {
        DataStoreCommandLineOptions.applyOptions(allOptions);
        IngestCommandLineOptions.applyOptions(allOptions);
    }

    @Override
    protected boolean runInternal(
            String[] args,
            List<IngestFormatPluginProviderSpi<?, ?>> pluginProviders) {

        if (serverAddr == null) {
            LOGGER.trace("Server address not set, add it to the commandline args");
            return false;
        }
        while (true) {
            try {
                // ServerSocket serverSocket = new ServerSocket(port);
                // LOGGER.trace("Opened listen socket on port " + port);

                Socket socket = new Socket(
                        serverAddr,
                        serverPort);
                try {
                    init();
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()));
                    while (!socket.isClosed()) {
                        String line = in.readLine();
                        processLine(line);
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    LOGGER.trace(e.getLocalizedMessage());

                }
            } catch (ConnectException f) {
                LOGGER.trace("Connection Failed: " + f.getLocalizedMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void processLine(
            final String line) {
        LOGGER.trace(line);
        try {
            AISData point = new AISData(
                    line);
            pointBuilder.set(
                    "geometry",
                    GeometryUtils.GEOMETRY_FACTORY.createPoint(new Coordinate(
                            point.getLongitude(),
                            point.getLatitude())));
            pointBuilder.set(
                    "TimeStamp",
                    new Date(point.getTime()));
            pointBuilder.set(
                    "Latitude",
                    point.getLatitude());
            pointBuilder.set(
                    "Longitude",
                    point.getLongitude());
            pointBuilder.set(
                    "Heading",
                    point.getHeading());
            pointBuilder.set(
                    "ShipID",
                    point.getShipID());
            pointBuilder.set(
                    "data",
                    line);
            // Note since trajectoryID and comment are marked as nillable we
            // don't need to set them (they default ot null).

            SimpleFeature sft = pointBuilder.buildFeature(null);
            synchronized (lock) {
                indexWriter.write(
                        adapter,
                        sft);
            }
        } catch (InvalidMessage | UnsupportedMessageType e) {
            LOGGER.trace(e.getLocalizedMessage());
        }
    }

    private void init() {
        try {
            String geowaveNamespace = "aisdata";
            final BasicAccumuloOperations bao = new BasicAccumuloOperations(
                    zookeepers,
                    accumuloInstance,
                    accumuloUser,
                    accumuloPass,
                    geowaveNamespace);
            geowaveDataStore = new AccumuloDataStore(
                    new AccumuloIndexStore(
                            bao),
                    new AccumuloAdapterStore(
                            bao),
                    new AccumuloDataStatisticsStore(
                            bao),
                    new AccumuloSecondaryIndexDataStore(
                            bao),
                    bao);
            final PrimaryIndex index = new SpatialDimensionalityTypeProvider().createPrimaryIndex();

            final SimpleFeatureType point = createPointFeatureType();
            indexWriter = geowaveDataStore.createIndexWriter(
                    index,
                    DataStoreUtils.DEFAULT_VISIBILITY);
            pointBuilder = new SimpleFeatureBuilder(
                    point);

            // This is an adapter, that is needed to describe how to persist
            // the
            // data type passed
            adapter = new FeatureDataAdapter(
                    point);

            Timer time = new Timer();
            FlushTask flushTask = new FlushTask();
            time.schedule(flushTask, 0L, 10000L);

        } catch (AccumuloSecurityException | AccumuloException e) {
            e.printStackTrace();
        }
    }

    private class FlushTask extends TimerTask {
        @Override
        public void run() {
            synchronized (lock) {
                //TODO: should just call indexWriter.flush() but this doesnt appear to work
                try {
                    indexWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final PrimaryIndex index = new SpatialDimensionalityTypeProvider().createPrimaryIndex();
                indexWriter = geowaveDataStore.createIndexWriter(
                        index,
                        DataStoreUtils.DEFAULT_VISIBILITY);
                System.out.println("Successfully flushed the index's");
            }
        }
    }

    private static SimpleFeatureType createPointFeatureType() {

        final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        final AttributeTypeBuilder ab = new AttributeTypeBuilder();

        // Names should be unique (at least for a given GeoWave namespace) -
        // think about names in the same sense as a full classname
        // The value you set here will also persist through discovery - so when
        // people are looking at a dataset they will see the
        // type names associated with the data.
        builder.setName("AIS_FEATURE");

        // The data is persisted in a sparse format, so if data is nullable it
        // will not take up any space if no values are persisted.
        // Data which is included in the primary index (in this example
        // lattitude/longtiude) can not be null
        // Calling out latitude an longitude separately is not strictly needed,
        // as the geometry contains that information. But it's
        // convienent in many use cases to get a text representation without
        // having to handle geometries.
        builder.add(ab.binding(
                Geometry.class).nillable(
                true).buildDescriptor(
                "geometry"));
        builder.add(ab.binding(
                Date.class).nillable(
                true).buildDescriptor(
                "TimeStamp"));
        builder.add(ab.binding(
                Double.class).nillable(
                false).buildDescriptor(
                "Latitude"));
        builder.add(ab.binding(
                Double.class).nillable(
                false).buildDescriptor(
                "Longitude"));
        builder.add(ab.binding(
                Double.class).nillable(
                false).buildDescriptor(
                "Heading"));
        builder.add(ab.binding(
                Double.class).nillable(
                false).buildDescriptor(
                "ShipID"));
        builder.add(ab.binding(
                String.class).nillable(
                false).buildDescriptor(
                "data"));

        return builder.buildFeatureType();
    }
}
