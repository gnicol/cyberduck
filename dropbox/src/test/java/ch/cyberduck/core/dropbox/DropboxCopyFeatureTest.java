package ch.cyberduck.core.dropbox;

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

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledProgressListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginConnectionService;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.shared.DefaultHomeFinderService;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.test.IntegrationTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(IntegrationTest.class)
public class DropboxCopyFeatureTest {

    @Test
    public void testCopyFile() throws Exception {
        final DropboxSession session = new DropboxSession(new Host(new DropboxProtocol(), new DropboxProtocol().getDefaultHostname()),
                new DisabledX509TrustManager(), new DefaultX509KeyManager());
        new LoginConnectionService(new DisabledLoginCallback() {
            @Override
            public void prompt(final Host bookmark, final Credentials credentials, final String title, final String reason, final LoginOptions options) throws LoginCanceledException {
                fail(reason);
            }
        }, new DisabledHostKeyCallback(),
                new DisabledPasswordStore() {
                    @Override
                    public String getPassword(Scheme scheme, int port, String hostname, String user) {
                        if(user.equals("Dropbox OAuth2 Access Token")) {
                            return System.getProperties().getProperty("dropbox.accesstoken");
                        }
                        if(user.equals("Dropbox OAuth2 Refresh Token")) {
                            return System.getProperties().getProperty("dropbox.refreshtoken");
                        }
                        return null;
                    }
                }, new DisabledProgressListener())
                .connect(session, PathCache.empty(), new DisabledCancelCallback());
        final Path file = new Path(new DefaultHomeFinderService(session).find(), new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
        final Path target = new Path(new DefaultHomeFinderService(session).find(), new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
        new DropboxTouchFeature(session).touch(file, new TransferStatus());
        assertTrue(new DropboxFindFeature(session).find(file));
        new DropboxCopyFeature(session).copy(file, target, new TransferStatus());
        assertTrue(new DropboxFindFeature(session).find(file));
        assertTrue(new DropboxFindFeature(session).find(target));
        new DropboxDeleteFeature(session).delete(Collections.singletonList(target), new DisabledLoginCallback(), new Delete.DisabledCallback());
        session.close();
    }

    @Test
    public void testCopyDirectory() throws Exception {
        final DropboxSession session = new DropboxSession(new Host(new DropboxProtocol(), new DropboxProtocol().getDefaultHostname()),
                new DisabledX509TrustManager(), new DefaultX509KeyManager());
        new LoginConnectionService(new DisabledLoginCallback() {
            @Override
            public void prompt(final Host bookmark, final Credentials credentials, final String title, final String reason, final LoginOptions options) throws LoginCanceledException {
                fail(reason);
            }
        }, new DisabledHostKeyCallback(),
                new DisabledPasswordStore() {
                    @Override
                    public String getPassword(Scheme scheme, int port, String hostname, String user) {
                        if(user.equals("Dropbox OAuth2 Access Token")) {
                            return System.getProperties().getProperty("dropbox.accesstoken");
                        }
                        if(user.equals("Dropbox OAuth2 Refresh Token")) {
                            return System.getProperties().getProperty("dropbox.refreshtoken");
                        }
                        return null;
                    }
                }, new DisabledProgressListener())
                .connect(session, PathCache.empty(), new DisabledCancelCallback());
        final Path directory = new Path(new DefaultHomeFinderService(session).find(), new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
        final String name = new AlphanumericRandomStringService().random();
        final Path file = new Path(directory, name, EnumSet.of(Path.Type.file));
        new DropboxDirectoryFeature(session).mkdir(directory, null, new TransferStatus());
        new DropboxTouchFeature(session).touch(file, new TransferStatus());
        assertTrue(new DropboxFindFeature(session).find(file));
        final Path copy = new Path(new DefaultHomeFinderService(session).find(), new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
        new DropboxCopyFeature(session).copy(directory, copy, new TransferStatus());
        assertTrue(new DropboxFindFeature(session).find(file));
        assertTrue(new DropboxFindFeature(session).find(copy));
        assertTrue(new DropboxFindFeature(session).find(new Path(copy, name, EnumSet.of(Path.Type.file))));
        new DropboxDeleteFeature(session).delete(Arrays.asList(directory, copy), new DisabledLoginCallback(), new Delete.DisabledCallback());
        session.close();
    }
}