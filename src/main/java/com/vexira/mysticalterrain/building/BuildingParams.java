package com.vexira.mysticalterrain.building;

public record BuildingParams(BuildingKind kind, int size, int floors, double decay, long seed) {
}
