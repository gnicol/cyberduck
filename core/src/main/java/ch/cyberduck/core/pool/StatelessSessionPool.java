/*
 * Copyright (c) 2002-2016 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package ch.cyberduck.core.pool;

import ch.cyberduck.core.ConnectionService;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.TranscriptListener;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.threading.BackgroundActionState;
import ch.cyberduck.core.threading.CancelCallback;
import ch.cyberduck.core.threading.DefaultFailureDiagnostics;
import ch.cyberduck.core.threading.FailureDiagnostics;
import ch.cyberduck.core.vault.VaultRegistry;

import org.apache.log4j.Logger;

import java.util.concurrent.Semaphore;

public class StatelessSessionPool implements SessionPool {
    private static final Logger log = Logger.getLogger(StatelessSessionPool.class);

    private final FailureDiagnostics<BackgroundException> diagnostics = new DefaultFailureDiagnostics();
    private final ConnectionService connect;
    private final TranscriptListener transcript;
    private final Session<?> session;
    private final PathCache cache;
    private final VaultRegistry registry;

    private final Object lock = new Object();

    /**
     * Fair queue
     */
    private Semaphore semaphore = new Semaphore(
            PreferencesFactory.get().getInteger("connection.pool.maxtotal"), true);

    public StatelessSessionPool(final ConnectionService connect, final Session<?> session, final PathCache cache,
                                final TranscriptListener transcript, final VaultRegistry registry) {
        this.connect = connect;
        this.transcript = transcript;
        this.session = session.withRegistry(registry);
        this.registry = registry;
        this.cache = cache;
    }

    @Override
    public SessionPool withMinIdle(final int count) {
        return this;
    }

    @Override
    public SessionPool withMaxIdle(final int count) {
        return this;
    }

    @Override
    public SessionPool withMaxTotal(final int count) {
        semaphore.drainPermits();
        semaphore = new Semaphore(count, true);
        return this;
    }

    @Override
    public Session<?> borrow(final BackgroundActionState callback) throws BackgroundException {
        synchronized(lock) {
            semaphore.acquireUninterruptibly();
            connect.check(session.withListener(transcript), cache, new CancelCallback() {
                @Override
                public void verify() throws ConnectionCanceledException {
                    if(callback.isCanceled()) {
                        throw new ConnectionCanceledException();
                    }
                }
            });
            return session;
        }
    }

    @Override
    public void release(final Session<?> conn, final BackgroundException failure) {
        synchronized(lock) {
            try {
                if(failure != null && diagnostics.determine(failure) == FailureDiagnostics.Type.network) {
                    connect.close(conn);
                }
            }
            finally {
                semaphore.release();
            }
        }
    }

    @Override
    public void evict() {
        synchronized(lock) {
            try {
                session.close();
            }
            catch(BackgroundException e) {
                log.warn(String.format("Ignore failure closing connection. %s", e.getMessage()));
            }
            finally {
                session.removeListener(transcript);
                registry.clear();
            }
        }
    }

    @Override
    public void shutdown() {
        synchronized(lock) {
            try {
                session.close();
            }
            catch(BackgroundException e) {
                log.warn(String.format("Failure closing session. %s", e.getMessage()));
            }
            finally {
                registry.clear();
            }
        }
    }

    @Override
    public Session.State getState() {
        return session.getState();
    }

    @Override
    public <T> T getFeature(final Class<T> type) {
        return session.getFeature(type);
    }

    @Override
    public Host getHost() {
        return session.getHost();
    }

    @Override
    public PathCache getCache() {
        return cache;
    }

    @Override
    public VaultRegistry getVault() {
        return registry;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SingleSessionPool{");
        sb.append("session=").append(session);
        sb.append('}');
        return sb.toString();
    }
}
