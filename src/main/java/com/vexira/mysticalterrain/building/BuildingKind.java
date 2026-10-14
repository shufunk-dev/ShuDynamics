package com.vexira.mysticalterrain.building;

public enum BuildingKind {
   TOWER,
   KEEP,
   TEMPLE,
   ZIGGURAT,
   CRYPT,
   HAMLET,
   SHRINE,
   WATCHTOWER,
   OBSERVATORY,
   LIBRARY,
   FORGE,
   ARENA,
   MAZE,
   OUTPOST,
   VAULT,
   GRAVEYARD,
   LIGHTHOUSE,
   LAIR_FORGE,
   LAIR_GLACIER,
   LAIR_PALACE,
   LAIR_NEST,
   LAIR_CATHEDRAL,
   LAIR_SHRINE,
   LAIR_PYRAMID,
   LAIR_SPORECAP,
   LAIR_OBSERVATORY,
   LAIR_SPIRE;

   public boolean lair() {
      return this.name().startsWith("LAIR_");
   }
}
