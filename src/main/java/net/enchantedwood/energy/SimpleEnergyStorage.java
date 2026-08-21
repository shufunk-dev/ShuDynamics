package net.enchantedwood.energy;

import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

public class SimpleEnergyStorage implements EnergyStorage {
    protected int energy;
    protected final int maxEnergy;
    protected final int maxInsert;
    protected final int maxExtract;

    public SimpleEnergyStorage(int maxEnergy, int maxTransfer) {
        this(maxEnergy, maxTransfer, maxTransfer, 0);
    }

    public SimpleEnergyStorage(int maxEnergy, int maxInsert, int maxExtract, int initialEnergy) {
        this.maxEnergy = maxEnergy;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.energy = Math.max(0, Math.min(initialEnergy, maxEnergy));
    }

    @Override
    public int getEnergy() {
        return this.energy;
    }

    @Override
    public int getMaxEnergy() {
        return this.maxEnergy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, this.maxEnergy));
    }

    @Override
    public int insertEnergy(int amount, boolean simulate) {
        if (!canInsert() || amount <= 0) return 0;
        int insertable = Math.min(amount, Math.min(this.maxInsert, this.maxEnergy - this.energy));
        if (!simulate) {
            this.energy += insertable;
        }
        return insertable;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        if (!canExtract() || amount <= 0) return 0;
        int extractable = Math.min(amount, Math.min(this.maxExtract, this.energy));
        if (!simulate) {
            this.energy -= extractable;
        }
        return extractable;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    @Override
    public boolean canInsert() {
        return this.maxInsert > 0;
    }

    @Override
    public int getTransferRate() {
        return Math.max(this.maxInsert, this.maxExtract);
    }

    public void readData(ReadView view) {
        this.energy = view.getInt("StoredEnergy", 0);
        if (this.energy > this.maxEnergy) {
            this.energy = this.maxEnergy;
        }
    }

    public void writeData(WriteView view) {
        view.putInt("StoredEnergy", this.energy);
    }
}
