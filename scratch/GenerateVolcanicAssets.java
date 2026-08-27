import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateVolcanicAssets {
    public static void main(String[] args) throws Exception {
        File blockDir = new File("src/main/resources/assets/enchantedwood/textures/block");
        File itemDir = new File("src/main/resources/assets/enchantedwood/textures/item");
        File guiDir = new File("src/main/resources/assets/enchantedwood/textures/gui/container");
        blockDir.mkdirs();
        itemDir.mkdirs();
        guiDir.mkdirs();

        // 1. volcanic_soil.png (16x16) - Rich dark peat soil with warm orange/red volcanic mineral flecks
        BufferedImage soil = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[] soilBases = {0xFF241C18, 0xFF2D231E, 0xFF352B24, 0xFF1D1714};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int col = soilBases[(x * 3 + y * 7 + (x ^ y)) % soilBases.length];
                if ((x * 13 + y * 17) % 19 == 0) col = 0xFF8A3B18; // warm mineral
                if ((x * 7 + y * 11) % 23 == 0) col = 0xFFC95B20;  // bright ember speck
                soil.setRGB(x, y, col);
            }
        }
        ImageIO.write(soil, "png", new File(blockDir, "volcanic_soil.png"));

        // 2. pozzolanic_asphalt.png (16x16) - Smooth Roman dark pavement with fine aggregate specks
        BufferedImage asphalt = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[] aspBases = {0xFF26262B, 0xFF2F2F34, 0xFF36363C, 0xFF222226};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int col = aspBases[(x * 5 + y * 11 + (x ^ y)) % aspBases.length];
                if ((x * 17 + y * 7) % 29 == 0) col = 0xFF585860;
                if ((x * 9 + y * 13) % 31 == 0) col = 0xFF7A6A55; // pozzolanic ash speck
                asphalt.setRGB(x, y, col);
            }
        }
        ImageIO.write(asphalt, "png", new File(blockDir, "pozzolanic_asphalt.png"));

        // 3. volcanic_bricks.png (16x16) - Dark volcanic heat bricks with subtle mortar
        BufferedImage bricks = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gBricks = bricks.createGraphics();
        gBricks.setColor(new Color(0x18, 0x15, 0x15));
        gBricks.fillRect(0, 0, 16, 16);
        // Brick layout
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean isMortar = (y % 4 == 0) || ((y / 4) % 2 == 0 ? (x % 8 == 0) : ((x + 4) % 8 == 0));
                if (isMortar) {
                    bricks.setRGB(x, y, 0xFF141212);
                } else {
                    int shade = 0xFF2E2422 + ((x * 7 + y * 13) % 4) * 0x00040303;
                    if ((x * 5 + y * 9) % 17 == 0) shade = 0xFF4A322C;
                    bricks.setRGB(x, y, shade);
                }
            }
        }
        gBricks.dispose();
        ImageIO.write(bricks, "png", new File(blockDir, "volcanic_bricks.png"));

        // 4. soil_infuser machine textures
        BufferedImage infTop = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage infSide = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage infBottom = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage infFrontOff = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage infFrontOn = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int base = (x == 0 || x == 15 || y == 0 || y == 15) ? 0xFF22262B : 0xFF353C45;
                infTop.setRGB(x, y, base);
                infSide.setRGB(x, y, base);
                infBottom.setRGB(x, y, 0xFF22262B);
                infFrontOff.setRGB(x, y, base);
                infFrontOn.setRGB(x, y, base);
            }
        }
        // Front chamber
        for (int y = 4; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
                infFrontOff.setRGB(x, y, 0xFF1E1C1A);
                infFrontOn.setRGB(x, y, 0xFFC95B20); // glowing orange infusion chamber
            }
        }
        ImageIO.write(infTop, "png", new File(blockDir, "soil_infuser_top.png"));
        ImageIO.write(infSide, "png", new File(blockDir, "soil_infuser_side.png"));
        ImageIO.write(infBottom, "png", new File(blockDir, "soil_infuser_bottom.png"));
        ImageIO.write(infFrontOff, "png", new File(blockDir, "soil_infuser_front.png"));
        ImageIO.write(infFrontOn, "png", new File(blockDir, "soil_infuser_front_on.png"));

        // 5. volcanic_fertilizer.png (16x16 item)
        BufferedImage fert = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gFert = fert.createGraphics();
        gFert.setColor(new Color(0x3B, 0x2A, 0x22));
        gFert.fillOval(3, 4, 10, 9);
        gFert.setColor(new Color(0x28, 0x7E, 0x3B)); // vibrant growth green
        gFert.fillOval(5, 5, 6, 6);
        gFert.setColor(new Color(0xE8, 0x6E, 0x22)); // volcanic ember center
        gFert.fillOval(7, 7, 3, 3);
        gFert.dispose();
        ImageIO.write(fert, "png", new File(itemDir, "volcanic_fertilizer.png"));

        // 6. soil_infuser_gui.png (256x256)
        BufferedImage gui = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gui.createGraphics();
        // Background
        g.setColor(new Color(0xC6, 0xC6, 0xC6));
        g.fillRect(0, 0, 176, 166);
        // Bevels
        g.setColor(Color.WHITE);
        g.drawLine(0, 0, 175, 0);
        g.drawLine(0, 0, 0, 165);
        g.setColor(new Color(0x55, 0x55, 0x55));
        g.drawLine(175, 0, 175, 165);
        g.drawLine(0, 165, 175, 165);

        // Slots
        drawSlot(g, 43, 34);  // Dirt input A
        drawSlot(g, 63, 34);  // Mineral input B
        drawSlot(g, 119, 34); // Output
        drawSlot(g, 151, 7);  // Gear slot

        // Player Inventory Slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, 7 + col * 18, 83 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(g, 7 + col * 18, 141);
        }

        // Energy Bar Frame at (17, 19), 18x52
        g.setColor(new Color(0x37, 0x37, 0x37));
        g.fillRect(17, 19, 18, 52);
        g.setColor(new Color(0x1F, 0x1F, 0x1F));
        g.fillRect(18, 20, 16, 50);

        // Progress Arrow Frame at (86, 34), 24x17
        g.setColor(new Color(0x8B, 0x8B, 0x8B));
        g.fillRect(86, 34, 24, 17);

        // SPRITE SHEET REGION:
        // Progress Arrow (24x17) at (176, 14)
        g.setColor(new Color(0x28, 0x8A, 0x44)); // rich green infusion arrow
        g.fillRect(176, 14, 24, 17);

        // Energy Bar (16x50) at (192, 0)
        for (int i = 0; i < 50; i++) {
            float ratio = (float) i / 50.0f;
            int red = (int) (220 + 35 * ratio);
            int grn = (int) (60 + 120 * ratio);
            g.setColor(new Color(red, grn, 20));
            g.drawLine(192, i, 207, i);
        }

        g.dispose();
        ImageIO.write(gui, "png", new File(guiDir, "soil_infuser_gui.png"));

        System.out.println("Volcanic assets successfully created!");
    }

    private static void drawSlot(Graphics2D g, int x, int y) {
        g.setColor(new Color(0x37, 0x37, 0x37));
        g.drawLine(x, y, x + 17, y);
        g.drawLine(x, y, x, y + 17);
        g.setColor(Color.WHITE);
        g.drawLine(x + 17, y + 1, x + 17, y + 17);
        g.drawLine(x + 1, y + 17, x + 17, y + 17);
        g.setColor(new Color(0x8B, 0x8B, 0x8B));
        g.fillRect(x + 1, y + 1, 16, 16);
    }
}
