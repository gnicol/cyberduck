package ch.cyberduck.core.openstack;

/*
 * Copyright (c) 2013 David Kocher. All rights reserved.
 * http://cyberduck.ch/
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
 *
 * Bug fixes, suggestions and comments should be sent to:
 * feedback@cyberduck.ch
 */

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.PathNormalizer;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.exception.LoginCanceledException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import ch.iterate.openstack.swift.method.Authentication10UsernameKeyRequest;
import ch.iterate.openstack.swift.method.Authentication11UsernameKeyRequest;
import ch.iterate.openstack.swift.method.Authentication20AccessKeySecretKeyRequest;
import ch.iterate.openstack.swift.method.Authentication20RAXUsernameKeyRequest;
import ch.iterate.openstack.swift.method.Authentication20UsernamePasswordRequest;
import ch.iterate.openstack.swift.method.Authentication20UsernamePasswordTenantIdRequest;
import ch.iterate.openstack.swift.method.AuthenticationRequest;

/**
 * @version $Id$
 */
public class SwiftAuthenticationService {
    private static final Logger log = Logger.getLogger(SwiftAuthenticationService.class);

    /**
     * Default authentication version.
     */
    private String version;

    public SwiftAuthenticationService() {
        this(Preferences.instance().getProperty("openstack.authentication.context"));
    }

    public SwiftAuthenticationService(final String version) {
        this.version = version;
    }

    public Set<? extends AuthenticationRequest> getRequest(final Host host, final LoginCallback prompt) throws LoginCanceledException {
        final Credentials credentials = host.getCredentials();
        final StringBuilder url = new StringBuilder();
        url.append(host.getProtocol().getScheme().toString()).append("://");
        url.append(host.getHostname());
        if(!(host.getProtocol().getScheme().getPort() == host.getPort())) {
            url.append(":").append(host.getPort());
        }
        final String context;
        if(StringUtils.isBlank(host.getProtocol().getContext())) {
            // Default to 1.0
            context = PathNormalizer.normalize(version);
        }
        else {
            context = PathNormalizer.normalize(host.getProtocol().getContext());
        }
        // Custom authentication context
        url.append(context);
        if(host.getProtocol().getDefaultHostname().endsWith("identity.api.rackspacecloud.com")
                || host.getHostname().endsWith("identity.api.rackspacecloud.com")) {
            return Collections.singleton(new Authentication20RAXUsernameKeyRequest(
                    URI.create(url.toString()),
                    credentials.getUsername(), credentials.getPassword(), null)
            );
        }
        if(context.contains("1.0")) {
            return Collections.singleton(new Authentication10UsernameKeyRequest(URI.create(url.toString()),
                    credentials.getUsername(), credentials.getPassword()));
        }
        else if(context.contains("1.1")) {
            return Collections.singleton(new Authentication11UsernameKeyRequest(URI.create(url.toString()),
                    credentials.getUsername(), credentials.getPassword()));
        }
        else if(context.contains("2.0")) {
            // Prompt for tenant
            final String user;
            final String tenant;
            if(StringUtils.contains(credentials.getUsername(), ':')) {
                tenant = StringUtils.split(credentials.getUsername(), ':')[0];
                user = StringUtils.split(credentials.getUsername(), ':')[1];
            }
            else {
                user = credentials.getUsername();
                final Credentials tenantCredentials = new TenantCredentials();
                final LoginOptions options = new LoginOptions();
                options.password = false;
                prompt.prompt(new SwiftProtocol() {
                    @Override
                    public boolean validate(Credentials credentials, LoginOptions options) {
                        // Allow empty tenant
                        return true;
                    }
                }, tenantCredentials,
                        LocaleFactory.localizedString("Provide additional login credentials", "Credentials"),
                        LocaleFactory.localizedString("Tenant Name", "Mosso"), options);
                tenant = tenantCredentials.getUsername();
                if(StringUtils.isNotBlank(tenant)) {
                    // Save tenant in username
                    credentials.setUsername(String.format("%s:%s", tenant, credentials.getUsername()));
                }
            }
            final Set<AuthenticationRequest> options = new LinkedHashSet<AuthenticationRequest>();
            options.add(new Authentication20UsernamePasswordRequest(
                    URI.create(url.toString()),
                    user, credentials.getPassword(), tenant)
            );
            options.add(new Authentication20UsernamePasswordTenantIdRequest(
                    URI.create(url.toString()),
                    user, credentials.getPassword(), tenant)
            );
            options.add(new Authentication20AccessKeySecretKeyRequest(
                    URI.create(url.toString()),
                    user, credentials.getPassword(), tenant));
            return options;
        }
        else {
            log.warn(String.format("Unknown context version in %s. Default to v1 authentication.", context));
            // Default to 1.0
            return Collections.singleton(new Authentication10UsernameKeyRequest(URI.create(url.toString()),
                    credentials.getUsername(), credentials.getPassword()));
        }
    }
}