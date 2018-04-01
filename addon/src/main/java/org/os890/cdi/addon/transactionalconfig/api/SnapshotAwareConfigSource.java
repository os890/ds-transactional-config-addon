package org.os890.cdi.addon.transactionalconfig.api;

import org.apache.deltaspike.core.spi.config.ConfigSource;

public interface SnapshotAwareConfigSource extends ConfigSource {
    long getTimestampOfLatestChange();
}
