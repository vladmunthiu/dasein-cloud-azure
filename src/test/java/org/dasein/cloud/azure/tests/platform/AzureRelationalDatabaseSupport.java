package org.dasein.cloud.azure.tests.platform;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.platform.AzureSqlDatabaseSupport;
import org.dasein.cloud.platform.Database;

public class AzureRelationalDatabaseSupport extends AzureSqlDatabaseSupport {

	private Database database;
	
	public AzureRelationalDatabaseSupport(Azure provider, Database database) {
		super(provider);
		this.database = database;
	}
	
	@Override
	public Database getDatabase(final String providerDatabaseId) throws CloudException, InternalException {
		return this.database;
	}

}
