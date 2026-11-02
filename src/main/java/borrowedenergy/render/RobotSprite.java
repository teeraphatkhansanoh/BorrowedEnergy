package borrowedenergy.render;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/** Shares the original character artwork between the world and the title screen. */
public final class RobotSprite {
    private static final String RESOURCE = "/sprites/robot-reference.png";
    private static final BufferedImage IMAGE = load();
    private static final Map<Integer, BufferedImage> SIZES = new ConcurrentHashMap<>();
    private static final WalkAtlas WALK_ATLAS = loadWalkAtlas();
    private static final Map<Integer, WalkFrame[]> WALK_SIZES = new ConcurrentHashMap<>();

    private RobotSprite() { }

    /** Anchors the artwork at its feet, so its pose never changes the collision position. */
    public static void draw(Graphics2D graphics, double centerX, double feetY,
                            double height, boolean facingLeft) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        BufferedImage sprite = SIZES.computeIfAbsent(Math.max(1, (int)Math.ceil(height)), RobotSprite::scaled);
        double scale = height / sprite.getHeight();
        g.translate(centerX, feetY);
        g.scale(facingLeft ? -scale : scale, scale);
        g.drawImage(sprite, -sprite.getWidth() / 2, -sprite.getHeight(), null);
        g.dispose();
    }

    /** Distance-driven walk poses with actual front, side and rear artwork. */
    public static void drawWalking(Graphics2D graphics, double centerX, double feetY,
                                   double height, double facingX, double facingY,
                                   double walkPhase, double speed, double time) {
        double motion = Math.min(1, Math.max(0, speed / 3.65));
        boolean walking = speed > .12;
        if (WALK_ATLAS == null) {
            draw(graphics, centerX, feetY + (walking ? Math.sin(walkPhase * 2) * .5 : 0), height, facingX < -.1);
            return;
        }
        int row = Math.abs(facingX) > Math.abs(facingY) ? (facingX < 0 ? 1 : 2) : (facingY < 0 ? 3 : 0);
        int pose = walking ? Math.floorMod((int)Math.floor(walkPhase * 2 / Math.PI), 4) : 1;
        WalkFrame[] frames = WALK_SIZES.computeIfAbsent(Math.max(1, (int)Math.ceil(height)), RobotSprite::scaledWalk);
        WalkFrame frame = frames[row * 4 + pose];
        Graphics2D g = (Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double breathe = walking ? -.3 * Math.abs(Math.sin(walkPhase * 2)) * motion : Math.sin(time * 2.4) * .22;
        g.translate(centerX, feetY + breathe);
        double finalScale = height / Math.ceil(height);
        g.scale(finalScale, finalScale);
        g.drawImage(frame.image(), (int)Math.round(-frame.anchorX()), -frame.image().getHeight(), null);
        g.dispose();
    }

    private record WalkFrame(BufferedImage image, double anchorX) { }
    private record WalkAtlas(WalkFrame[] frames, int referenceHeight) { }

    private static WalkAtlas loadWalkAtlas() {
        try {
            BufferedImage sheet = read("/sprites/robot-walk-sheet.png");
            if (sheet == null) return null;
            List<Silhouette> silhouettes = silhouettes(sheet);
            silhouettes.sort(Comparator.comparingInt(Silhouette::area).reversed());
            if (silhouettes.size() < 16) return null;
            List<Silhouette> poses = new ArrayList<>(silhouettes.subList(0, 16));
            poses.sort(Comparator.comparingDouble(s -> s.bounds().getCenterY()));
            for (int row = 0; row < 4; row++)
                poses.subList(row * 4, row * 4 + 4).sort(Comparator.comparingDouble(s -> s.bounds().getCenterX()));
            WalkFrame[] frames = new WalkFrame[16];
            int referenceHeight = 1;
            for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) {
                Rectangle bounds = poses.get(row * 4 + col).bounds();
                // Anchor at the helmet center, rather than the swinging arms or feet.
                long weight = 0, weightedX = 0;
                for (int y = bounds.y; y < bounds.y + bounds.height * .4; y++) {
                    for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                        int alpha = sheet.getRGB(x, y) >>> 24;
                        if (alpha > 24) { weight += alpha; weightedX += (long)x * alpha; }
                    }
                }
                double anchor = (weight > 0 ? weightedX / (double)weight : bounds.getCenterX()) - bounds.x;
                frames[row * 4 + col] = new WalkFrame(sheet.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height), anchor);
                referenceHeight = Math.max(referenceHeight, bounds.height);
            }
            return new WalkAtlas(frames, referenceHeight);
        } catch (IOException e) {
            // The exact reference artwork remains usable if an optional atlas is absent.
            return null;
        }
    }

    private static WalkFrame[] scaledWalk(int height) {
        WalkFrame[] result = new WalkFrame[16];
        double scale = height / (double)WALK_ATLAS.referenceHeight();
        for (int i = 0; i < result.length; i++) {
            WalkFrame frame = WALK_ATLAS.frames()[i];
            int w = Math.max(1, (int)Math.round(frame.image().getWidth() * scale));
            int h = Math.max(1, (int)Math.round(frame.image().getHeight() * scale));
            result[i] = new WalkFrame(resize(frame.image(), w, h), frame.anchorX() * scale);
        }
        return result;
    }

    private static BufferedImage scaled(int height) {
        int width = Math.max(1, (int)Math.round(IMAGE.getWidth() * height / (double)IMAGE.getHeight()));
        return resize(IMAGE, width, height);
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage result = source;
        // Several small reductions keep the thin outlines readable at gameplay size.
        do {
            int w = Math.max(width, result.getWidth() / 2);
            int h = Math.max(height, result.getHeight() / 2);
            BufferedImage step = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = step.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(result, 0, 0, w, h, null); g.dispose();
            result = step;
        } while (result.getWidth() != width || result.getHeight() != height);
        return result;
    }

    private static BufferedImage load() {
        try {
            BufferedImage image = read(RESOURCE);
            if (image == null) throw new IOException("Unsupported character image");
            Rectangle bounds = bounds(image, 0);
            if (bounds.isEmpty()) throw new IOException("Character image is empty");
            // Ignore transparent PDF layout margins at rendering time; preserve the source PNG.
            return image.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + RESOURCE + ". Start the game with ./run.command.", e);
        }
    }

    private static BufferedImage read(String resource) throws IOException {
        try (InputStream stream = RobotSprite.class.getResourceAsStream(resource)) {
            if (stream != null) return ImageIO.read(stream);
        }
        // Supports IDE Run in addition to the bundled classpath resource.
        try (InputStream source = Files.newInputStream(Path.of("src/main/resources" + resource))) {
            return ImageIO.read(source);
        }
    }

    private static Rectangle bounds(BufferedImage image, int alphaThreshold) {
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            if ((image.getRGB(x, y) >>> 24) > alphaThreshold) {
                left = Math.min(left, x); right = Math.max(right, x);
                top = Math.min(top, y); bottom = Math.max(bottom, y);
            }
        }
        return right < left ? new Rectangle() : new Rectangle(left, top, right - left + 1, bottom - top + 1);
    }

    private record Silhouette(Rectangle bounds, int area) { }

    /** Finds full poses despite small spacing differences in a generated grid. */
    private static List<Silhouette> silhouettes(BufferedImage sheet) {
        int w = sheet.getWidth(), h = sheet.getHeight();
        int[] pixels = sheet.getRGB(0, 0, w, h, null, 0, w);
        boolean[] visited = new boolean[w * h];
        int[] queue = new int[w * h];
        List<Silhouette> result = new ArrayList<>();
        for (int seed = 0; seed < visited.length; seed++) {
            if (visited[seed] || (pixels[seed] >>> 24) <= 24) continue;
            int head = 0, tail = 1, left = w, right = -1, top = h, bottom = -1;
            queue[0] = seed; visited[seed] = true;
            while (head < tail) {
                int pixel = queue[head++], x = pixel % w, y = pixel / w;
                left = Math.min(left, x); right = Math.max(right, x);
                top = Math.min(top, y); bottom = Math.max(bottom, y);
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int n = ny * w + nx;
                    if (!visited[n] && (pixels[n] >>> 24) > 24) {
                        visited[n] = true; queue[tail++] = n;
                    }
                }
            }
            if (tail > 200) result.add(new Silhouette(new Rectangle(left, top, right - left + 1, bottom - top + 1), tail));
        }
        return result;
    }
}
