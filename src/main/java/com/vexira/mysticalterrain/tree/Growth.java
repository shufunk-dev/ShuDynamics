package com.vexira.mysticalterrain.tree;

public record Growth(
   int lobes,
   double spread,
   double flat,
   double tiers,
   double lopside,
   double bends,
   double swing,
   int forks,
   double split,
   double sprout,
   double tropism,
   double twig,
   double drip,
   double hollow,
   double buttress,
   double jitter
) {
   public static final Growth DEFAULT = new Growth(5, 0.5, 0.5, 0.5, 0.2, 0.5, 0.3, 1, 0.6, 0.4, 0.1, 1.0, 0.3, 0.5, 0.3, 0.3);
}
