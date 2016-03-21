package mil.nga.giat.geowave.core.ingest.socket;

import mil.nga.giat.geowave.core.ingest.IngestFormatOptionProvider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Created by cameron on 7/03/16.
 */
public class AISFilterOptionProvider implements
		IngestFormatOptionProvider
{
	@Override
	public void applyOptions(
			Options allOptions ) {
		allOptions.addOption(
				"AIS",
				true,
				"An AIS filter, only data matching this filter will be ingested");
	}

	@Override
	public void parseOptions(
			CommandLine commandLine ) {

	}
}
