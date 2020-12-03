package net.silthus.regions;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.limits.Limit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LimitTest {

    private ServerMock server;
    private RegionsPlugin plugin;
    private RegionPlayer player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(RegionsPlugin.class);
        player = RegionPlayer.getOrCreate(server.addPlayer());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("hasReachedTotalLimit(...)")
    class hasReachedTotalLimit {

        @Test
        @DisplayName("player should reach limit if region sum is reached")
        void shouldReachLimitWithSumOfRegions() {

            Limit limit = new Limit().total(5);
            for (int i = 0; i < 5; i++) {
                player.regions().add(mock(Region.class));
            }

            assertThat(limit.hasReachedTotalLimit(player))
                    .extracting(Limit.Result::reachedLimit)
                    .isEqualTo(true);
        }

        @Test
        @DisplayName("player should not reach limit with no regions")
        void shouldNotHaveReachedLimit() {

            Limit limit = new Limit().total(1);
            assertThat(limit.hasReachedTotalLimit(player))
                    .extracting(Limit.Result::reachedLimit)
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("an infinite limit should never be reached")
        void shouldNeverReachLimit() {

            Limit limit = new Limit();
            for (int i = 0; i < 100; i++) {
                player.regions().add(mock(Region.class));
            }

            assertThat(limit.hasReachedTotalLimit(player))
                    .extracting(Limit.Result::reachedLimit)
                    .isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("hasReachedRegionsInGroupLimit(...)")
    class hasReachedRegionsInGroupLimit {

        private RegionGroup fooGroup;
        private RegionGroup barGroup;

        @BeforeEach
        void setUp() {
            fooGroup = RegionGroup.getOrCreate("foo");
            barGroup = RegionGroup.getOrCreate("bar");
        }

        @Test
        @DisplayName("should reach regions inside group limit")
        void shouldReachRegionsInGroupLimit() {

            Limit limit = new Limit().setGroupLimit("foo", 1);
            player.regions().add(new Region("foo").group(fooGroup));

            assertThat(limit.hasReachedRegionsInGroupLimit(player, fooGroup))
                    .extracting(Limit.Result::reachedLimit)
                    .isEqualTo(true);
        }

        @Test
        @DisplayName("should only check given group")
        void shouldNotCheckOtherGroups() {

            Limit limit = new Limit().setGroupLimit("foo", 1);
            player.regions().add(new Region("foo").group(barGroup));

            assertThat(limit.hasReachedRegionsInGroupLimit(player, fooGroup))
                    .extracting(Limit.Result::reachedLimit)
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("should not check group limits if group is null")
        void shouldNotCheckGroupIfGroupIsNull() {

            Limit limit = new Limit().setGroupLimit("foo", 1);
            player.regions().add(new Region("foo").group(fooGroup));

            assertThat(limit.hasReachedRegionsInGroupLimit(player, null))
                    .extracting(Limit.Result::reachedLimit)
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("should ignore regions with null groups")
        void shouldIgnoreRegionsWithNullGroups() {

            Limit limit = new Limit().setGroupLimit("foo", 1);
            player.regions().add(new Region("foo"));

            assertThat(limit.hasReachedRegionsInGroupLimit(player, fooGroup))
                    .extracting(Limit.Result::reachedLimit)
                    .isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("hasReachedGroupLimit(...)")
    class hasReachedGroupLimit {

        private RegionGroup fooGroup;
        private RegionGroup barGroup;

        @BeforeEach
        void setUp() {
            fooGroup = RegionGroup.getOrCreate("foo");
            barGroup = RegionGroup.getOrCreate("bar");
        }

        @Test
        @DisplayName("should not reach limit for multiple regions inside the same group")
        void shouldNotReachLimitForRegionsInSameGroup() {

            Limit limit = new Limit().groups(2);
            player.regions().add(new Region("foo").group(fooGroup));
            player.regions().add(new Region("bar").group(fooGroup));

            assertThat(limit.hasReachedGroupLimit(player))
                    .extracting(Limit.Result::reachedLimit)
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("should reach limit for groups with multiple regions")
        void shouldReachLimitForMultipleGroupsWithRegions() {

            Limit limit = new Limit().groups(2);
            player.regions().add(new Region("foo").group(fooGroup));
            player.regions().add(new Region("bar").group(barGroup));

            assertThat(limit.hasReachedGroupLimit(player))
                    .extracting(Limit.Result::reachedLimit)
                    .isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("combine(...)")
    class combine {

        private Limit defaultPrio;
        private Limit highPrio;

        @BeforeEach
        void setUp() {
            defaultPrio = new Limit()
                    .total(1);
            highPrio = new Limit()
                    .priority(101)
                    .total(1)
                    .groups(2);
        }

        @Test
        @DisplayName("should combine and use higher priority limit")
        void shouldUseHigherPriorityLimit() {

            assertThat(defaultPrio.combine(highPrio))
                    .extracting(Limit::priority, Limit::total, Limit::groups)
                    .contains(101, 1, 2);
        }
    }
}