package ch.cyberduck.core.davs;

/*
 *  Copyright (c) 2008 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.core.*;
import ch.cyberduck.core.dav.DAVPath;

/**
 * @version $Id$
 */
public class DAVSPath extends DAVPath {

    static {
        PathFactory.addFactory(Protocol.WEBDAV_SSL, new Factory());
    }

    private static class Factory extends PathFactory {
        protected Path create(Session session, String path, int type) {
            return new DAVSPath((DAVSSession) session, path, type);
        }

        protected Path create(Session session, String parent, String name, int type) {
            return new DAVSPath((DAVSSession) session, parent, name, type);
        }

        protected Path create(Session session, String path, Local file) {
            return new DAVSPath((DAVSSession) session, path, file);
        }

        protected <T> Path create(Session session, T dict) {
            return new DAVSPath((DAVSSession) session, dict);
        }
    }

    protected DAVSPath(DAVSSession s, String parent, String name, int type) {
        super(s, parent, name, type);
    }

    protected DAVSPath(DAVSSession s, String path, int type) {
        super(s, path, type);
    }

    protected DAVSPath(DAVSSession s, String parent, Local file) {
        super(s, parent, file);
    }

    protected <T> DAVSPath(DAVSSession s, T dict) {
        super(s, dict);
    }
}
