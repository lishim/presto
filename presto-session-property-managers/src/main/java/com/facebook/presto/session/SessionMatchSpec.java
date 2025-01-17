/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.session;

import com.facebook.presto.common.PrestoVersion;
import com.facebook.presto.spi.resourceGroups.ResourceGroupId;
import com.facebook.presto.spi.session.SessionConfigurationContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class SessionMatchSpec
{
    private final Optional<Pattern> userRegex;
    private final Optional<Pattern> sourceRegex;
    private final Set<String> clientTags;
    private final Optional<String> queryType;
    private final Optional<Pattern> clientInfoRegex;
    private final Optional<Pattern> resourceGroupRegex;
    private final Optional<Boolean> overrideSessionProperties;
    private final Map<String, String> sessionProperties;
    private final Optional<PrestoVersion> minVersion;
    private final Optional<PrestoVersion> maxVersion;

    @JsonCreator
    public SessionMatchSpec(
            @JsonProperty("user") Optional<Pattern> userRegex,
            @JsonProperty("source") Optional<Pattern> sourceRegex,
            @JsonProperty("clientTags") Optional<List<String>> clientTags,
            @JsonProperty("queryType") Optional<String> queryType,
            @JsonProperty("group") Optional<Pattern> resourceGroupRegex,
            @JsonProperty("clientInfo") Optional<Pattern> clientInfoRegex,
            @JsonProperty("overrideSessionProperties") Optional<Boolean> overrideSessionProperties,
            @JsonProperty("sessionProperties") Map<String, String> sessionProperties,
            @JsonProperty("minVersion") Optional<String> minVersion,
            @JsonProperty("maxVersion") Optional<String> maxVersion)
    {
        this.userRegex = requireNonNull(userRegex, "userRegex is null");
        this.sourceRegex = requireNonNull(sourceRegex, "sourceRegex is null");
        requireNonNull(minVersion, "Min version is null");
        requireNonNull(maxVersion, "Max version is null");
        this.minVersion = minVersion.map(PrestoVersion::new);
        this.maxVersion = maxVersion.map(PrestoVersion::new);
        requireNonNull(clientTags, "clientTags is null");
        this.clientTags = ImmutableSet.copyOf(clientTags.orElse(ImmutableList.of()));
        this.queryType = requireNonNull(queryType, "queryType is null");
        this.resourceGroupRegex = requireNonNull(resourceGroupRegex, "resourceGroupRegex is null");
        this.clientInfoRegex = requireNonNull(clientInfoRegex, "clientInfoRegex is null");
        this.overrideSessionProperties = requireNonNull(overrideSessionProperties, "overrideSessionProperties is null");
        requireNonNull(sessionProperties, "sessionProperties is null");
        this.sessionProperties = ImmutableMap.copyOf(sessionProperties);
    }

    public Map<String, String> match(SessionConfigurationContext context, PrestoVersion coordinatorVersion)
    {
        if (userRegex.isPresent() && !userRegex.get().matcher(context.getUser()).matches()) {
            return ImmutableMap.of();
        }
        if (sourceRegex.isPresent()) {
            String source = context.getSource().orElse("");
            if (!sourceRegex.get().matcher(source).matches()) {
                return ImmutableMap.of();
            }
        }
        if (!clientTags.isEmpty() && !context.getClientTags().containsAll(clientTags)) {
            return ImmutableMap.of();
        }

        if (queryType.isPresent()) {
            String contextQueryType = context.getQueryType().orElse("");
            if (!queryType.get().equalsIgnoreCase(contextQueryType)) {
                return ImmutableMap.of();
            }
        }

        if (clientInfoRegex.isPresent()) {
            String clientInfo = context.getClientInfo().orElse("");
            if (!clientInfoRegex.get().matcher(clientInfo).matches()) {
                return ImmutableMap.of();
            }
        }

        if (resourceGroupRegex.isPresent()) {
            String resourceGroupId = context.getResourceGroupId().map(ResourceGroupId::toString).orElse("");
            if (!resourceGroupRegex.get().matcher(resourceGroupId).matches()) {
                return ImmutableMap.of();
            }
        }

        if (maxVersion.isPresent() || minVersion.isPresent()) {
            boolean validVersion = true;
            if (maxVersion.isPresent()) {
                validVersion = coordinatorVersion.lessThanOrEqualTo(maxVersion.get());
            }
            if (minVersion.isPresent()) {
                validVersion = validVersion && coordinatorVersion.greaterThanOrEqualTo(minVersion.get());
            }
            if (!validVersion) {
                return ImmutableMap.of();
            }
        }
        return sessionProperties;
    }

    @JsonProperty("user")
    public Optional<Pattern> getUserRegex()
    {
        return userRegex;
    }

    @JsonProperty("source")
    public Optional<Pattern> getSourceRegex()
    {
        return sourceRegex;
    }

    @JsonProperty
    public Set<String> getClientTags()
    {
        return clientTags;
    }

    @JsonProperty
    public Optional<String> getQueryType()
    {
        return queryType;
    }

    @JsonProperty("group")
    public Optional<Pattern> getResourceGroupRegex()
    {
        return resourceGroupRegex;
    }

    @JsonProperty
    public Optional<Pattern> getClientInfoRegex()
    {
        return clientInfoRegex;
    }

    @JsonProperty
    public Optional<Boolean> getOverrideSessionProperties()
    {
        return overrideSessionProperties;
    }

    @JsonProperty
    public Map<String, String> getSessionProperties()
    {
        return sessionProperties;
    }
}
