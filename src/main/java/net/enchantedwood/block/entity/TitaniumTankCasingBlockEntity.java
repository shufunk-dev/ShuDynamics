package net.enchantedwood.block.entity;

import net.enchantedwood.fluid.LavaProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class TitaniumTankCasingBlockEntity extends BlockEntity implements LavaProvider, NamedScreenHandlerFactory {
    private BlockPos masterPos = null;

    public TitaniumTankCasingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TITANIUM_TANK_CASING_BLOCK_ENTITY, pos, state);
    }

    public void setMasterPos(@Nullable BlockPos pos) {
        this.masterPos = pos;
        markDirty();
    }

    public @Nullable BlockPos getMasterPos() {
        return this.masterPos;
    }

    public @Nullable TitaniumTankControllerBlockEntity getMaster() {
        if (this.masterPos != null && this.world != null) {
            BlockEntity be = this.world.getBlockEntity(this.masterPos);
            if (be instanceof TitaniumTankControllerBlockEntity controller && controller.isFormed()) {
                return controller;
            }
        }
        return null;
    }

    public boolean isValidOutboundPort() {
        TitaniumTankControllerBlockEntity master = getMaster();
        if (master == null || !master.isFormed()) return false;
        BlockPos min = master.getMinPos();
        if (min == null) return false;

        int rx = this.pos.getX() - min.getX();
        int ry = this.pos.getY() - min.getY();
        int rz = this.pos.getZ() - min.getZ();

        // Only bottom layer (ry == 0) and not the 4 corners
        if (ry != 0) return false;
        boolean isCornerX = (rx == 0 || rx == 4);
        boolean isCornerZ = (rz == 0 || rz == 4);
        return !(isCornerX && isCornerZ);
    }

    // Outbound Lava Provider logic: delegates to master controller
    @Override
    public int getLavaAmount() {
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.getLavaAmount() : 0;
    }

    @Override
    public int getMaxLava() {
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.getMaxLava() : 0;
    }

    @Override
    public int insertLava(int amount, boolean simulate) {
        // Outer casings are OUTBOUND only; inbound must go through the top inbound port
        return 0;
    }

    @Override
    public int extractLava(int amount, boolean simulate) {
        if (!isValidOutboundPort()) return 0;
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.extractLava(amount, simulate) : 0;
    }

    @Override
    public boolean canInsertLava() {
        return false; // Casings only output lava
    }

    @Override
    public boolean canExtractLava() {
        return isValidOutboundPort() && getMaster() != null && getMaster().canExtractLava();
    }

    @Override
    public Text getDisplayName() {
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.getDisplayName() : Text.translatable("container.enchantedwood.titanium_tank");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory, PlayerEntity player) {
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.createMenu(syncId, playerInventory, player) : null;
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (this.masterPos != null) {
            view.putInt("MasterX", this.masterPos.getX());
            view.putInt("MasterY", this.masterPos.getY());
            view.putInt("MasterZ", this.masterPos.getZ());
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        if (view.contains("MasterX") && view.contains("MasterY") && view.contains("MasterZ")) {
            this.masterPos = new BlockPos(view.getInt("MasterX", 0), view.getInt("MasterY", 0), view.getInt("MasterZ", 0));
        } else {
            this.masterPos = null;
        }
    }
}
