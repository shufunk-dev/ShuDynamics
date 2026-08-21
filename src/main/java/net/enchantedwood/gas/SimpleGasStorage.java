package net.enchantedwood.gas;

import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

public class SimpleGasStorage implements GasStorage {
    private GasType gasType = GasType.NONE;
    private int amount = 0;
    private final int capacity;
    private final int maxTransfer;

    public SimpleGasStorage(int capacity, int maxTransfer) {
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
    }

    @Override
    public GasType getGasType() {
        return this.amount > 0 ? this.gasType : GasType.NONE;
    }

    @Override
    public int getAmount() {
        return this.amount;
    }

    @Override
    public int getCapacity() {
        return this.capacity;
    }

    public void setGas(GasType type, int amount) {
        this.gasType = type;
        this.amount = Math.max(0, Math.min(amount, this.capacity));
        if (this.amount == 0) {
            this.gasType = GasType.NONE;
        }
    }

    @Override
    public int insertGas(GasType type, int insertAmount, boolean simulate) {
        if (type == GasType.NONE || insertAmount <= 0) return 0;
        if (this.amount > 0 && this.gasType != type) return 0;

        int space = this.capacity - this.amount;
        int insertable = Math.min(space, Math.min(insertAmount, this.maxTransfer));
        if (!simulate && insertable > 0) {
            this.gasType = type;
            this.amount += insertable;
        }
        return insertable;
    }

    @Override
    public int extractGas(GasType type, int extractAmount, boolean simulate) {
        if (this.amount <= 0 || extractAmount <= 0) return 0;
        if (type != GasType.NONE && this.gasType != type) return 0;

        int extractable = Math.min(this.amount, Math.min(extractAmount, this.maxTransfer));
        if (!simulate && extractable > 0) {
            this.amount -= extractable;
            if (this.amount == 0) {
                this.gasType = GasType.NONE;
            }
        }
        return extractable;
    }

    @Override
    public boolean canExtract(GasType type) {
        return this.amount > 0 && (type == GasType.NONE || this.gasType == type);
    }

    @Override
    public boolean canInsert(GasType type) {
        return type != GasType.NONE && (this.amount == 0 || this.gasType == type) && this.amount < this.capacity;
    }

    public void readData(ReadView view, String prefix) {
        String typeName = view.getString(prefix + "_GasType", "none");
        for (GasType t : GasType.values()) {
            if (t.asString().equalsIgnoreCase(typeName)) {
                this.gasType = t;
                break;
            }
        }
        this.amount = view.getInt(prefix + "_GasAmount", 0);
        if (this.amount <= 0) {
            this.gasType = GasType.NONE;
        }
    }

    public void writeData(WriteView view, String prefix) {
        view.putString(prefix + "_GasType", this.gasType.asString());
        view.putInt(prefix + "_GasAmount", this.amount);
    }
}
