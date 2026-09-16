package net.enchantedwood.block.entity;

import net.enchantedwood.fluid.LavaProvider;
import net.enchantedwood.fluid.MoltenMetal;
import net.enchantedwood.fluid.MoltenMetalProvider;
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

import java.util.List;

public class TitaniumTankCasingBlockEntity extends BlockEntity implements LavaProvider, MoltenMetalProvider, NamedScreenHandlerFactory {
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
                BlockPos min = controller.getMinPos();
                if (min != null) {
                    int rx = this.pos.getX() - min.getX();
                    int ry = this.pos.getY() - min.getY();
                    int rz = this.pos.getZ() - min.getZ();
                    if (rx >= 0 && rx < 5 && ry >= 0 && ry < 5 && rz >= 0 && rz < 5) {
                        return controller;
                    }
                }
            }
        }
        // Self-healing: locate the true controller governing this casing's coordinates
        if (this.world != null) {
            TitaniumTankControllerBlockEntity controller = TitaniumTankControllerBlockEntity.findControllerForBlock(this.world, this.pos);
            if (controller != null && controller.isFormed()) {
                this.masterPos = controller.getPos();
                markDirty();
                return controller;
            }
        }
        return null;
    }

    public boolean isValidOutboundPort() {
        TitaniumTankControllerBlockEntity master = getMaster();
        if (master == null || !master.isFormed()) return false;
        return this.pos.getY() < master.getPos().getY();
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
        return 0; // Casings are outbound only; use Top Center Valve
    }

    @Override
    public int extractLava(int amount, boolean simulate) {
        if (!isValidOutboundPort()) return 0;
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.extractLavaInternal(amount, simulate) : 0;
    }

    @Override
    public boolean canInsertLava() {
        return false; // Casings are outbound only; use Top Center Valve
    }

    @Override
    public boolean canExtractLava() {
        return isValidOutboundPort() && getMaster() != null && getMaster().getLavaAmount() > 0 && getMaster().getFluidType() == MoltenMetal.LAVA;
    }

    // Molten Metal Provider logic: delegates to master controller
    @Override
    public MoltenMetal getFluidType() {
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.getFluidType() : MoltenMetal.NONE;
    }

    @Override
    public int getFluidAmount(MoltenMetal metal) {
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.getFluidAmount(metal) : 0;
    }

    @Override
    public int getMaxFluid() {
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.getMaxFluid() : 0;
    }

    @Override
    public int insertFluid(MoltenMetal metal, int amount, boolean simulate) {
        return 0; // Casings are outbound only; use Top Center Valve
    }

    @Override
    public boolean canInsertFluid(MoltenMetal metal) {
        return false; // Casings are outbound only; use Top Center Valve
    }

    @Override
    public int extractFluid(MoltenMetal metal, int amount, boolean simulate) {
        if (!isValidOutboundPort()) return 0;
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.extractFluidInternal(metal, amount, simulate) : 0;
    }

    @Override
    public boolean canExtractFluid(MoltenMetal metal) {
        return isValidOutboundPort() && getMaster() != null && getMaster().getFluidAmount(metal) > 0;
    }

    @Override
    public List<MoltenMetal> getContainedFluids() {
        TitaniumTankControllerBlockEntity master = getMaster();
        return master != null ? master.getContainedFluids() : List.of();
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
