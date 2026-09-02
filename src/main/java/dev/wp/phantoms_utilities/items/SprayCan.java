package dev.wp.phantoms_utilities.items;

import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import aztech.modern_industrialization.pipes.MIPipes;
import aztech.modern_industrialization.pipes.api.PipeNetwork;
import aztech.modern_industrialization.pipes.api.PipeNetworkData;
import aztech.modern_industrialization.pipes.api.PipeNetworkNode;
import aztech.modern_industrialization.pipes.api.PipeNetworkType;
import aztech.modern_industrialization.pipes.impl.PipeBlock;
import aztech.modern_industrialization.pipes.impl.PipeBlockEntity;
import aztech.modern_industrialization.pipes.impl.PipeVoxelShape;
import dev.wp.phantoms_utilities.PUComponents;
import dev.wp.phantoms_utilities.PUConfig;
import dev.wp.phantoms_utilities.PUSounds;
import dev.wp.phantoms_utilities.PUTags;
import dev.wp.phantoms_utilities.helpers.IMouseWheelItem;
import dev.wp.phantoms_utilities.mixin.PipeBlockEntityAccessor;
import dev.wp.phantoms_utilities.mixin.PipeNetworkNodeAccessor;
import dev.wp.phantoms_utilities.util.PUColor;
import dev.wp.phantoms_utilities.util.Utils;
import java.util.*;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SprayCan extends Item implements IMouseWheelItem {
    public SprayCan(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static void floodFillCables(Level level, BlockPos startPos, AEColor newColor, Direction side, Player player) {
        final int maxTotalChecks = PUConfig.maxTotalChecks;
        final int maxBlocks = PUConfig.maxCableDyeCount;

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
        final int maxTotalChecks = PUConfig.maxTotalChecks;
        final int maxBlocks = PUConfig.maxBlockDyeCount;

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
        BlockState currentState = level.getBlockState(pos);
        if (!currentState.equals(originalState)) return false;

        CompoundTag data = null;
        if (level.getBlockEntity(pos) instanceof BlockEntity be) {
            data = be.saveWithFullMetadata(level.registryAccess());
        }

        level.setBlockAndUpdate(pos, newState);

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
    return PUConfig.blacklistedMods.contains(blockId.getNamespace());
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
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(originalState.getBlock());
        ResourceLocation toBeId = Utils.getRecoloredBlockID(blockId, color);

        if (blockId.equals(toBeId)) return InteractionResult.PASS;

        if (BuiltInRegistries.BLOCK.containsKey(toBeId)) {
            Block newBlock = BuiltInRegistries.BLOCK.get(toBeId);
            BlockState newState = newBlock.defaultBlockState();
            for (Property<?> property : originalState.getProperties()) {
                newState = Utils.copyProperties(originalState, newState, property);
            }

            if (!level.isClientSide()) {
                if (player.isShiftKeyDown()) floodFillBlocks(level, pos, originalState, newState, player);
                else processBlockPos(level, pos, originalState, newState);
            }
            playSound(player, pos, PUSounds.SPRAY_CAN_SPRAY, level);
            return InteractionResult.sidedSuccess(player.level().isClientSide());
//        } else informPlayer(player, "No block found with ID: " + toBeId);
        }
        return InteractionResult.FAIL;
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
