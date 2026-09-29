package dev.wp.phantoms_utilities.items;

import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import appeng.items.parts.ColoredPartItem;
import aztech.modern_industrialization.pipes.MIPipes;
import aztech.modern_industrialization.pipes.api.PipeNetwork;
import aztech.modern_industrialization.pipes.api.PipeNetworkData;
import aztech.modern_industrialization.pipes.api.PipeNetworkNode;
import aztech.modern_industrialization.pipes.api.PipeNetworkType;
import aztech.modern_industrialization.pipes.impl.PipeBlock;
import aztech.modern_industrialization.pipes.impl.PipeBlockEntity;
import aztech.modern_industrialization.pipes.impl.PipeItem;
import aztech.modern_industrialization.pipes.impl.PipeVoxelShape;
import dev.wp.phantoms_utilities.PUComponents;
import dev.wp.phantoms_utilities.PUSounds;
import dev.wp.phantoms_utilities.PUTags;
import dev.wp.phantoms_utilities.PUTooltips;
import dev.wp.phantoms_utilities.config.ServerConfig;
import dev.wp.phantoms_utilities.helpers.IMouseWheelItem;
import dev.wp.phantoms_utilities.mixin.PipeBlockEntityAccessor;
import dev.wp.phantoms_utilities.mixin.PipeNetworkNodeAccessor;
import dev.wp.phantoms_utilities.util.PUColor;
import dev.wp.phantoms_utilities.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class SprayCan extends Item implements IMouseWheelItem {
    public SprayCan(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static void floodFillCables(Level level, BlockPos startPos, AEColor newColor, Direction side, Player player) {
        final int maxTotalChecks = ServerConfig.maxTotalChecks;
        final int maxBlocks = ServerConfig.maxCableDyeCount;

        // Validate initial position
        if (!(PartHelper.getPart(level, startPos, null) instanceof IPart origPart)) return;
        IPartItem<?> originalCable = origPart.getPartItem();

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);

        int currTotalChecks = 0;
        int currBlocks = 0;

        while (!queue.isEmpty() && currTotalChecks < maxTotalChecks) {
            currTotalChecks++;

            BlockPos currentPos = queue.poll();
            if (visited.contains(currentPos) || !Utils.mayBreakBlock(level, currentPos, level.getBlockState(currentPos), player))
                continue;
            visited.add(currentPos);

            if (processCablePos(level, currentPos, side, newColor, originalCable, player)) {
                currBlocks++;
                if (currBlocks >= maxBlocks) {
                    informPlayer(player, "Max dyeing limit (" + maxBlocks + ") reached, stopping.");
                    break;
                }

                // Add adjacent positions to the queue
                for (Direction dir : Direction.values()) {
                    queue.add(currentPos.relative(dir));
                }
            }
        }
    }

    private static boolean processCablePos(Level level, BlockPos pos, Direction side, AEColor newColor,
                                           IPartItem<?> originalCable, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IColorableBlockEntity cableBusBE)) return false;
        if (!(PartHelper.getPart(level, pos, null) instanceof IPart part)) return false;

        IPartItem<?> candidateCable = part.getPartItem();
        if (candidateCable == null || !candidateCable.equals(originalCable)) return false;

        cableBusBE.recolourBlock(side, newColor, player);
        return true;
    }

    private static void floodFillBlocks(Level level, BlockPos startPos, BlockState originalState, BlockState newState, Player player) {
        final int maxTotalChecks = ServerConfig.maxTotalChecks;
        final int maxBlocks = ServerConfig.maxBlockDyeCount;

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);

        int currTotalChecks = 0;
        int currBlocks = 0;

        while (!queue.isEmpty() && currTotalChecks < maxTotalChecks) {
            currTotalChecks++;

            BlockPos currentPos = queue.poll();
            if (visited.contains(currentPos)
                    || !Utils.mayBreakBlock(level, currentPos, level.getBlockState(currentPos), player))
                continue;
            visited.add(currentPos);

            if (processBlockPos(level, currentPos, originalState, newState)) {
                currBlocks++;
                if (currBlocks >= maxBlocks) {
                    informPlayer(player, "Max dyeing limit (" + maxBlocks + ") reached, stopping.");
                    break;
                }

                // Add adjacent positions to the queue
                for (Direction dir : Direction.values()) {
                    queue.add(currentPos.relative(dir));
                }
            }
        }
    }

    private static boolean processBlockPos(Level level, BlockPos pos, BlockState originalState, BlockState newState) {
        return processBlockPos(level, pos, originalState, newState, Block.UPDATE_ALL);
    }

    private static boolean processBlockPos(Level level, BlockPos pos, BlockState originalState, BlockState newState, int flags) {
        BlockState currentState = level.getBlockState(pos);
        if (!currentState.equals(originalState)) return false;

        CompoundTag data = null;
        if (level.getBlockEntity(pos) instanceof BlockEntity be) {
            data = be.saveWithFullMetadata(level.registryAccess());
        }

        level.setBlock(pos, newState, flags);

        if (data != null && level.getBlockEntity(pos) instanceof BlockEntity newBe) {
            newBe.loadWithComponents(data, level.registryAccess());
            newBe.setChanged();
        }

        return true;
    }

    private static void playSound(Player player, BlockPos pos, SoundEvent sound, Level level) {
        if (player != null) level.playSound(player, pos, sound, player.getSoundSource(), 1.0F, 1.0F);
    }

    private static AEColor getAEColor(PUColor color) {
        return color == PUColor.CLEAR ? AEColor.TRANSPARENT : AEColor.valueOf(color.name());
    }

    private static void informPlayer(Player player, String message) {
        if (player != null) player.displayClientMessage(Component.literal(message), false);
    }

    private static Optional<PipeNetworkNode> getNodeOfType(
            PipeBlockEntity pipeBE, PipeNetworkType type) {
        Optional<PipeNetworkNode> originalNode = Optional.empty();
        for (PipeNetworkNode node : pipeBE.getNodes()) {
            // Already contains a pipe of the same color/type
            if (node.getType() == type) originalNode = Optional.of(node);
        }
        return originalNode;
    }

    public static boolean isBlacklisted(BlockState blockState) {
        if (blockState.getTags().toList().contains(PUTags.Blocks.SPRAY_CAN_BLACKLIST)) return true;
        var blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        return ServerConfig.blacklistedMods.contains(blockId.getNamespace());
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        BlockPos pos = ctx.getClickedPos();
        ItemStack stack = ctx.getItemInHand();
        Direction side = ctx.getClickedFace();

        if (Utils.isAE2Loaded && level.getBlockEntity(pos) instanceof IColorableBlockEntity colorableBlock)
            return paintCables(level, pos, getActiveColor(stack), side, player, colorableBlock);
        else if (Utils.isMILoaded && level.getBlockState(pos).getBlock() instanceof PipeBlock)
            return paintMIPipes(level, pos, getActiveColor(stack), new BlockHitResult(ctx.getClickLocation(), ctx.getClickedFace(), ctx.getClickedPos(), ctx.isInside()), player);
        else return paintBlocks(level, pos, getActiveColor(stack), player);
    }

    private InteractionResult paintMIPipes(Level level, BlockPos pos, PUColor color, BlockHitResult hit, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity)) return InteractionResult.FAIL;

        PipeVoxelShape hitPart = PipeBlock.getHitPart(level, pos, hit);
        if (hitPart == null) return InteractionResult.FAIL;

        PipeNetworkType type = hitPart.type;
        ResourceLocation typeId = type.getIdentifier();

        // Ignore cables, those don't have colors
        if (typeId.getPath().endsWith("_cable")) return InteractionResult.FAIL;

        ResourceLocation newTypeId = getRecoloredMIPipeID(typeId, color);
        if (newTypeId.equals(typeId)) return InteractionResult.FAIL;

        PipeNetworkType newType = PipeNetworkType.get(newTypeId);
        if (newType == null) return InteractionResult.FAIL;
        if (player.isShiftKeyDown()) {
            recolorFullMIPipeNetwork(level, pos, type, newType, player);
        } else {
            getNodeOfType((PipeBlockEntity) level.getBlockEntity(pos), type)
                    .ifPresent(
                            (node) -> {
                                processMIPipePos(level, pos, type, newType, node);
                                cleanUpMIPipe(level, pos);
                            });
        }

        playSound(player, pos, PUSounds.SPRAY_CAN_SPRAY, level);
        return InteractionResult.SUCCESS;
    }

    private void recolorFullMIPipeNetwork(
            Level level,
            BlockPos pipePos,
            PipeNetworkType originalType,
            PipeNetworkType newType,
            Player player) {
        var pipeBE = (PipeBlockEntity) level.getBlockEntity(pipePos);
        getNodeOfType(pipeBE, originalType)
                .ifPresent(
                        startNode -> {
                            var network = ((PipeNetworkNodeAccessor) startNode).getNetwork();
                            if (network == null) {
                                return;
                            }
                            // copy the entire map, since it is about to be mutated
                            var nodes = Map.copyOf(network.getRawNodeMap());
                            nodes.forEach(
                                    (blockPos, node) -> {
                                        processMIPipePos(level, blockPos, originalType, newType, node);
                                    });
                            nodes.forEach((blockPos, _node) -> cleanUpMIPipe(level, blockPos));
                        });
    }

    private boolean processMIPipePos(
            Level level,
            BlockPos pos,
            PipeNetworkType originalType,
            PipeNetworkType newType,
            PipeNetworkNode originalNode) {
        if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBE)) return false;
        if (originalNode.getType() == newType) return false;
        if (getNodeOfType(pipeBE, newType).isPresent()) return false;

        // Capture node data to preserve external connections and settings
        CompoundTag nodeTag = new CompoundTag();
        HolderLookup.Provider registries = level.registryAccess();
        originalNode.toTag(nodeTag, registries);

        PipeNetworkData data;
        PipeNetwork network = ((PipeNetworkNodeAccessor) originalNode).getNetwork();
        if (network != null) data = network.data.clone();
        else data = MIPipes.INSTANCE.getPipeItem(originalType).defaultData.clone();

        // Manual removal without dropping items
        ((PipeBlockEntityAccessor) pipeBE).getPipes().remove(originalNode);
        originalNode.getManager().removeNode(pos);

        pipeBE.addPipe(newType, data);

        for (PipeNetworkNode newNode : pipeBE.getNodes()) {
            if (newNode.getType() == newType) {
                newNode.fromTag(nodeTag, registries);
                break;
            }
        }
        return true;
    }

    private void cleanUpMIPipe(Level level, BlockPos pos) {
        var pipeBE = (PipeBlockEntity) level.getBlockEntity(pos);
        pipeBE.onConnectionsChanged();

        for (Direction dir : Direction.values()) {
            level.neighborChanged(pos.relative(dir), level.getBlockState(pos).getBlock(), pos);
        }
    }

    public static ResourceLocation getRecoloredMIPipeID(ResourceLocation originalId, PUColor color) {
        String path = originalId.getPath();
        String namespace = originalId.getNamespace();

        for (PUColor c : PUColor.VALID_COLORS) {
            if (path.startsWith(c.registryPrefix + "_")) {
                path = path.substring(c.registryPrefix.length() + 1);
                break;
            }
        }

        if (color == PUColor.CLEAR) return ResourceLocation.fromNamespaceAndPath(namespace, path);
        else return ResourceLocation.fromNamespaceAndPath(namespace, color.registryPrefix + "_" + path);
    }

    @NotNull
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        var color = this.getColor(stack).dye;

        if (color != null && target instanceof Sheep sheep) {
            if (sheep.isAlive() && !sheep.isSheared() && sheep.getColor() != color) {
                sheep.setColor(color);
                sheep.level().playSound(player, sheep, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide());
        }
        return InteractionResult.PASS;
    }

    private InteractionResult paintCables(Level level, BlockPos pos, PUColor color, Direction side, Player player, IColorableBlockEntity colorableBlock) {
        AEColor aeColor = getAEColor(color);
        if (colorableBlock.getColor() == aeColor) return InteractionResult.FAIL;

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown() && colorableBlock instanceof IPartHost)
                floodFillCables(level, pos, aeColor, side, player);
            else colorableBlock.recolourBlock(side, aeColor, player);
        }
        playSound(player, pos, PUSounds.SPRAY_CAN_SPRAY, level);
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    private InteractionResult paintBlocks(Level level, BlockPos pos, PUColor color, Player player) {
        BlockState originalState = level.getBlockState(pos);
        if (isBlacklisted(originalState)) return InteractionResult.FAIL;
        BlockState newState = getRecoloredState(originalState, color);
        if (newState != null) {
            if (player.isShiftKeyDown()) floodFillBlocks(level, pos, originalState, newState, player);
            else processBlockPos(level, pos, originalState, newState);
            playSound(player, pos, PUSounds.SPRAY_CAN_SPRAY, level);
            return InteractionResult.sidedSuccess(player.level().isClientSide());
        }
        return InteractionResult.FAIL;
    }

    private static @Nullable BlockState getRecoloredState(BlockState originalState, PUColor color) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(originalState.getBlock());
        ResourceLocation toBeId;
        // c:dyed only: red sand, red nether bricks and mushroom blocks aren't dyes.
        if (color == PUColor.CLEAR) {
            if (!originalState.is(Tags.Blocks.DYED)) return null;
            toBeId = Utils.findClearedID(blockId, BuiltInRegistries.BLOCK);
        } else {
            toBeId = Utils.getRecoloredBlockID(blockId, color);
            if (toBeId.equals(blockId)) toBeId = findDyedBlockID(blockId, color);
            else if (!originalState.is(Tags.Blocks.DYED)) return null;
        }
        if (toBeId == null || blockId.equals(toBeId) || !BuiltInRegistries.BLOCK.containsKey(toBeId)) return null;

        BlockState newState = BuiltInRegistries.BLOCK.get(toBeId).defaultBlockState();
        for (Property<?> property : originalState.getProperties()) {
            newState = Utils.copyProperties(originalState, newState, property);
        }
        return newState;
    }

    // glass -> red_stained_glass, terracotta -> red_terracotta
    private static @Nullable ResourceLocation findDyedBlockID(ResourceLocation id, PUColor color) {
        for (String prefix : List.of(color.registryPrefix + "_", color.registryPrefix + "_stained_")) {
            ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), prefix + id.getPath());
            if (BuiltInRegistries.BLOCK.containsKey(candidate)
                    && BuiltInRegistries.BLOCK.get(candidate).defaultBlockState().is(Tags.Blocks.DYED)) {
                return candidate;
            }
        }
        return null;
    }

    public static BlockState getOffhandPlacementState(BlockPlaceContext context, BlockState state) {
        PUColor color = getOffhandColor(context.getPlayer());
        if (color == null || isBlacklisted(state)) return state;
        BlockState colored = getRecoloredState(state, color);
        if (colored == null) return state;
        context.getLevel().playSound(context.getPlayer(), context.getClickedPos(), PUSounds.SPRAY_CAN_SPRAY,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        return colored;
    }

    // Fallback for items that bypass BlockItem.place.
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Player player)) return;
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof SprayCan can)) return;
        PUColor color = can.getColor(offhand);

        List<BlockSnapshot> snapshots = event instanceof BlockEvent.EntityMultiPlaceEvent multi
                ? multi.getReplacedBlockSnapshots() : List.of(event.getBlockSnapshot());
        boolean painted = false;
        for (BlockSnapshot snapshot : snapshots) {
            painted |= can.paintPlaced(level, snapshot.getPos(), color, player);
        }
        if (painted) level.playSound(null, event.getPos(), PUSounds.SPRAY_CAN_SPRAY, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // Shape updates suppressed so a bed/door half doesn't break against its not-yet-recolored partner.
    private boolean paintPlaced(Level level, BlockPos pos, PUColor color, Player player) {
        BlockState state = level.getBlockState(pos);
        if (isBlacklisted(state)) return false;

        if (Utils.isAE2Loaded && level.getBlockEntity(pos) instanceof IColorableBlockEntity colorable) {
            AEColor aeColor = getAEColor(color);
            return colorable.getColor() != aeColor && colorable.recolourBlock(Direction.UP, aeColor, player);
        }

        BlockState newState = getRecoloredState(state, color);
        return newState != null && processBlockPos(level, pos, state, newState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    public static @Nullable PUColor getOffhandColor(@Nullable Player player) {
        if (player == null) return null;
        ItemStack offhand = player.getOffhandItem();
        return offhand.getItem() instanceof SprayCan can ? can.getColor(offhand) : null;
    }

    // Center cable only; attaching a part to an existing cable leaves its color alone.
    public static void onAE2PartPlaced(Level level, BlockPos pos, IPart part, @Nullable Player player) {
        PUColor color = getOffhandColor(player);
        if (color == null || level.isClientSide() || isBlacklisted(level.getBlockState(pos))) return;
        if (!(level.getBlockEntity(pos) instanceof IPartHost host) || host.getPart(null) != part) return;
        if (!(host instanceof IColorableBlockEntity colorable)) return;

        AEColor aeColor = getAEColor(color);
        if (colorable.getColor() != aeColor && colorable.recolourBlock(Direction.UP, aeColor, player)) {
            level.playSound(null, pos, PUSounds.SPRAY_CAN_SPRAY, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static @Nullable PipeNetworkType getOffhandPipeType(@Nullable Player player, PipeNetworkType held) {
        PUColor color = getOffhandColor(player);
        if (color == null || held.getIdentifier().getPath().endsWith("_cable")) return null;
        if (isBlacklisted(MIPipes.BLOCK_PIPE.get().defaultBlockState())) return null;
        PipeNetworkType type = PipeNetworkType.get(getRecoloredMIPipeID(held.getIdentifier(), color));
        return type == held ? null : type;
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBreaker() instanceof Player player)) return;
        if (!(player.getOffhandItem().getItem() instanceof SprayCan)) return;
        if (isBlacklisted(event.getState())) return;

        boolean stripped = false;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            Item cleared = getClearedItem(stack);
            if (cleared != null) {
                drop.setItem(stack.transmuteCopy(cleared));
                stripped = true;
            }
        }
        if (stripped) {
            event.getLevel().playSound(null, event.getPos(), PUSounds.SPRAY_CAN_SPRAY, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    // For wrench pickups, which bypass BlockDropsEvent.
    public static ItemStack stripForOffhand(@Nullable Player player, ItemStack stack) {
        if (player == null || !(player.getOffhandItem().getItem() instanceof SprayCan)) return stack;
        Item cleared = getClearedItem(stack);
        if (cleared == null) return stack;
        player.level().playSound(null, player.blockPosition(), PUSounds.SPRAY_CAN_SPRAY, SoundSource.PLAYERS, 1.0F, 1.0F);
        return stack.transmuteCopy(cleared);
    }

    private static @Nullable Item getClearedItem(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation cleared;
        if (Utils.isAE2Loaded && stack.getItem() instanceof ColoredPartItem<?> part) {
            Item fluix = AE2Parts.getTransparent(part);
            return fluix == stack.getItem() ? null : fluix;
        } else if (Utils.isMILoaded && stack.getItem() instanceof PipeItem) {
            cleared = getRecoloredMIPipeID(id, PUColor.CLEAR);
        } else if (stack.is(Tags.Items.DYED)) {
            cleared = Utils.findClearedID(id, BuiltInRegistries.ITEM);
        } else return null;

        if (cleared == null || cleared.equals(id) || !BuiltInRegistries.ITEM.containsKey(cleared)) return null;
        return BuiltInRegistries.ITEM.get(cleared);
    }

    // Transparent variant of any colored part item, addons included, keyed by part class.
    private static final class AE2Parts {
        private static final Map<Class<?>, Item> TRANSPARENT = new HashMap<>();

        static {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof ColoredPartItem<?> part && part.getColor() == AEColor.TRANSPARENT) {
                    TRANSPARENT.put(part.getPartClass(), item);
                }
            }
        }

        static @Nullable Item getTransparent(ColoredPartItem<?> part) {
            return TRANSPARENT.get(part.getPartClass());
        }
    }

    public void cycleColors(ItemStack stack, @Nullable PUColor currColor, Boolean forward) {
        int colorCount = PUColor.VALID_COLORS.size();

        if (currColor == null || currColor == PUColor.CLEAR) {
            if (forward) this.setColor(stack, PUColor.VALID_COLORS.getFirst());
            else this.setColor(stack, PUColor.VALID_COLORS.get(colorCount - 1));
            return;
        }

        int index = PUColor.VALID_COLORS.indexOf(currColor);

        if (forward) {
            index++;
            if (index >= colorCount) this.setColor(stack, PUColor.CLEAR);
            else this.setColor(stack, PUColor.VALID_COLORS.get(index));
        } else {
            index--;
            if (index < 0) this.setColor(stack, PUColor.CLEAR);
            else this.setColor(stack, PUColor.VALID_COLORS.get(index));
        }
    }

    public PUColor getActiveColor(ItemStack stack) {
        return this.getColor(stack);
    }

    public PUColor getColor(ItemStack stack) {
        var selectedColor = stack.get(PUComponents.SELECTED_COLOR);
        if (selectedColor != null) return selectedColor;
        return PUColor.CLEAR;
    }

    private void setColor(ItemStack stack, @Nullable PUColor newColor) {
        stack.set(PUComponents.SELECTED_COLOR, newColor);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component extra = Component.empty();
        final PUColor color = getActiveColor(stack);
        if (color != null && Dist.CLIENT.isClient()) extra = Component.translatable(color.translationKey);

        return super.getName(stack).copy().append(" - ").append(extra);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var use = PUTooltips.key("key.use");
        var sneak = PUTooltips.key("key.sneak");
        var pick = PUTooltips.key("key.pickItem");
        PUTooltips.addShiftInfo(tooltip,
                PUTooltips.line(PUTooltips.SPRAY_CAN_PAINT, use),
                PUTooltips.line(PUTooltips.SPRAY_CAN_PAINT_CONNECTED, sneak, use),
                PUTooltips.line(PUTooltips.SPRAY_CAN_CYCLE, sneak),
                PUTooltips.line(PUTooltips.SPRAY_CAN_PICK, pick),
                PUTooltips.line(PUTooltips.SPRAY_CAN_PICKER, sneak, pick),
                PUTooltips.line(PUTooltips.SPRAY_CAN_OFFHAND));
    }

    @Override
    public void onScroll(ItemStack stack, boolean up) {
        this.cycleColors(stack, stack.get(PUComponents.SELECTED_COLOR), up);
    }

    public void setActiveColor(ItemStack sprayCan, @Nullable PUColor color) {
        if (color == null || color == PUColor.CLEAR) {
            setColor(sprayCan, PUColor.CLEAR);
            return;
        }

        for (PUColor puColor : PUColor.VALID_COLORS) {
            if (puColor == color) {
                setColor(sprayCan, color);
                return;
            }
        }
    }
}
