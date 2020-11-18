package net.silthus.regions.entities;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import net.silthus.regions.Constants;
import net.silthus.regions.RegionsPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RegionPlayerTest {

    private ServerMock server;
    private RegionsPlugin plugin;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(RegionsPlugin.class);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("should use player permissions to calculate price factor")
    public void shouldUsePlayerPermissionsToCalculatePriceFactor() {

        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, Constants.PRICE_MODIFIER_PREFIX + "2.0", true);

        RegionPlayer regionPlayer = RegionPlayer.of(player);
        assertThat(regionPlayer.priceMultiplier()).isEqualTo(2.0);
    }
}