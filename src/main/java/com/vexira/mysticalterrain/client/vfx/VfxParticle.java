package com.vexira.mysticalterrain.client.vfx;

import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.boss.vfx.VfxOptions;
import com.vexira.mysticalterrain.boss.vfx.VfxParticles;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class VfxParticle extends SingleQuadParticle {
   public static final ParticleRenderType GROUP = new ParticleRenderType("mysticalterrain:vfx", "mt_vfx");
   private static final int LEGACY = 8;
   private static final int K_FLARE = 8;
   private static final int K_SLASH = 9;
   private static final int K_SMOKE = 10;
   private static final int K_GLINT = 11;
   private static final int K_OPEN = 12;
   private static final int F_RING = 8;
   private static final int F_FLARE = 9;
   private static final int F_SPARK = 10;
   private static final int F_EMBER = 11;
   private static final int F_SMOKE = 12;
   private static final int F_SHARD = 13;
   private static final int F_GLINT = 14;
   private static final int F_SHOCK = 15;
   private static final int F_SCORCH = 16;
   private final VfxParticles.Kind kind;
   private final SpriteSet sprites;
   private final int mode;
   private final int curve;
   private final int fade;
   private final int delay;
   private final int life;
   private final int variant;
   private final boolean additive;
   private final boolean soft;
   private final float tumble;
   private float turn;
   private float oTurn;
   private final boolean mirror;
   private final boolean lie;
   private final boolean flutter;
   private final float half;
   private final float yawDeg;
   private final float pitchDeg;
   private final float planeRoll;
   private final float spin;
   private final float curl;
   private final float stretch;
   private final float phase;
   private final float r0;
   private final float g0;
   private final float b0;
   private final float r1;
   private final float g1;
   private final float b1;
   private final boolean shift;
   private final double sx;
   private final double sy;
   private final double sz;
   private final Quaternionf q = new Quaternionf();

   public VfxParticle(
      ClientLevel level, double x, double y, double z, double dx, double dy, double dz, VfxOptions o, VfxParticles.Kind kind, SpriteSet sprites
   ) {
      super(level, x, y, z, sprites.first());
      this.kind = kind;
      this.sprites = sprites;
      this.mode = o.mode() == 0 ? autoMode(kind) : o.mode();
      this.curve = o.curve() == 0 ? kindCurve(kind) : o.curve();
      this.fade = o.fade() == 0 ? kindFade(kind) : o.fade();
      this.delay = Math.max(0, o.delay());
      this.life = Math.max(2, o.life());
      this.lifetime = this.delay + this.life;
      this.half = Math.max(0.005F, o.scale() * 0.5F);
      this.quadSize = this.half;
      this.yawDeg = o.yaw();
      this.pitchDeg = o.pitch();
      this.phase = this.random.nextFloat() * (float) (Math.PI * 2);
      int c = kind == VfxParticles.Kind.SHARD ? lift(o.color()) : o.color();
      this.r0 = (c >> 16 & 0xFF) / 255.0F;
      this.g0 = (c >> 8 & 0xFF) / 255.0F;
      this.b0 = (c & 0xFF) / 255.0F;
      this.shift = o.color2() >= 0;
      int c2 = this.shift ? (kind == VfxParticles.Kind.SHARD ? lift(o.color2()) : o.color2()) : c;
      this.r1 = (c2 >> 16 & 0xFF) / 255.0F;
      this.g1 = (c2 >> 8 & 0xFF) / 255.0F;
      this.b1 = (c2 & 0xFF) / 255.0F;
      this.setColor(this.r0, this.g0, this.b0);
      if (this.mode == 6) {
         this.sx = dx;
         this.sy = dy;
         this.sz = dz;
         this.xd = this.yd = this.zd = 0.0;
      } else {
         this.sx = this.sy = this.sz = 0.0;
         this.xd = dx;
         this.yd = dy;
         this.zd = dz;
      }

      this.hasPhysics = this.mode != 6 && (o.has(512) || o.mode() == 0 && (kind == VfxParticles.Kind.SHARD || kind == VfxParticles.Kind.SPARK));
      float var10001;
      if (Float.isNaN(o.gravity())) {
         switch (kind) {
            case SPARK:
               var10001 = 0.55F;
               break;
            case SHARD:
               var10001 = 1.1F;
               break;
            case SMOKE:
               var10001 = -0.02F;
               break;
            case EMBER:
               var10001 = -0.04F;
               break;
            case DEBRIS:
               var10001 = 0.35F;
               break;
            default:
               var10001 = 0.0F;
         }
      } else {
         var10001 = o.gravity();
      }

      this.gravity = var10001;
      if (Float.isNaN(o.drag())) {
         switch (kind) {
            case SPARK:
               var10001 = 0.9F;
               break;
            case SHARD:
               var10001 = 0.96F;
               break;
            case SMOKE:
            case EMBER:
               var10001 = 0.94F;
               break;
            case DEBRIS:
               var10001 = 0.93F;
               break;
            default:
               var10001 = 1.0F;
         }
      } else {
         var10001 = o.drag();
      }

      this.friction = var10001;
      float s = o.spin();
      if (s == 0.0F) {
         s = switch (kind) {
            case SHARD -> (this.random.nextFloat() - 0.5F) * 0.8F;
            case SMOKE -> (this.random.nextFloat() - 0.5F) * 0.05F;
            default -> 0.0F;
            case DEBRIS -> (this.random.nextFloat() - 0.5F) * 0.3F;
         };
      }

      this.spin = s;
      this.curl = o.curl();
      this.stretch = o.stretch();
      this.mirror = o.has(1024);
      this.lie = o.has(2048);
      float startRoll = o.roll();
      if (this.mode == 4) {
         this.planeRoll = Float.isNaN(startRoll) ? 0.0F : startRoll;
         this.roll = 0.0F;
      } else {
         this.planeRoll = 0.0F;
         boolean anyAngle = this.mode == 1 || this.mode == 2;
         this.roll = Float.isNaN(startRoll) ? (anyAngle ? this.random.nextFloat() * (float) (Math.PI * 2) : 0.0F) : startRoll * (float) (Math.PI / 180.0);
      }

      this.oRoll = this.roll;
      this.additive = kind.glow && VfxLayers.additiveUsable();
      this.soft = (kind == VfxParticles.Kind.SMOKE || kind == VfxParticles.Kind.SCORCH) && VfxLayers.softUsable();
      this.tumble = kind == VfxParticles.Kind.SHARD ? (0.18F + this.random.nextFloat() * 0.3F) * (this.random.nextBoolean() ? 1 : -1) : 0.0F;
      this.turn = this.oTurn = this.random.nextFloat() * (float) (Math.PI * 2);
      int v = o.variant() >= 0 ? Math.min(o.variant(), kind.variants - 1) : this.random.nextInt(kind.variants);
      this.variant = kind == VfxParticles.Kind.SMOKE ? 0 : v;
      this.flutter = kind == VfxParticles.Kind.DEBRIS && this.variant != Vfx.Debris.BONE.ordinal();
      if (kind == VfxParticles.Kind.SMOKE) {
         this.setSprite(sprites.get(0, this.life));
      } else {
         int count = kind.glow ? kind.variants * 2 : kind.variants;
         int idx = kind.glow && !this.additive ? this.variant + kind.variants : this.variant;
         this.setSprite(count <= 1 ? sprites.first() : sprites.get(idx, count - 1));
      }
   }

   private static int lift(int c) {
      int r = c >> 16 & 0xFF;
      int g = c >> 8 & 0xFF;
      int b = c & 0xFF;
      int add = 107 - Math.max(r, Math.max(g, b));
      return add <= 0 ? c & 16777215 : r + add << 16 | g + add << 8 | b + add;
   }

   private static int autoMode(VfxParticles.Kind kind) {
      return switch (kind) {
         case RING, SHOCK, RUNE, CRACK, SCORCH, TELEGRAPH -> 2;
         case SLASH -> 8;
         case BEAM, BOLT -> 6;
         default -> 1;
      };
   }

   private static int kindCurve(VfxParticles.Kind kind) {
      return switch (kind) {
         case SPARK -> 2;
         default -> 5;
         case SMOKE -> 10;
         case RING, SHOCK -> 6;
         case RUNE, CRACK, HEX -> 12;
         case SLASH -> 9;
         case FLARE -> 8;
         case GLINT -> 11;
         case WISP -> 3;
      };
   }

   private static int kindFade(VfxParticles.Kind kind) {
      return switch (kind) {
         case SPARK -> 10;
         case SHARD -> 13;
         case SMOKE -> 12;
         case EMBER -> 11;
         case DEBRIS, CRACK -> 2;
         case RING -> 8;
         case SHOCK -> 15;
         case RUNE, WISP, HEX -> 3;
         case SCORCH -> 16;
         case TELEGRAPH -> 5;
         case SLASH, BEAM -> 1;
         case BOLT -> 4;
         case FLARE -> 9;
         case GLINT -> 14;
         default -> 1;
      };
   }

   public void tick() {
      if (this.age < this.delay) {
         this.age++;
         this.xo = this.x;
         this.yo = this.y;
         this.zo = this.z;
         this.oRoll = this.roll;
         this.oTurn = this.turn;
      } else {
         if (this.curl != 0.0F) {
            float c = Mth.cos(this.curl);
            float s = Mth.sin(this.curl);
            double nx = this.xd * c - this.zd * s;
            this.zd = this.xd * s + this.zd * c;
            this.xd = nx;
         }

         if (this.flutter && !this.onGround) {
            this.xd = this.xd + Mth.sin(this.age * 0.25F + this.phase) * 0.004F;
            this.zd = this.zd + Mth.cos(this.age * 0.21F + this.phase) * 0.004F;
         }

         this.oRoll = this.roll;
         this.oTurn = this.turn;
         super.tick();
         if (this.kind != VfxParticles.Kind.SHARD || !this.onGround) {
            this.roll = this.roll + this.spin;
            this.turn = this.turn + this.tumble;
         }

         if (this.flutter && !this.onGround) {
            this.roll = this.roll + Mth.sin(this.age * 0.3F + this.phase) * 0.06F;
         }

         if (this.kind == VfxParticles.Kind.SMOKE && !this.removed) {
            this.setSprite(this.sprites.get(Math.min(this.age - this.delay, this.life), this.life));
         }
      }
   }

   private float size(float t, float time) {
      return switch (this.curve) {
         case 1 -> 0.2F + 0.8F * (1.0F - (1.0F - t) * (1.0F - t));
         case 2 -> 1.0F - t;
         case 3 -> 1.0F + 0.12F * Mth.sin(time * 0.45F + this.phase);
         case 4 -> t < 0.12F ? 1.15F * t / 0.12F : 1.15F - 0.15F * Math.min(1.0F, (t - 0.12F) / 0.2F);
         default -> 1.0F;
         case 6 -> 0.15F + 0.85F * (1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t));
         case 7 -> t;
         case 8 -> 0.55F + 0.45F * Math.min(1.0F, t / 0.15F);
         case 9 -> 0.7F + 0.3F * (1.0F - (1.0F - t) * (1.0F - t));
         case 10 -> 0.7F + 0.8F * t;
         case 11 -> (float)Math.pow(Math.max(0.0F, Mth.sin((float) Math.PI * Math.min(1.0F, t * 1.1F))), 0.7);
         case 12 -> t < 0.1F ? 0.6F + 0.4F * (1.0F - (1.0F - t / 0.1F) * (1.0F - t / 0.1F)) : 1.0F;
      };
   }

   private float bright(float t, float time) {
      return switch (this.fade) {
         case 1 -> 1.0F - t;
         case 2 -> t < 0.7F ? 1.0F : (1.0F - t) / 0.3F;
         case 3 -> Math.min(1.0F, t / 0.12F) * Math.min(1.0F, (1.0F - t) / 0.3F);
         case 4 -> (0.55F + 0.45F * Mth.sin(time * 2.3F + this.phase) * Mth.cos(time * 0.9F)) * Mth.sqrt(1.0F - t);
         case 5 -> Math.min(1.0F, t / 0.1F);
         default -> 1.0F;
         case 8 -> (1.0F - t) * (1.0F - t);
         case 9 -> t < 0.15F ? 1.0F : (1.0F - t) / 0.85F;
         case 10 -> 1.0F - t * t;
         case 11 -> Math.min(1.0F, (1.0F - t) * 1.5F) * (0.65F + 0.35F * Mth.sin(time * 0.9F + this.half * 80.0F));
         case 12 -> 0.55F * (1.0F - t) * Math.min(1.0F, t * 6.0F);
         case 13 -> t > 0.8F ? (1.0F - t) / 0.2F : 1.0F;
         case 14 -> 1.0F - t * t * t;
         case 15 -> (float)Math.pow(1.0F - t, 1.5);
         case 16 -> 0.85F * (t < 0.6F ? 1.0F : (1.0F - t) / 0.4F);
      };
   }

   boolean visible(Frustum frustum) {
      if (this.age < this.delay) {
         return false;
      }

      double r = this.half * 1.3 + 0.25;
      if (this.mode == 6) {
         return frustum.isVisible(new AABB(this.x, this.y, this.z, this.x + this.sx, this.y + this.sy, this.z + this.sz).inflate(r));
      }

      if (this.mode == 5) {
         r += Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd) * this.stretch;
      }

      return frustum.isVisible(new AABB(this.x - r, this.y - r, this.z - r, this.x + r, this.y + r, this.z + r));
   }

   public void extract(QuadParticleRenderState submittable, Camera camera, float tickProgress) {
      if (this.age >= this.delay) {
         float time = this.age - this.delay + tickProgress;
         float t = Mth.clamp(time / this.life, 0.0F, 1.0F);
         float b = Mth.clamp(this.bright(t, time), 0.0F, 1.0F);
         if (!(b <= 0.004F)) {
            Vec3 cam = camera.position();
            float px = (float)(Mth.lerp(tickProgress, this.xo, this.x) - cam.x);
            float py = (float)(Mth.lerp(tickProgress, this.yo, this.y) - cam.y);
            float pz = (float)(Mth.lerp(tickProgress, this.zo, this.z) - cam.z);
            float c = this.size(t, time);
            float size = this.half * c;
            if (this.kind == VfxParticles.Kind.SMOKE || this.kind == VfxParticles.Kind.SHARD || this.kind == VfxParticles.Kind.DEBRIS) {
               float dist = Mth.sqrt(px * px + py * py + pz * pz);
               if (this.kind == VfxParticles.Kind.SMOKE) {
                  b *= Mth.clamp((dist - 0.3F) / (0.8F + size), 0.0F, 1.0F);
                  if (b <= 0.004F) {
                     return;
                  }
               } else {
                  if (dist < 0.3F) {
                     return;
                  }

                  size = Math.min(size, dist * 0.07F);
               }
            }

            float r = this.r0;
            float g = this.g0;
            float bl = this.b0;
            if (this.shift) {
               float k = Math.min(1.0F, t / 0.6F);
               r = Mth.lerp(k, this.r0, this.r1);
               g = Mth.lerp(k, this.g0, this.g1);
               bl = Mth.lerp(k, this.b0, this.b1);
            }

            int color;
            if (!this.kind.glow) {
               color = ARGB.colorFromFloat(b, r, g, bl);
            } else if (this.additive) {
               color = ARGB.colorFromFloat(1.0F, r * b, g * b, bl * b);
            } else {
               float m = Math.max(r, Math.max(g, bl));
               if (m < 0.004F) {
                  return;
               }

               color = ARGB.colorFromFloat(b * Math.min(1.0F, m), r / m, g / m, bl / m);
            }

            int light = this.getLightCoords(tickProgress);
            float ang = Mth.lerp(tickProgress, this.oRoll, this.roll);
            Quaternionf q = this.q;
            if (this.kind == VfxParticles.Kind.SHARD && this.mode == 1) {
               float across = Mth.cos(Mth.lerp(tickProgress, this.oTurn, this.turn));
               q.set(camera.rotation());
               if (ang != 0.0F) {
                  q.rotateZ(ang);
               }

               this.quad(submittable, q, px, py, pz, size, size * Math.max(0.25F, Math.abs(across)), color, light, false, across < 0.0F);
            } else {
               switch (this.mode) {
                  case 2:
                     q.rotationY((float) Math.PI - this.yawDeg * (float) (Math.PI / 180.0) - ang).rotateX((float) (-Math.PI / 2));
                     this.quad(submittable, q, px, py, pz, size, size, color, light, true);
                     break;
                  case 3:
                     facing(q, this.yawDeg, this.pitchDeg);
                     if (ang != 0.0F) {
                        q.rotateZ(ang);
                     }

                     this.quad(submittable, q, px, py, pz, size, size, color, light, true);
                     break;
                  case 4:
                     plane(q, this.yawDeg, this.pitchDeg, this.planeRoll);
                     if (ang != 0.0F) {
                        q.rotateZ(ang);
                     }

                     this.quad(submittable, q, px, py, pz, size, size, color, light, true);
                     break;
                  case 5: {
                     float speed = (float)Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
                     if (speed < 0.001F) {
                        this.faceCamera(submittable, camera, px, py, pz, size, ang, color, light);
                        return;
                     }

                     Vector3f axis = new Vector3f((float)this.xd, (float)this.yd, (float)this.zd).div(speed);
                     float halfLen = size + speed * this.stretch * 0.5F;
                     float back = halfLen - size;
                     float cx = px - axis.x * back;
                     float cy = py - axis.y * back;
                     float cz = pz - axis.z * back;
                     alongAxis(q, axis, cx, cy, cz);
                     this.quad(submittable, q, cx, cy, cz, halfLen, size, color, light, false);
                     break;
                  }
                  case 6: {
                     float len = (float)Math.sqrt(this.sx * this.sx + this.sy * this.sy + this.sz * this.sz);
                     if (len < 0.001F) {
                        this.faceCamera(submittable, camera, px, py, pz, this.half, ang, color, light);
                        return;
                     }

                     Vector3f axis = new Vector3f((float)this.sx, (float)this.sy, (float)this.sz).div(len);
                     float halfLen = len * c * 0.5F;
                     float cx = px + axis.x * halfLen;
                     float cy = py + axis.y * halfLen;
                     float cz = pz + axis.z * halfLen;
                     if (this.lie && lying(q, axis)) {
                        this.quad(submittable, q, cx, cy, cz, halfLen, this.half, color, light, true);
                     } else {
                        alongAxis(q, axis, cx, cy, cz);
                        this.quad(submittable, q, cx, cy, cz, halfLen, this.half, color, light, false);
                     }
                     break;
                  }
                  case 7:
                     Quaternionf cr = camera.rotation();
                     if (cr.y * cr.y + cr.w * cr.w < 1.0E-6F) {
                        q.set(cr);
                     } else {
                        q.set(0.0F, cr.y, 0.0F, cr.w).normalize();
                     }

                     if (ang != 0.0F) {
                        q.rotateZ(ang);
                     }

                     this.quad(submittable, q, px, py, pz, size, size, color, light, false);
                     break;
                  case 8:
                     q.rotationY(-this.yawDeg * (float) (Math.PI / 180.0)).rotateX(-1.2207963F);
                     this.quad(submittable, q, px, py, pz, size, size, color, light, true);
                     break;
                  default:
                     this.faceCamera(submittable, camera, px, py, pz, size, ang, color, light);
               }
            }
         }
      }
   }

   private void faceCamera(QuadParticleRenderState state, Camera camera, float x, float y, float z, float size, float ang, int color, int light) {
      this.q.set(camera.rotation());
      if (ang != 0.0F) {
         this.q.rotateZ(ang);
      }

      this.quad(state, this.q, x, y, z, size, size, color, light, false);
   }

   private void quad(
      QuadParticleRenderState state, Quaternionf q, float x, float y, float z, float halfLen, float halfWid, int color, int light, boolean twoSided
   ) {
      this.quad(state, q, x, y, z, halfLen, halfWid, color, light, twoSided, false);
   }

   private void quad(
      QuadParticleRenderState state,
      Quaternionf q,
      float x,
      float y,
      float z,
      float halfLen,
      float halfWid,
      int color,
      int light,
      boolean twoSided,
      boolean flipV
   ) {
      if (!(halfLen <= 1.0E-4F) && !(halfWid <= 1.0E-4F)) {
         boolean flipU = this.mirror;
         if (twoSided && !this.additive && !this.soft) {
            Vector3f n = q.transform(new Vector3f(0.0F, 0.0F, 1.0F));
            if (n.x * -x + n.y * -y + n.z * -z < 0.0F) {
               q.rotateY((float) Math.PI);
               flipU = !flipU;
            }
         }

         float k = (float)Math.sqrt(halfLen / halfWid);
         float u0 = this.getU0();
         float u1 = this.getU1();
         if (flipU) {
            float tmp = u0;
            u0 = u1;
            u1 = tmp;
         }

         float v0 = flipV ? this.getV1() : this.getV0();
         float v1 = flipV ? this.getV0() : this.getV1();
         state.add(this.getLayer(), x, y, z, q.x * k, q.y * k, q.z * k, q.w * k, halfWid, u0, u1, v0, v1, color, light);
      }
   }

   private static void alongAxis(Quaternionf q, Vector3f axis, float cx, float cy, float cz) {
      Vector3f toCam = new Vector3f(-cx, -cy, -cz);
      Vector3f y = new Vector3f(toCam).cross(axis);
      if (y.lengthSquared() < 1.0E-6F) {
         y.set(Math.abs(axis.y) < 0.9F ? 0.0F : 1.0F, Math.abs(axis.y) < 0.9F ? 1.0F : 0.0F, 0.0F).cross(axis);
      }

      y.normalize();
      Vector3f z = new Vector3f(axis).cross(y);
      basis(q, axis, y, z);
   }

   private static boolean lying(Quaternionf q, Vector3f axis) {
      Vector3f n = new Vector3f(-axis.x * axis.y, 1.0F - axis.y * axis.y, -axis.z * axis.y);
      if (n.lengthSquared() < 1.0E-4F) {
         return false;
      }

      n.normalize();
      Vector3f y = new Vector3f(n).cross(axis);
      basis(q, axis, y, n);
      return true;
   }

   private static void facing(Quaternionf q, float yawDeg, float pitchDeg) {
      float yr = yawDeg * (float) (Math.PI / 180.0);
      float pr = pitchDeg * (float) (Math.PI / 180.0);
      float cp = Mth.cos(pr);
      Vector3f n = new Vector3f(-Mth.sin(yr) * cp, -Mth.sin(pr), Mth.cos(yr) * cp);
      Vector3f y = new Vector3f(-n.x * n.y, 1.0F - n.y * n.y, -n.z * n.y);
      if (y.lengthSquared() < 1.0E-4F) {
         y.set(-Mth.sin(yr), 0.0F, Mth.cos(yr));
      }

      y.normalize();
      Vector3f x = new Vector3f(y).cross(n);
      basis(q, x, y, n);
   }

   private static void plane(Quaternionf q, float yawDeg, float pitchDeg, float rollDeg) {
      float yr = yawDeg * (float) (Math.PI / 180.0);
      float pr = pitchDeg * (float) (Math.PI / 180.0);
      float cp = Mth.cos(pr);
      Vector3f f = new Vector3f(-Mth.sin(yr) * cp, -Mth.sin(pr), Mth.cos(yr) * cp);
      Vector3f right = new Vector3f(-Mth.cos(yr), 0.0F, -Mth.sin(yr));
      Vector3f up = new Vector3f(right).cross(f);
      float rr = rollDeg * (float) (Math.PI / 180.0);
      Vector3f x = right.mul(Mth.cos(rr)).add(up.mul(Mth.sin(rr))).normalize();
      Vector3f n = new Vector3f(x).cross(f);
      basis(q, x, f, n);
   }

   private static void basis(Quaternionf q, Vector3f x, Vector3f y, Vector3f z) {
      q.setFromNormalized(new Matrix3f(x, y, z));
   }

   public ParticleRenderType getGroup() {
      return GROUP;
   }

   protected Layer getLayer() {
      if (this.additive) {
         return VfxLayers.additive();
      } else {
         return this.soft ? VfxLayers.soft() : Layer.TRANSLUCENT;
      }
   }

   @Override
   protected int getLightCoords(float tint) {
      return this.kind.glow ? 15728880 : super.getLightCoords(tint);
   }
}
