package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeDecoder;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.kneelawk.graphlib.api.wire.FullWireBlockNode;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public record GearboxNode() implements MechanicalNode {
    public static Identifier ID = ModInit.id("gearbox");

    public static final BlockNodeDecoder DECODER = new BlockNodeDecoder() {
        @Override
        public @Nullable BlockNode decode(@Nullable NbtElement tag) {
            return new GearboxNode();
        }
    };

    @Override
    public @NotNull Identifier getTypeId() {
        return ID;
    }

    @Override
    public @Nullable NbtElement toTag() {
        return null;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();

        for (var dir : Direction.values()) {
            self.getGraphWorld().getNodesAt(self.getPos().offset(dir))
                    .filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x)).forEach(list::add);
        }
        return list;
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return MechanicalNode.super.canConnect(self, other);
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }
}
