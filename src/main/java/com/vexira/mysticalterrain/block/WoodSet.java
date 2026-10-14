package com.vexira.mysticalterrain.block;

import java.util.List;
import net.minecraft.block.Block;

public record WoodSet(
   Block log,
   Block wood,
   Block strippedLog,
   Block strippedWood,
   Block planks,
   Block stairs,
   Block slab,
   Block fence,
   Block fenceGate,
   Block door,
   Block trapdoor,
   boolean flammable
) {
   public List<Block> building() {
      return List.of(this.planks, this.stairs, this.slab, this.fence, this.fenceGate);
   }

   public List<Block> logs() {
      return List.of(this.log, this.wood, this.strippedLog, this.strippedWood);
   }
}
