package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.polyfactory.display.LodElementHolder;
import eu.pb4.polyfactory.display.LodItemDisplayElement;
import eu.pb4.polyfactory.item.tool.WrenchItem;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.MovingItemProvider;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;


public class FunnelBlock extends Block implements PolymerBlock, MovingItemConsumer, MovingItemProvider, WrenchItem.Wrenchable, BlockEntityProvider, BlockWithElementHolder {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty ENABLED = Properties.ENABLED;
    public static final EnumProperty<ConveyorLikeDirectional.TransferMode> MODE = EnumProperty.of("mode", ConveyorLikeDirectional.TransferMode.class,
            ConveyorLikeDirectional.TransferMode.FROM_CONVEYOR, ConveyorLikeDirectional.TransferMode.TO_CONVEYOR);

    public FunnelBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(ENABLED, true));
        Model.MODEL_OUT.isEmpty();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE, ENABLED);
    }

    @Override
    public boolean pushItemTo(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        var selfState = self.getBlockState();
        if (!selfState.get(ENABLED)) {
            return false;
        }

        var selfDir = selfState.get(FACING);
        var mode = selfState.get(MODE);
        if (!mode.fromConveyor || relative != Direction.UP || selfDir.getOpposite() == pushDirection || conveyor.movementDelta() < (selfDir == pushDirection ? 0.90 : 0.48) || selfDir.getAxis() == Direction.Axis.Y) {
            return false;
        }
        var be = self.getBlockEntity();
        if (be instanceof FunnelBlockEntity funnelBlockEntity && !funnelBlockEntity.matches(conveyor.getContainer().get())) {
            return false;
        }
        var stack = conveyor.getContainer();

        if (FactoryUtil.tryInserting(self.getWorld(), self.getPos().offset(selfState.get(FACING)), stack.get(), selfDir.getOpposite()) == -1) {
            return selfDir.getAxis() == pushDirection.getAxis();
        }

        if (stack.get().isEmpty()) {
            conveyor.clearContainer();
        }


        return selfDir.getAxis() == pushDirection.getAxis();
    }


    @Override
    public void getItemFrom(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        if (relative != Direction.DOWN || !conveyor.isContainerEmpty()) {
            return;
        }

        var selfState = self.getBlockState();
        var mode = selfState.get(MODE);
        var selfFacing = selfState.get(FACING);
        if (!selfState.get(ENABLED) || !mode.toConveyor || pushDirection == selfFacing) {
            return;
        }

        var inv = HopperBlockEntity.getInventoryAt(self.getWorld(), self.getPos().offset(selfFacing));
        var sided = inv instanceof SidedInventory s ? s : null;
        var be = self.getBlockEntity();
        if (inv != null && be instanceof FunnelBlockEntity funnelBlockEntity) {
            for (var i = 0; i < inv.size(); i++) {
                var stack = inv.getStack(i);
                if (!stack.isEmpty() && funnelBlockEntity.matches(stack) && (sided == null || sided.canExtract(i, stack, selfFacing.getOpposite()))) {
                    if (conveyor.pushNew(stack)) {
                        inv.markDirty();
                        if (stack.isEmpty()) {
                            inv.setStack(i, ItemStack.EMPTY);
                        }
                        conveyor.setMovementPosition(pushDirection.getOpposite() == selfFacing ? 0.15 : 0.5);
                        return;
                    }
                }
            }
        }

    }

    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
    }

    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        this.updateEnabled(world, pos, state);
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }

    private void updateEnabled(World world, BlockPos pos, BlockState state) {
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered == state.get(ENABLED)) {
            world.setBlockState(pos, state.with(ENABLED, !powered), 4);
        }
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (ctx.getSide() == Direction.DOWN) {
            this.getDefaultState().with(FACING, Direction.UP).with(MODE, ConveyorLikeDirectional.TransferMode.TO_CONVEYOR);
        }

        var dir = ctx.getSide().getOpposite();

        if (dir == Direction.DOWN) {
            dir = ctx.getHorizontalPlayerFacing();
        }

        var selfPos = ctx.getBlockPos();
        if (ctx.getSide() != Direction.UP) {
            selfPos = selfPos.offset(ctx.getSide());
        }

        selfPos = selfPos.down();
        var below = ctx.getWorld().getBlockState(selfPos);
        var mode = below.getBlock() instanceof ConveyorLikeDirectional directional
                ? directional.getTransferMode(below, dir.getOpposite())
                : ConveyorLikeDirectional.TransferMode.TO_CONVEYOR;
        return this.getDefaultState().with(FACING, dir).with(MODE, mode);
    }

    @Override
    public ActionResult useWithWrench(ItemUsageContext context) {
        var be = context.getWorld().getBlockEntity(context.getBlockPos());
        if (be instanceof FunnelBlockEntity funnelBlockEntity) {
            funnelBlockEntity.setFilter(context.getPlayer().getStackInHand(context.getHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND));
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void attackWithWrench(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        var state = world.getBlockState(pos);

        world.setBlockState(pos, state.with(MODE,
                state.get(MODE) == ConveyorLikeDirectional.TransferMode.TO_CONVEYOR
                ? ConveyorLikeDirectional.TransferMode.FROM_CONVEYOR
                : ConveyorLikeDirectional.TransferMode.TO_CONVEYOR
        ));
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.IRON_TRAPDOOR;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FunnelBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    public final class Model extends LodElementHolder {
        private static final ItemStack MODEL_IN = new ItemStack(Items.CANDLE);
        private static final ItemStack MODEL_OUT = new ItemStack(Items.CANDLE);
        private final Matrix4f mat = new Matrix4f();
        private final ItemDisplayElement mainElement;
        final ItemDisplayElement filterElement;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.mainElement = new LodItemDisplayElement();
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.filterElement = new LodItemDisplayElement();
            this.filterElement.setDisplaySize(1, 1);
            this.filterElement.setModelTransformation(ModelTransformationMode.GUI);
            this.filterElement.setViewRange(0.1f);
            this.updateFacing(state);
            this.addElement(this.mainElement);
            this.addElement(this.filterElement);
        }

        private void updateFacing(BlockState facing) {
            var rot = facing.get(FACING).getOpposite().getRotationQuaternion().mul(Direction.NORTH.getRotationQuaternion());
            mat.identity();
            mat.rotate(rot);
            mat.scale(2f);
            var outModel = facing.get(MODE) == ConveyorLikeDirectional.TransferMode.FROM_CONVEYOR;

            this.mainElement.setItem(outModel ? MODEL_OUT : MODEL_IN);
            this.mainElement.setTransformation(mat);


            mat.identity();
            mat.rotate(rot).rotateY(MathHelper.PI);
            if (outModel) {
                mat.rotateX(22.5f * MathHelper.RADIANS_PER_DEGREE);
                mat.translate(0, 0.50f, 0.008f);
            } else {
                mat.translate(0, 0.555f, 0.025f);
            }
            mat.scale(0.4f, 0.4f, 0.005f);
            this.filterElement.setTransformation(mat);
            this.tick();
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(BlockBoundAttachment.get(this).getBlockState());
            }
        }



        static {
            MODEL_IN.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(Items.CANDLE, FactoryUtil.id("block/funnel_in")).value());
            MODEL_OUT.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(Items.CANDLE, FactoryUtil.id("block/funnel_out")).value());
        }
    }

}
