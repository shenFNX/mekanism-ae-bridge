package io.github.shenfnx.mekanismae.gametest;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import appeng.core.definitions.AEBlocks;
import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.block.entity.AbstractMeProcessingBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.AbstractMultiKeyMeMachineBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeChemicalInfuserBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeChemicalOxidizerBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeElectrolyticSeparatorBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeEnrichmentChamberBlockEntity;
import io.github.shenfnx.mekanismae.config.MachineSettings;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.compat.mekanismextras.MekanismExtrasCompat;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import io.github.shenfnx.mekanismae.registry.ModItems;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.registries.MekanismItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MekanismAeMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineLedgerGameTests {
    private static final String EMPTY_TEMPLATE = "bastion/mobs/empty";
    private static final BlockPos MACHINE_POS = BlockPos.ZERO;

    private MachineLedgerGameTests() {
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void machineSettingsClampAndSaturate(GameTestHelper helper) {
        helper.assertValueEqual(ModItems.SPEED_CARD.get().getDefaultMaxStackSize(), 64,
                "speed-card inventory stack size");
        helper.assertValueEqual(ModItems.PARALLEL_CARD.get().getDefaultMaxStackSize(), 64,
                "parallel-card inventory stack size");
        helper.assertValueEqual(ModItems.ENERGY_CARD.get().getDefaultMaxStackSize(), 64,
                "energy-card inventory stack size");
        helper.assertValueEqual(MachineType.ME_CHEMICAL_INFUSER.defaultBaseOperationsPerCycle(), 1_000,
                "chemical infuser base throughput");
        helper.assertValueEqual(MachineType.ME_ELECTROLYTIC_SEPARATOR.defaultBaseOperationsPerCycle(), 1_000,
                "electrolytic separator base throughput");
        helper.assertValueEqual(MachineType.ME_ROTARY_CONDENSENTRATOR.defaultBaseOperationsPerCycle(), 1_000,
                "rotary condensentrator base throughput");
        helper.assertValueEqual(MachineType.ME_CHEMICAL_WASHER.defaultBaseOperationsPerCycle(), 1_000,
                "chemical washer base throughput");

        MachineSettings settings = new MachineSettings(
                true, 2.0, true,
                2_000_000_000, 2_000_000_000, 250_000_000, 250_000_000,
                10_000, 20, 1_000, Long.MAX_VALUE / 2 + 1,
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
                List.of(1, 4, 16, 64, 256, 1_024, 4_096, 16_384, 65_536),
                List.of(3, 6, 10, 16, 32, 64, 128, 256),
                List.of(2, 4, 8, 16, 32, 64, 128, 256),
                List.of(2, 4, 8, 16, 32, 64, 128, 256),
                List.of(2, 4, 8, 16, 32, 64, 128, 256));

        helper.assertValueEqual(settings.speedMultiplier(-10), 1, "negative speed-card clamp");
        helper.assertValueEqual(settings.speedMultiplier(99), 9, "excess speed-card clamp");
        helper.assertValueEqual(settings.parallelMultiplier(4, 3), 4_096,
                "player-visible parallel multiplier excludes the internal batch base");
        helper.assertValueEqual(settings.processingOperationsPerCycle(4, 3), 4_096_000,
                "chemical processing retains the internal batch base");
        helper.assertValueEqual(settings.parallelMultiplier(8, 3), 1_048_576,
                "eight-card Ultimate parallel multiplier");
        helper.assertValueEqual(settings.parallelMultiplier(8, 7), 16_777_216,
                "Mekanism Extras Infinite visible parallel multiplier");
        helper.assertValueEqual(settings.processingOperationsPerCycle(8, 7), Integer.MAX_VALUE,
                "processing operation count saturates safely");
        helper.assertValueEqual(settings.energyCapacity(8, 3), Integer.MAX_VALUE,
                "saturated energy capacity");
        helper.assertValueEqual(settings.energyReceive(8, 3), Integer.MAX_VALUE,
                "saturated receive limit");
        helper.assertValueEqual(settings.bufferOperationLimit(3), Long.MAX_VALUE,
                "saturated task-buffer limit");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void upgradeCardsHaveCraftingRecipes(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String path : List.of("speed_card", "parallel_card", "energy_card")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MekanismAeMod.MOD_ID, path);
            helper.assertTrue(recipes.byKey(id).isPresent(), path + " crafting recipe must be loaded");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void upgradesAndEnergySurviveNbtRoundTrip(GameTestHelper helper) {
        MeEnrichmentChamberBlockEntity machine = placeMachine(helper);
        int firstUpgradeSlot = AbstractMeProcessingBlockEntity.PATTERN_SLOT_COUNT;

        machine.setItem(firstUpgradeSlot, new ItemStack(ModItems.SPEED_CARD.get(), 16));
        machine.setItem(firstUpgradeSlot + 1, new ItemStack(ModItems.PARALLEL_CARD.get(), 16));
        machine.setItem(firstUpgradeSlot + 2, new ItemStack(ModItems.ENERGY_CARD.get(), 16));
        machine.setItem(AbstractMeProcessingBlockEntity.TIER_SLOT_INDEX,
                new ItemStack(MekanismItems.ULTIMATE_TIER_INSTALLER.get(), 4));
        machine.getEnergyStorage().receiveEnergy(123_456, false);
        machine.toggleNetworkEnabled();

        helper.assertValueEqual(machine.getUpgradeCount(ModItems.SPEED_CARD.get()), 8,
                "speed-card slot clamp");
        helper.assertValueEqual(machine.getUpgradeCount(ModItems.PARALLEL_CARD.get()), 8,
                "parallel-card slot clamp");
        helper.assertValueEqual(machine.getUpgradeCount(ModItems.ENERGY_CARD.get()), 8,
                "energy-card slot clamp");
        helper.assertValueEqual(machine.getItem(AbstractMeProcessingBlockEntity.TIER_SLOT_INDEX).getCount(), 1,
                "tier-installer slot clamp");
        helper.assertValueEqual(machine.getUpgradeLimitForSlot(firstUpgradeSlot + 3,
                new ItemStack(ModItems.PARALLEL_CARD.get())), 0, "global parallel-card cap");

        CompoundTag saved = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        MeEnrichmentChamberBlockEntity restored = new MeEnrichmentChamberBlockEntity(
                MACHINE_POS, ModBlocks.ME_ENRICHMENT_CHAMBER.get().defaultBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());

        helper.assertValueEqual(restored.getUpgradeCount(ModItems.SPEED_CARD.get()), 8,
                "restored speed cards");
        helper.assertValueEqual(restored.getUpgradeCount(ModItems.PARALLEL_CARD.get()), 8,
                "restored parallel cards");
        helper.assertValueEqual(restored.getUpgradeCount(ModItems.ENERGY_CARD.get()), 8,
                "restored energy cards");
        helper.assertValueEqual(restored.getTierIndex(), 3, "restored ultimate tier");
        helper.assertFalse(restored.isNetworkEnabled(), "network toggle should survive reload");
        helper.assertValueEqual(restored.getEnergyStorage().getEnergyStored(), 123_456,
                "restored energy");
        helper.assertValueEqual(restored.getParallelMultiplier(), machine.getParallelMultiplier(),
                "restored parallel multiplier");
        helper.assertValueEqual(restored.getBufferOperationLimit(), machine.getBufferOperationLimit(),
                "restored buffer limit");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void optionalMekanismExtrasTierInstallers(GameTestHelper helper) {
        List<Item> installers = MekanismExtrasCompat.availableTierInstallers();
        helper.assertTrue(installers.size() == 4 || installers.size() == 8,
                "tier installer list must contain vanilla installers and optional Mekanism Extras installers");
        if (installers.size() == 8) {
            MeEnrichmentChamberBlockEntity machine = placeMachine(helper);
            for (int index = 4; index < installers.size(); index++) {
                ItemStack installer = new ItemStack(installers.get(index));
                helper.assertValueEqual(MekanismExtrasCompat.getTierIndex(installer), index,
                        "Mekanism Extras tier index");
                machine.setItem(AbstractMeProcessingBlockEntity.TIER_SLOT_INDEX, installer);
                helper.assertValueEqual(machine.getTierIndex(), index,
                        "Mekanism Extras tier installer acceptance");
                machine.removeItemNoUpdate(AbstractMeProcessingBlockEntity.TIER_SLOT_INDEX);
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void nodeTeardownDoesNotDirtyChunk(GameTestHelper helper) {
        placeMachine(helper);
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> {
                    MeEnrichmentChamberBlockEntity machine = helper.getBlockEntity(MACHINE_POS);
                    var chunk = helper.getLevel().getChunkAt(helper.absolutePos(MACHINE_POS));
                    chunk.setUnsaved(false);
                    machine.onChunkUnloaded();
                    helper.assertFalse(chunk.isUnsaved(),
                            "AE node teardown must not schedule a late chunk save that erases crafting CPU state");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void patternsStayIsolatedAndLedgerSurvivesReload(GameTestHelper helper) {
        MeEnrichmentChamberBlockEntity machine = placeMachine(helper);

        helper.assertTrue(push(machine, helper, Items.SAND, Items.GRAVEL, 1_000),
                "sand batch should be accepted");
        helper.assertTrue(push(machine, helper, Items.RED_SAND, Items.GRAVEL, 250),
                "red-sand batch should queue separately");
        helper.assertValueEqual(machine.getCurrentOperationCount(), 1_000L,
                "active sand operations");
        helper.assertValueEqual(machine.getBufferedOperationCount(), 1_250L,
                "total isolated operations");
        helper.assertTrue(machine.getProcessingInputDisplay().is(Items.SAND),
                "the active pattern must remain sand until it drains");

        CompoundTag saved = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        MeEnrichmentChamberBlockEntity restored = new MeEnrichmentChamberBlockEntity(
                MACHINE_POS, ModBlocks.ME_ENRICHMENT_CHAMBER.get().defaultBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());

        helper.assertValueEqual(restored.getCurrentOperationCount(), 1_000L,
                "restored active operations");
        helper.assertValueEqual(restored.getBufferedOperationCount(), 1_250L,
                "restored queued operations");
        helper.assertTrue(restored.getProcessingInputDisplay().is(Items.SAND),
                "restored active pattern identity");
        helper.assertFalse(restored.returnAllResourcesToNetwork(),
                "offline return must report incomplete");
        helper.assertValueEqual(restored.getBufferedOperationCount(), 1_250L,
                "offline return must not discard work");

        saved.putLong("PendingOperations", 999L);
        MeEnrichmentChamberBlockEntity corrupted = new MeEnrichmentChamberBlockEntity(
                MACHINE_POS, ModBlocks.ME_ENRICHMENT_CHAMBER.get().defaultBlockState());
        corrupted.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(corrupted.isProcessingFaulted(),
                "inconsistent persisted operation counts must fault safely");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void bufferAcceptsExactLimitAndRejectsOverflow(GameTestHelper helper) {
        MeEnrichmentChamberBlockEntity machine = placeMachine(helper);
        long limit = machine.getBufferOperationLimit();

        helper.assertTrue(push(machine, helper, Items.SAND, Items.GRAVEL, limit),
                "a batch exactly at the configured buffer limit should be accepted");
        helper.assertValueEqual(machine.getBufferedOperationCount(), limit,
                "exact-limit buffered operations");
        helper.assertFalse(push(machine, helper, Items.RED_SAND, Items.GRAVEL, 1),
                "one operation beyond the configured buffer limit must be rejected");
        helper.assertValueEqual(machine.getBufferedOperationCount(), limit,
                "rejected overflow must not mutate the ledger");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void itemToChemicalLedgerSurvivesReload(GameTestHelper helper) {
        MeChemicalOxidizerBlockEntity machine = placeMachine(
                helper, ModBlocks.ME_CHEMICAL_OXIDIZER.get());
        MekanismKey redstone = chemicalKey(MekanismChemicals.REDSTONE.asStack(1));
        List<GenericStack> patternInputs = List.of(new GenericStack(AEItemKey.of(Items.REDSTONE), 1_000));
        List<GenericStack> patternOutputs = List.of(new GenericStack(redstone, 10_000));

        helper.assertTrue(push(machine, helper, patternInputs, patternOutputs,
                List.of(new Delivered(AEItemKey.of(Items.REDSTONE), 1_000))),
                "item-to-chemical batch should be accepted");
        helper.assertValueEqual(machine.getCurrentOperationCount(), 1_000L,
                "active oxidizing operations");
        helper.assertValueEqual(machine.getProcessingOutputChemicalKey(), redstone,
                "oxidizing chemical output");
        helper.assertValueEqual(machine.getProcessingOutputChemicalAmount(), 10L,
                "oxidizing output per operation");

        CompoundTag saved = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        MeChemicalOxidizerBlockEntity restored = new MeChemicalOxidizerBlockEntity(
                MACHINE_POS, ModBlocks.ME_CHEMICAL_OXIDIZER.get().defaultBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertValueEqual(restored.getBufferedOperationCount(), 1_000L,
                "restored oxidizing operations");
        helper.assertValueEqual(restored.getProcessingOutputChemicalKey(), redstone,
                "restored oxidizing output key");
        helper.assertFalse(restored.returnAllResourcesToNetwork(),
                "offline chemical return must report incomplete");
        helper.assertValueEqual(restored.getBufferedOperationCount(), 1_000L,
                "offline chemical return must preserve work");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void dualChemicalPatternsStayIsolatedAndPersist(GameTestHelper helper) {
        MeChemicalInfuserBlockEntity machine = placeMachine(
                helper, ModBlocks.ME_CHEMICAL_INFUSER.get());
        MekanismKey hydrogen = chemicalKey(MekanismChemicals.HYDROGEN.asStack(1));
        MekanismKey chlorine = chemicalKey(MekanismChemicals.CHLORINE.asStack(1));
        MekanismKey hydrogenChloride = chemicalKey(MekanismChemicals.HYDROGEN_CHLORIDE.asStack(1));

        helper.assertTrue(push(machine, helper,
                List.of(new GenericStack(hydrogen, 1_000), new GenericStack(chlorine, 1_000)),
                List.of(new GenericStack(hydrogenChloride, 1_000)),
                List.of(new Delivered(hydrogen, 1_000), new Delivered(chlorine, 1_000))),
                "hydrogen/chlorine batch should be accepted");
        helper.assertTrue(push(machine, helper,
                List.of(new GenericStack(chlorine, 250), new GenericStack(hydrogen, 250)),
                List.of(new GenericStack(hydrogenChloride, 250)),
                List.of(new Delivered(chlorine, 250), new Delivered(hydrogen, 250))),
                "reversed-input pattern should queue separately");
        helper.assertValueEqual(machine.getCurrentOperationCount(), 1_000L,
                "active dual-chemical operations");
        helper.assertValueEqual(machine.getBufferedOperationCount(), 1_250L,
                "total isolated dual-chemical operations");
        helper.assertValueEqual(machine.getFirstProcessingChemicalKey(), hydrogen,
                "active first chemical");
        helper.assertValueEqual(machine.getSecondProcessingChemicalKey(), chlorine,
                "active second chemical");

        CompoundTag saved = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        MeChemicalInfuserBlockEntity restored = new MeChemicalInfuserBlockEntity(
                MACHINE_POS, ModBlocks.ME_CHEMICAL_INFUSER.get().defaultBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertValueEqual(restored.getCurrentOperationCount(), 1_000L,
                "restored active dual-chemical operations");
        helper.assertValueEqual(restored.getBufferedOperationCount(), 1_250L,
                "restored queued dual-chemical operations");
        helper.assertValueEqual(restored.getFirstProcessingChemicalKey(), hydrogen,
                "restored first chemical identity");
        helper.assertValueEqual(restored.getSecondProcessingChemicalKey(), chlorine,
                "restored second chemical identity");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void electrolysisTracksOmittedByproductAndPersists(GameTestHelper helper) {
        MeElectrolyticSeparatorBlockEntity machine = placeMachine(
                helper, ModBlocks.ME_ELECTROLYTIC_SEPARATOR.get());
        AEFluidKey water = AEFluidKey.of(Fluids.WATER);
        MekanismKey hydrogen = chemicalKey(MekanismChemicals.HYDROGEN.asStack(1));
        MekanismKey oxygen = chemicalKey(MekanismChemicals.OXYGEN.asStack(1));

        helper.assertTrue(push(machine, helper,
                List.of(new GenericStack(water, 2_000)),
                List.of(new GenericStack(hydrogen, 2_000)),
                List.of(new Delivered(water, 2_000))),
                "electrolysis pattern may omit the oxygen byproduct");
        helper.assertValueEqual(machine.getCurrentOperationCount(), 1_000L,
                "active electrolysis operations");
        helper.assertValueEqual(machine.getProcessingResourceKey(0), water,
                "electrolysis fluid input");
        helper.assertValueEqual(machine.getProcessingResourceKey(
                AbstractMultiKeyMeMachineBlockEntity.MAX_INPUT_SLOTS), hydrogen,
                "declared hydrogen output");
        helper.assertValueEqual(machine.getProcessingResourceAmount(
                AbstractMultiKeyMeMachineBlockEntity.MAX_INPUT_SLOTS), 2L,
                "hydrogen per operation");
        helper.assertValueEqual(machine.getProcessingResourceKey(
                AbstractMultiKeyMeMachineBlockEntity.MAX_INPUT_SLOTS + 1), oxygen,
                "undeclared oxygen byproduct");
        helper.assertValueEqual(machine.getProcessingResourceAmount(
                AbstractMultiKeyMeMachineBlockEntity.MAX_INPUT_SLOTS + 1), 1L,
                "oxygen per operation");

        CompoundTag saved = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        MeElectrolyticSeparatorBlockEntity restored = new MeElectrolyticSeparatorBlockEntity(
                MACHINE_POS, ModBlocks.ME_ELECTROLYTIC_SEPARATOR.get().defaultBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertValueEqual(restored.getBufferedOperationCount(), 1_000L,
                "restored electrolysis operations");
        helper.assertValueEqual(restored.getProcessingResourceKey(
                AbstractMultiKeyMeMachineBlockEntity.MAX_INPUT_SLOTS + 1), oxygen,
                "restored omitted byproduct identity");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void multiResourceLedgerRejectsWrongOutputAmount(GameTestHelper helper) {
        MeElectrolyticSeparatorBlockEntity machine = placeMachine(
                helper, ModBlocks.ME_ELECTROLYTIC_SEPARATOR.get());
        AEFluidKey water = AEFluidKey.of(Fluids.WATER);
        MekanismKey hydrogen = chemicalKey(MekanismChemicals.HYDROGEN.asStack(1));

        helper.assertFalse(push(machine, helper,
                List.of(new GenericStack(water, 2_000)),
                List.of(new GenericStack(hydrogen, 1_999)),
                List.of(new Delivered(water, 2_000))),
                "incorrect declared output amount must be rejected");
        helper.assertValueEqual(machine.getBufferedOperationCount(), 0L,
                "rejected multi-resource pattern must not mutate the ledger");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void busyMachineCannotBeBroken(GameTestHelper helper) {
        MeEnrichmentChamberBlockEntity machine = placeMachine(helper);
        helper.assertTrue(push(machine, helper, Items.SAND, Items.GRAVEL, 1_000),
                "test task should be accepted");

        BlockPos absolutePos = helper.absolutePos(MACHINE_POS);
        BlockState state = helper.getLevel().getBlockState(absolutePos);
        boolean destroyed = state.getBlock().onDestroyedByPlayer(state, helper.getLevel(), absolutePos,
                helper.makeMockPlayer(GameType.SURVIVAL), true, helper.getLevel().getFluidState(absolutePos));

        helper.assertFalse(destroyed, "a machine with processing resources must reject breaking");
        helper.assertBlockPresent(ModBlocks.ME_ENRICHMENT_CHAMBER.get(), MACHINE_POS);
        helper.assertValueEqual(machine.getBufferedOperationCount(), 1_000L,
                "rejected breaking must preserve the ledger");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void idleMachineDropsInstalledUpgrades(GameTestHelper helper) {
        MeEnrichmentChamberBlockEntity machine = placeMachine(helper);
        int firstUpgradeSlot = AbstractMeProcessingBlockEntity.PATTERN_SLOT_COUNT;
        Item tierInstaller = MekanismItems.BASIC_TIER_INSTALLER.get();
        machine.setItem(firstUpgradeSlot, new ItemStack(ModItems.PARALLEL_CARD.get(), 8));
        machine.setItem(AbstractMeProcessingBlockEntity.TIER_SLOT_INDEX, new ItemStack(tierInstaller));

        BlockPos absolutePos = helper.absolutePos(MACHINE_POS);
        BlockState state = helper.getLevel().getBlockState(absolutePos);
        boolean destroyed = state.getBlock().onDestroyedByPlayer(state, helper.getLevel(), absolutePos,
                helper.makeMockPlayer(GameType.SURVIVAL), true, helper.getLevel().getFluidState(absolutePos));

        helper.assertTrue(destroyed, "an idle machine should be breakable");
        helper.assertBlockNotPresent(ModBlocks.ME_ENRICHMENT_CHAMBER.get(), MACHINE_POS);
        helper.assertItemEntityCountIs(ModItems.PARALLEL_CARD.get(), MACHINE_POS, 2.0, 8);
        helper.assertItemEntityCountIs(tierInstaller, MACHINE_POS, 2.0, 1);
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE, timeoutTicks = 40)
    public static void appliedFluxCanPowerMachineOnSameGrid(GameTestHelper helper) {
        if (!ModList.get().isLoaded("appflux")) {
            helper.succeed();
            return;
        }

        MeEnrichmentChamberBlockEntity machine = placeMachine(helper);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(machine.getMainNode().getGrid() != null,
                            "the test machine must have an AE2 grid");
                    try {
                        Class<?> cacheType = Class.forName(
                                "com.glodblock.github.appflux.common.me.energy.EnergyCapCache");
                        Constructor<?> constructor = cacheType.getConstructor(
                                net.minecraft.server.level.ServerLevel.class,
                                BlockPos.class,
                                Supplier.class);
                        Method getEnergyCap = cacheType.getMethod(
                                "getEnergyCap", BlockCapability.class, Direction.class);

                        BlockPos machinePos = helper.absolutePos(MACHINE_POS);
                        BlockPos virtualAccessorPos = machinePos.relative(Direction.NORTH);
                        Supplier<Object> sameGrid = () -> machine.getMainNode().getGrid();
                        Object cache = constructor.newInstance(
                                helper.getLevel(), virtualAccessorPos, sameGrid);
                        Object energy = getEnergyCap.invoke(
                                cache, Capabilities.EnergyStorage.BLOCK, Direction.SOUTH);

                        helper.assertTrue(energy == machine.getEnergyStorage(),
                                "Applied Flux must expose the machine's receive-only FE capability on the same grid");
                    } catch (ReflectiveOperationException exception) {
                        throw new AssertionError("Unable to exercise the Applied Flux energy cache", exception);
                    }
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE, timeoutTicks = 60)
    public static void appliedFluxInductionCardDrawsStoredFe(GameTestHelper helper) {
        if (!ModList.get().isLoaded("appflux")) {
            helper.succeed();
            return;
        }

        MeEnrichmentChamberBlockEntity machine = placeMachine(helper);
        helper.setBlock(MACHINE_POS.relative(Direction.NORTH), AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(machine.isNetworkOnline(), "the induction-card machine must be online");
                    try {
                        AEKey feKey = appliedFluxFeKey();
                        AtomicLong stored = new AtomicLong(1_000_000);
                        MEStorage energyBank = new MEStorage() {
                            @Override
                            public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
                                if (!feKey.equals(what) || amount <= 0) {
                                    return 0;
                                }
                                long extracted = Math.min(amount, stored.get());
                                if (mode == Actionable.MODULATE) {
                                    stored.addAndGet(-extracted);
                                }
                                return extracted;
                            }

                            @Override
                            public void getAvailableStacks(KeyCounter out) {
                                out.add(feKey, stored.get());
                            }

                            @Override
                            public Component getDescription() {
                                return Component.literal("GameTest FE storage");
                            }
                        };
                        IStorageProvider provider = mounts -> mounts.mount(energyBank);
                        machine.getMainNode().getGrid().getStorageService().addGlobalStorageProvider(provider);

                        Item inductionCard = net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .get(ResourceLocation.fromNamespaceAndPath("appflux", "induction_card"));
                        int firstUpgradeSlot = AbstractMeProcessingBlockEntity.PATTERN_SLOT_COUNT;
                        machine.setItem(firstUpgradeSlot, new ItemStack(inductionCard, 8));
                        helper.assertValueEqual(machine.getItem(firstUpgradeSlot).getCount(), 1,
                                "induction-card slot clamp");
                        helper.assertValueEqual(machine.getUpgradeLimitForSlot(firstUpgradeSlot + 1,
                                new ItemStack(inductionCard)), 0, "global induction-card cap");

                        machine.tickServer();
                        helper.assertTrue(machine.getEnergyStorage().getEnergyStored() > 0,
                                "induction card must fill the local buffer from ME-stored FE");
                        helper.assertTrue(stored.get() < 1_000_000,
                                "induction card must extract FE from the ME storage service");
                    } catch (ReflectiveOperationException exception) {
                        throw new AssertionError("Unable to create the Applied Flux FE key", exception);
                    }
                })
                .thenSucceed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static AEKey appliedFluxFeKey() throws ReflectiveOperationException {
        Class<? extends Enum> energyType = (Class<? extends Enum>) Class.forName(
                "com.glodblock.github.appflux.common.me.key.type.EnergyType");
        Object fe = Enum.valueOf(energyType, "FE");
        Class<?> fluxKey = Class.forName("com.glodblock.github.appflux.common.me.key.FluxKey");
        return (AEKey) fluxKey.getMethod("of", energyType).invoke(null, fe);
    }

    private static MeEnrichmentChamberBlockEntity placeMachine(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, ModBlocks.ME_ENRICHMENT_CHAMBER.get());
        return helper.getBlockEntity(MACHINE_POS);
    }

    private static <T extends AbstractMeProcessingBlockEntity> T placeMachine(
            GameTestHelper helper, Block block) {
        helper.setBlock(MACHINE_POS, block);
        return helper.getBlockEntity(MACHINE_POS);
    }

    private static boolean push(MeEnrichmentChamberBlockEntity machine, GameTestHelper helper,
            Item input, Item output, long amount) {
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(input), 1)),
                List.of(new GenericStack(AEItemKey.of(output), 1)));
        if (!machine.setPattern(encoded)) {
            helper.fail("Machine failed to install the generated processing pattern");
            return false;
        }
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        if (details == null) {
            helper.fail("AE2 failed to decode the generated processing pattern");
            return false;
        }
        KeyCounter delivered = new KeyCounter();
        delivered.add(AEItemKey.of(input), amount);
        return machine.pushPattern(details, new KeyCounter[] {delivered});
    }

    private static boolean push(AbstractMeProcessingBlockEntity machine, GameTestHelper helper,
            List<GenericStack> patternInputs, List<GenericStack> patternOutputs,
            List<Delivered> deliveredInputs) {
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(patternInputs, patternOutputs);
        if (!machine.setPattern(encoded)) {
            helper.fail("Machine failed to install the generated processing pattern");
            return false;
        }
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        if (details == null) {
            helper.fail("AE2 failed to decode the generated multi-resource pattern");
            return false;
        }
        KeyCounter[] counters = new KeyCounter[deliveredInputs.size()];
        for (int index = 0; index < deliveredInputs.size(); index++) {
            Delivered delivered = deliveredInputs.get(index);
            counters[index] = new KeyCounter();
            counters[index].add(delivered.key(), delivered.amount());
        }
        return machine.pushPattern(details, counters);
    }

    private static MekanismKey chemicalKey(mekanism.api.chemical.ChemicalStack stack) {
        return MekanismKey.of(stack);
    }

    private record Delivered(AEKey key, long amount) {
    }
}
