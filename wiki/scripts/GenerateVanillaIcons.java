import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateVanillaIcons {
    public static void main(String[] args) throws Exception {
        File itemDir = new File("wiki/docs/public/textures/item");
        File blockDir = new File("wiki/docs/public/textures/block");
        itemDir.mkdirs();
        blockDir.mkdirs();

        // 1. Clay Ball (16x16 pixel art)
        BufferedImage clayBall = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int c_dark = 0xFF606775;
        int c_mid = 0xFF7A8394;
        int c_base = 0xFF8E98A8;
        int c_light = 0xFFAAB4C4;
        int c_high = 0xFFC8D2E2;

        // Draw Clay Ball lump (circle-like blob around center 8,8)
        int[][] clayPixels = {
            {0,0,0,0,0,c_dark,c_dark,c_dark,c_dark,0,0,0,0,0,0,0},
            {0,0,0,c_dark,c_high,c_high,c_light,c_light,c_mid,c_dark,0,0,0,0,0,0},
            {0,0,c_dark,c_high,c_high,c_light,c_base,c_base,c_mid,c_mid,c_dark,0,0,0,0,0},
            {0,c_dark,c_high,c_light,c_light,c_base,c_base,c_mid,c_mid,c_dark,c_dark,0,0,0,0,0},
            {0,c_dark,c_light,c_light,c_base,c_base,c_mid,c_mid,c_dark,c_dark,c_dark,0,0,0,0,0},
            {c_dark,c_light,c_base,c_base,c_base,c_mid,c_mid,c_dark,c_dark,c_dark,c_dark,0,0,0,0,0},
            {c_dark,c_light,c_base,c_base,c_mid,c_mid,c_dark,c_dark,c_dark,c_dark,c_dark,0,0,0,0,0},
            {c_dark,c_base,c_base,c_mid,c_mid,c_dark,c_dark,c_dark,c_dark,c_dark,c_dark,0,0,0,0,0},
            {0,c_dark,c_mid,c_mid,c_dark,c_dark,c_dark,c_dark,c_dark,c_dark,0,0,0,0,0,0},
            {0,0,c_dark,c_dark,c_dark,c_dark,c_dark,c_dark,c_dark,0,0,0,0,0,0,0}
        };
        for (int y = 0; y < clayPixels.length; y++) {
            for (int x = 0; x < clayPixels[y].length; x++) {
                if (clayPixels[y][x] != 0) {
                    clayBall.setRGB(x + 2, y + 3, clayPixels[y][x]);
                }
            }
        }
        ImageIO.write(clayBall, "PNG", new File(itemDir, "clay_ball.png"));
        System.out.println("Generated clay_ball.png");

        // 2. Water Bucket (16x16)
        BufferedImage waterBucket = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int b_dark = 0xFF383838;
        int b_mid = 0xFF7A7A7A;
        int b_light = 0xFFB5B5B5;
        int b_high = 0xFFE0E0E0;
        int w_deep = 0xFF1D4DB5;
        int w_mid = 0xFF2D6CE6;
        int w_light = 0xFF4D88FF;
        int w_high = 0xFF85B0FF;

        // Draw Bucket Shell + Water contents
        int[][] bucketPixels = {
            {0,0,0,0,b_high,b_light,w_high,w_light,w_light,w_mid,b_light,b_mid,0,0,0,0},
            {0,0,0,b_high,b_light,w_high,w_light,w_light,w_mid,w_deep,b_light,b_dark,0,0,0,0},
            {0,0,b_high,b_light,w_high,w_light,w_light,w_mid,w_deep,w_deep,b_light,b_dark,0,0,0,0},
            {0,0,b_high,b_light,w_light,w_light,w_mid,w_deep,w_deep,b_light,b_mid,b_dark,0,0,0,0},
            {0,0,b_high,b_light,w_light,w_mid,w_deep,w_deep,b_light,b_mid,b_dark,0,0,0,0,0},
            {0,0,0,b_high,b_light,w_mid,w_deep,b_light,b_mid,b_dark,0,0,0,0,0,0},
            {0,0,0,0,b_high,b_light,b_light,b_mid,b_dark,0,0,0,0,0,0,0},
            {0,0,0,0,0,b_high,b_mid,b_dark,0,0,0,0,0,0,0,0}
        };
        for (int y = 0; y < bucketPixels.length; y++) {
            for (int x = 0; x < bucketPixels[y].length; x++) {
                if (bucketPixels[y][x] != 0) {
                    waterBucket.setRGB(x + 2, y + 4, bucketPixels[y][x]);
                }
            }
        }
        ImageIO.write(waterBucket, "PNG", new File(itemDir, "water_bucket.png"));
        System.out.println("Generated water_bucket.png");

        // 3. Copy/Mirror to mod resources folder so sync_assets preserves them
        File modItemDir = new File("src/main/resources/assets/enchantedwood/textures/item");
        modItemDir.mkdirs();
        ImageIO.write(clayBall, "PNG", new File(modItemDir, "clay_ball.png"));
        ImageIO.write(waterBucket, "PNG", new File(modItemDir, "water_bucket.png"));
        System.out.println("Successfully saved to mod textures & wiki public textures!");
    }
}
