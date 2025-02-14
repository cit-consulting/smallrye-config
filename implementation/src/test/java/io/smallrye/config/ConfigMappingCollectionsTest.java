package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMappingCollectionsTest.ServerCollectionsSet.Environment;
import io.smallrye.config.ConfigMappingCollectionsTest.ServerCollectionsSet.Environment.App;

public class ConfigMappingCollectionsTest {
    @ConfigMapping(prefix = "server")
    public interface ServerCollectionSimple {
        List<String> environments();
    }

    @Test
    void mappingCollectionsSimple() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionSimple.class, "server")
                .withSources(config("server.environments[0]", "dev",
                        "server.environments[1]", "qa",
                        "server.environments[2]", "prod"))
                .build();

        List<String> environments = config.getValues("server.environments", String.class);
        assertEquals(3, environments.size());
        assertEquals("dev", environments.get(0));
        assertEquals("qa", environments.get(1));
        assertEquals("prod", environments.get(2));

        ServerCollectionSimple configMapping = config.getConfigMapping(ServerCollectionSimple.class);
        assertEquals(3, configMapping.environments().size());
        assertEquals("dev", configMapping.environments().get(0));
        assertEquals("qa", configMapping.environments().get(1));
        assertEquals("prod", configMapping.environments().get(2));
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollections {
        List<Environment> environments();

        interface Environment {
            String name();

            List<App> apps();

            interface App {
                String name();

                List<String> services();

                Optional<List<String>> databases();
            }
        }
    }

    @Test
    void mappingCollections() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollections.class, "server")
                .withSources(config(
                        "server.environments[0].name", "dev",
                        "server.environments[0].apps[0].name", "rest",
                        "server.environments[0].apps[0].services", "a,b,c",
                        "server.environments[0].apps[0].databases", "pg,h2",
                        "server.environments[0].apps[1].name", "batch",
                        "server.environments[0].apps[1].services", "a,b,c",
                        "server.environments[1].name", "prod",
                        "server.environments[1].apps[0].name", "web",
                        "server.environments[1].apps[0].services", "a,b,c",
                        "server.environments[1].apps[1].name", "rest",
                        "server.environments[1].apps[1].services", "a,b,c",
                        "server.environments[1].apps[2].name", "batch",
                        "server.environments[1].apps[2].services", "a,b,c"))
                .build();
        ServerCollections server = config.getConfigMapping(ServerCollections.class);

        assertEquals(2, server.environments().size());
        assertEquals("dev", server.environments().get(0).name());
        assertEquals(Stream.of("a", "b", "c").collect(toList()), server.environments().get(0).apps().get(0).services());
        assertTrue(server.environments().get(0).apps().get(0).databases().isPresent());
        assertEquals(Stream.of("pg", "h2").collect(toList()), server.environments().get(0).apps().get(0).databases().get());
        assertEquals(2, server.environments().get(0).apps().size());
        assertEquals("rest", server.environments().get(0).apps().get(0).name());
        assertEquals("batch", server.environments().get(0).apps().get(1).name());
        assertEquals("prod", server.environments().get(1).name());
        assertEquals(3, server.environments().get(1).apps().size());
        assertEquals("web", server.environments().get(1).apps().get(0).name());
        assertEquals("rest", server.environments().get(1).apps().get(1).name());
        assertEquals("batch", server.environments().get(1).apps().get(2).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionName {
        @WithName("envs")
        List<Environment> environments();

        interface Environment {
            String name();

            @WithName("apps")
            List<App> applications();

            interface App {
                String name();
            }
        }
    }

    @Test
    void mappingCollectionsWithName() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionName.class, "server")
                .withSources(config("server.envs[0].name", "dev",
                        "server.envs[0].apps[0].name", "rest",
                        "server.envs[0].apps[1].name", "batch",
                        "server.envs[1].name", "prod",
                        "server.envs[1].apps[0].name", "web",
                        "server.envs[1].apps[1].name", "rest",
                        "server.envs[1].apps[2].name", "batch"))
                .build();
        ServerCollectionName server = config.getConfigMapping(ServerCollectionName.class);

        assertEquals(2, server.environments().size());
        assertEquals("dev", server.environments().get(0).name());
        assertEquals(2, server.environments().get(0).applications().size());
        assertEquals("rest", server.environments().get(0).applications().get(0).name());
        assertEquals("batch", server.environments().get(0).applications().get(1).name());
        assertEquals("prod", server.environments().get(1).name());
        assertEquals(3, server.environments().get(1).applications().size());
        assertEquals("web", server.environments().get(1).applications().get(0).name());
        assertEquals("rest", server.environments().get(1).applications().get(1).name());
        assertEquals("batch", server.environments().get(1).applications().get(2).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsDefaults {
        @WithParentName
        List<Environment> environments();

        interface Environment {
            @WithDefault("-1")
            int id();

            String name();

            @WithDefault("web,rest")
            List<String> apps();

            @WithDefault("80,443")
            List<Integer> ports();
        }
    }

    @Test
    void mappingCollectionsWithDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsDefaults.class, "server")
                .withSources(config("server[0].name", "dev"))
                .build();

        ServerCollectionsDefaults server = config.getConfigMapping(ServerCollectionsDefaults.class);

        assertEquals(1, server.environments().size());
        assertEquals(-1, server.environments().get(0).id());
        assertEquals("dev", server.environments().get(0).name());
        assertEquals(Stream.of("web", "rest").collect(toList()), server.environments().get(0).apps());

        assertEquals(2, server.environments().get(0).ports().size());
        assertEquals(Stream.of(80, 443).collect(toList()), server.environments().get(0).ports());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsOptionals {
        @WithName("env")
        List<Environment> environments();

        Optional<Environment> notRequired();

        @WithName("req")
        Optional<Environment> required();

        Optional<List<Environment>> notRequiredEnvs();

        @WithName("reqEnvs")
        Optional<List<Environment>> requiredEnvs();

        interface Environment {
            String name();

            Optional<List<String>> notRequiredServices();

            Optional<List<String>> requiredServices();

            Optional<List<String>> indexed();

            Optional<App> app();

            interface App {
                Optional<String> alias();
            }
        }
    }

    @Test
    void mappingCollectionsOptionals() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsOptionals.class, "server")
                .withSources(config(
                        "server.env[0].name", "dev",
                        "server.env[1].name", "prod",
                        "server.env[0].app.alias", "rest",
                        "server.req.name", "dev",
                        "server.req.required-services", "rest,db",
                        "server.req.indexed[0]", "rest",
                        "server.req.indexed[1]", "db",
                        "server.reqEnvs[0].name", "dev",
                        "server.reqEnvs[1].name", "prod"))
                .build();

        ServerCollectionsOptionals server = config.getConfigMapping(ServerCollectionsOptionals.class);

        assertFalse(server.environments().isEmpty());
        assertEquals("dev", server.environments().get(0).name());
        assertTrue(server.environments().get(0).app().isPresent());
        assertTrue(server.environments().get(0).app().get().alias().isPresent());
        assertEquals("rest", server.environments().get(0).app().get().alias().get());
        assertFalse(server.notRequired().isPresent());
        assertTrue(server.required().isPresent());
        assertEquals("dev", server.required().get().name());
        assertFalse(server.required().get().notRequiredServices().isPresent());
        assertTrue(server.required().get().requiredServices().isPresent());
        assertEquals("rest", server.required().get().requiredServices().get().get(0));
        assertEquals("db", server.required().get().requiredServices().get().get(1));
        assertTrue(server.required().get().indexed().isPresent());
        assertEquals("rest", server.required().get().indexed().get().get(0));
        assertEquals("db", server.required().get().indexed().get().get(1));

        assertFalse(server.notRequiredEnvs().isPresent());
        assertTrue(server.requiredEnvs().isPresent());
        assertEquals("dev", server.requiredEnvs().get().get(0).name());
        assertEquals("prod", server.requiredEnvs().get().get(1).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsConverters {
        List<Enviroment> envs();

        interface Enviroment {
            @WithConverter(HostConverter.class)
            List<Host> hosts();
        }
    }

    static class Host {
        final String name;

        Host(final String name) {
            this.name = name;
        }
    }

    static class HostConverter implements Converter<Host> {
        public HostConverter() {
        }

        @Override
        public Host convert(final String value) throws IllegalArgumentException, NullPointerException {
            return new Host(value);
        }
    }

    @Test
    void mappingCollectionsConverters() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsConverters.class, "server")
                .withSources(config("server.envs[0].hosts[0]", "localhost",
                        "server.envs[0].hosts[1]", "127.0.0.1"))
                .build();

        ServerCollectionsConverters server = config.getConfigMapping(ServerCollectionsConverters.class);

        assertEquals(1, server.envs().size());
        assertEquals(2, server.envs().get(0).hosts().size());
        assertEquals("localhost", server.envs().get(0).hosts().get(0).name);
        assertEquals("127.0.0.1", server.envs().get(0).hosts().get(1).name);
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsSet {
        Set<Environment> environments();

        interface Environment {
            String name();

            Set<App> apps();

            interface App {
                String name();

                Set<String> services();

                Optional<Set<String>> databases();
            }
        }
    }

    @Test
    void mappingCollectionsSet() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsSet.class, "server")
                .withSources(config(
                        "server.environments[0].name", "dev",
                        "server.environments[0].apps[0].name", "rest",
                        "server.environments[0].apps[0].services", "a,b,c",
                        "server.environments[0].apps[0].databases", "pg,h2",
                        "server.environments[0].apps[1].name", "batch",
                        "server.environments[0].apps[1].services", "a,b,c",
                        "server.environments[1].name", "prod",
                        "server.environments[1].apps[0].name", "web",
                        "server.environments[1].apps[0].services", "a,b,c",
                        "server.environments[1].apps[1].name", "rest",
                        "server.environments[1].apps[1].services", "a,b,c",
                        "server.environments[1].apps[2].name", "batch",
                        "server.environments[1].apps[2].services", "a,b,c"))
                .build();
        ServerCollectionsSet server = config.getConfigMapping(ServerCollectionsSet.class);

        assertEquals(2, server.environments().size());

        Optional<Environment> dev = server.environments().stream().filter(environment -> environment.name().equals("dev"))
                .findFirst();
        assertTrue(dev.isPresent());
        dev.ifPresent(environment -> {
            assertEquals(2, environment.apps().size());
            Optional<App> rest = environment.apps().stream().filter(app -> app.name().equals("rest")).findFirst();
            assertTrue(rest.isPresent());
            rest.ifPresent(app -> {
                assertTrue(Stream.of("a", "b", "c").collect(toSet()).containsAll(app.services()));
                assertTrue(app.databases().isPresent());
                assertTrue(Stream.of("pg", "h2").collect(toSet()).containsAll(app.databases().get()));
            });
        });
    }

    @ConfigMapping(prefix = "server")
    public interface ServerOptionalGroup {
        Optional<Environment> environment();

        interface Environment {
            List<String> services();

            List<App> apps();

            interface App {
                String name();
            }
        }
    }

    @Test
    void mappingOptionalGroupWithCollection() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerOptionalGroup.class, "server")
                .withSources(config(
                        "server.environment.services[0]", "rest",
                        "server.environment.services[1]", "batch",
                        "server.environment.apps[0].name", "a"))
                .build();

        ServerOptionalGroup server = config.getConfigMapping(ServerOptionalGroup.class);
        assertTrue(server.environment().isPresent());
        assertEquals("rest", server.environment().get().services().get(0));
        assertEquals("batch", server.environment().get().services().get(1));
        assertEquals("a", server.environment().get().apps().get(0).name());

        config = new SmallRyeConfigBuilder()
                .withMapping(ServerOptionalGroup.class, "server")
                .withSources(config(
                        "server.environment.services", "rest,batch",
                        "server.environment.apps[0].name", "a"))
                .build();

        server = config.getConfigMapping(ServerOptionalGroup.class);
        assertTrue(server.environment().isPresent());
        assertEquals("rest", server.environment().get().services().get(0));
        assertEquals("batch", server.environment().get().services().get(1));
        assertEquals("a", server.environment().get().apps().get(0).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerSingleCollection {
        @WithParentName
        List<Server> origins();

        interface Server {
            String host();

            int port();
        }
    }

    @Test
    void mappingSingleCollection() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerSingleCollection.class, "server")
                .withSources(config(
                        "server[0].host", "localhost",
                        "server[0].port", "8080",
                        "server[1].host", "my-server",
                        "server[1].port", "80"))
                .build();

        ServerSingleCollection server = config.getConfigMapping(ServerSingleCollection.class);
        assertEquals(2, server.origins().size());
        assertEquals("localhost", server.origins().get(0).host());
        assertEquals(8080, server.origins().get(0).port());
        assertEquals("my-server", server.origins().get(1).host());
        assertEquals(80, server.origins().get(1).port());
    }

    @Test
    void mappingCollectionProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerSingleCollection.class, "server")
                .withSources(config(
                        "server[0].host", "localhost",
                        "server[0].port", "8080",
                        "server[1].host", "my-server",
                        "server[1].port", "80",
                        "%test.server[0].host", "localhost-test",
                        "%test.server[0].port", "8081"))
                .withProfile("test")
                .build();

        ServerSingleCollection mapping = config.getConfigMapping(ServerSingleCollection.class);
        assertEquals(2, mapping.origins().size());
        assertEquals("localhost-test", mapping.origins().get(0).host());
        assertEquals(8081, mapping.origins().get(0).port());
        assertEquals("my-server", mapping.origins().get(1).host());
        assertEquals(80, mapping.origins().get(1).port());
    }

    @ConfigMapping(prefix = "servers")
    public interface ServerCollectionMap {
        @WithParentName
        List<Map<String, String>> servers();

        List<Map<String, Server>> moreServers();

        Optional<List<Map<String, String>>> optional();

        Optional<List<Map<String, String>>> present();

        interface Server {
            String host();

            int port();
        }
    }

    @Test
    void mappingCollectionsMap() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionMap.class, "servers")
                .withSources(config(
                        "servers[0].localhost", "localhost",
                        "servers[1].konoha", "konoha",
                        "servers.more-servers[0].local.host", "localhost",
                        "servers.more-servers[0].local.port", "8080",
                        "servers.more-servers[1].kon.host", "konoha",
                        "servers.more-servers[1].kon.port", "80",
                        "servers.present[0].localhost", "localhost"))
                .build();

        ServerCollectionMap mapping = config.getConfigMapping(ServerCollectionMap.class);
        assertEquals("localhost", mapping.servers().get(0).get("localhost"));
        assertEquals("konoha", mapping.servers().get(1).get("konoha"));

        assertEquals("localhost", mapping.moreServers().get(0).get("local").host());
        assertEquals(8080, mapping.moreServers().get(0).get("local").port());
        assertEquals("konoha", mapping.moreServers().get(1).get("kon").host());
        assertEquals(80, mapping.moreServers().get(1).get("kon").port());

        assertFalse(mapping.optional().isPresent());

        assertTrue(mapping.present().isPresent());
        assertEquals("localhost", mapping.present().get().get(0).get("localhost"));
    }
}
