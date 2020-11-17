package net.silthus.regions;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import java.io.File;

import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RegionsPluginTests {

    private ServerMock server;

    @Before
    public void setUp() {
        server = MockBukkit.mock();
        MockBukkit.loadWith(RegionsPlugin.class, new File("build/tmp/spigotPluginYaml/plugin.yml"));
    }

    @Test
    public void shouldFirePlayerJoinEvent() {

        server.addPlayer();

        server.getPluginManager().assertEventFired(PlayerJoinEvent.class);
    }

    @After
    public void tearDown() {
        MockBukkit.unmock();
    }
}
