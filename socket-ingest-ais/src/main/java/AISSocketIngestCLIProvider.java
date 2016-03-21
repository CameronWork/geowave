import mil.nga.giat.geowave.core.cli.CLIOperation;
import mil.nga.giat.geowave.core.cli.CLIOperationCategory;
import mil.nga.giat.geowave.core.cli.CLIOperationProviderSpi;
import mil.nga.giat.geowave.core.ingest.IngestOperationCategory;

/**
 * Created by cameron on 7/03/16.
 */
public class AISSocketIngestCLIProvider implements
		CLIOperationProviderSpi
{

	private static final CLIOperationCategory CATEGORY = new IngestOperationCategory();

	@Override
	public CLIOperation[] getOperations() {
		return new CLIOperation[] {
			new CLIOperation(
					"socket",
					"Ingest AIS from Socket",
					new SocketIngestPlugin(
							"socket"))
		};
	}

	@Override
	public CLIOperationCategory getCategory() {
		return CATEGORY;
	}
}
