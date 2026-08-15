package com.racks.render;

import com.racks.model.RackItemType;
import com.racks.model.RackPart;
import com.racks.model.RackType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Every display transform the data pack applied, transcribed value for value.
 *
 * <p>Two families live here:
 * <ul>
 *   <li><b>Body parts</b> ({@link #groundBody()}, {@link #wallBody()}) — the fence and button
 *       {@code block_display} entities that make up the rack itself, from
 *       {@code entities/body/create/<ground|wall>/_run}.</li>
 *   <li><b>Held items</b> ({@link #item}) — one table per rack type × slot × item type, from
 *       {@code entities/items/update/<ground|wall>/<left|right>/<item>}, each holding one entry per
 *       pose (six on the ground, four on a wall).</li>
 * </ul>
 *
 * <p>Item tables are stored in a flat array indexed by rack type, part and item type, so posing an
 * item is three array reads. Every {@link Transformation} is <b>rebuilt per call</b> rather than
 * cached and handed out: Bukkit's {@code Transformation} exposes its JOML vectors directly, and a
 * shared instance would let one display's applied transform be mutated through another's.
 *
 * <p>The right rotation is always identity. The data pack never set it: its item updates merge only
 * {@code scale}, {@code left_rotation} and {@code translation} into a freshly summoned display,
 * whose right rotation starts as identity and is never touched again.
 */
public final class RackTransforms {

    private RackTransforms() {
    }

    // ------------------------------------------------------------------------------------------------
    // Body parts
    // ------------------------------------------------------------------------------------------------

    /**
     * One block display of a rack's body: which block it shows and how it is transformed. Part
     * numbering matches the data pack's {@code pk.racks.block.rack.body.<n>} tags.
     */
    public record BodyPart(int id, boolean button, Vector3f translation, Quaternionf leftRotation, Vector3f scale) {

        public Transformation transformation() {
            return new Transformation(new Vector3f(translation), new Quaternionf(leftRotation),
                    new Vector3f(scale), new Quaternionf());
        }
    }

    /** Six fence displays, from {@code entities/body/create/ground/_run}. */
    private static final BodyPart[] GROUND_BODY = {
            body(1, false, -0.743f, -0.677f, 0.135f, -0.225f, 0f, 0f, 0.974f, 0.65f, 1.0f, 0.65f),
            body(2, false, 0.0939f, -0.677f, 0.135f, -0.225f, 0f, 0f, 0.974f, 0.65f, 1.0f, 0.65f),
            body(3, false, -0.7429f, -0.39f, -0.719f, 0.225f, 0f, 0f, 0.974f, 0.65f, 1.0f, 0.65f),
            body(4, false, 0.094f, -0.39f, -0.719f, 0.225f, 0f, 0f, 0.974f, 0.65f, 1.0f, 0.65f),
            body(5, false, 0.501f, 0.5f, -0.01f, 0.5f, -0.5f, 0.5f, 0.5f, 0.26f, 1.001f, 0.65f),
            body(6, false, 0.501f, 0.5f, -0.25f, 0.5f, -0.5f, 0.5f, 0.5f, 0.26f, 1.001f, 0.65f),
    };

    /**
     * Two button displays then two fence displays, from {@code entities/body/create/wall/_run}. The
     * data pack picked the block per part id there: parts 1–2 are buttons, parts 3+ are fences.
     */
    private static final BodyPart[] WALL_BODY = {
            body(1, true, 0.85f, -0.715f, -0.5325f, 0f, 0f, 0.707f, 0.707f, 1.1f, 1.1f, 1.1f),
            body(2, true, 0.25f, -0.715f, -0.5325f, 0f, 0f, 0.707f, 0.707f, 1.1f, 1.1f, 1.1f),
            body(3, false, 0.05f, -0.47f, 0.3375f, -0.383f, 0f, 0f, 0.924f, 0.5f, 0.45f, 0.5f),
            body(4, false, -0.55f, -0.47f, 0.3375f, -0.383f, 0f, 0f, 0.924f, 0.5f, 0.45f, 0.5f),
    };

    public static BodyPart[] groundBody() {
        return GROUND_BODY;
    }

    public static BodyPart[] wallBody() {
        return WALL_BODY;
    }

    public static BodyPart[] body(RackType type) {
        return type.isWall() ? WALL_BODY : GROUND_BODY;
    }

    private static BodyPart body(int id, boolean button, float tx, float ty, float tz,
                                 float qx, float qy, float qz, float qw,
                                 float sx, float sy, float sz) {
        return new BodyPart(id, button, new Vector3f(tx, ty, tz), new Quaternionf(qx, qy, qz, qw),
                new Vector3f(sx, sy, sz));
    }

    // ------------------------------------------------------------------------------------------------
    // Held items
    // ------------------------------------------------------------------------------------------------

    /** A single pose within an item table: where the item sits and how it is turned. */
    public record Pose(Vector3f translation, Quaternionf leftRotation) {
    }

    /** All poses for one rack type × slot × item type, plus the scale and display mode they share. */
    public record ItemTable(Vector3f scale, ItemDisplay.ItemDisplayTransform mode, Pose[] poses) {

        /** The transform for {@code pose}, clamped into range so a stale stored pose cannot throw. */
        public Transformation transformation(int pose) {
            Pose p = poses[Math.floorMod(pose, poses.length)];
            return new Transformation(new Vector3f(p.translation()), new Quaternionf(p.leftRotation()),
                    new Vector3f(scale), new Quaternionf());
        }
    }

    private static final int TYPES = RackItemType.values().length;
    private static final int PARTS = RackPart.values().length;

    /** [rackType][part][itemType]; null where the data pack had no function to dispatch to. */
    private static final ItemTable[][][] ITEMS = new ItemTable[RackType.values().length][PARTS][TYPES];

    /**
     * Transform table for an item on a rack, or null when that combination has none — an empty slot,
     * or an item the data pack had no pose file for. A null table means "leave the display alone",
     * which is what the data pack's dispatch to a non-existent function amounted to.
     */
    public static @Nullable ItemTable item(RackType rackType, RackPart part, RackItemType itemType) {
        return ITEMS[rackType.ordinal()][part.ordinal()][itemType.ordinal()];
    }

    private static void put(RackType rackType, RackPart part, RackItemType itemType, ItemTable table) {
        ITEMS[rackType.ordinal()][part.ordinal()][itemType.ordinal()] = table;
    }

    private static Pose p(float qx, float qy, float qz, float qw, float tx, float ty, float tz) {
        return new Pose(new Vector3f(tx, ty, tz), new Quaternionf(qx, qy, qz, qw));
    }

    private static ItemTable table(float scale, Pose... poses) {
        return new ItemTable(new Vector3f(scale, scale, scale), ItemDisplay.ItemDisplayTransform.NONE, poses);
    }

    private static ItemTable handTable(float scale, Pose... poses) {
        return new ItemTable(new Vector3f(scale, scale, scale),
                ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND, poses);
    }

    static {
        registerGroundLeft();
        registerGroundRight();
        registerWallLeft();
    }

    // -- Ground, left slot ---------------------------------------------------------------------------
    //
    // Most ground items share one rotation sweep across the six poses and differ only in translation;
    // the values are still written out per item so each table matches its source file line for line.

    private static void registerGroundLeft() {
        RackType g = RackType.GROUND;
        RackPart l = RackPart.LEFT;

        put(g, l, RackItemType.AXE, table(1f,
                p(-.271f, .653f, -.271f, .653f, .155f, .188f, -.04f),
                p(-.354f, .854f, -.146f, .354f, .135f, .188f, -.035f),
                p(-.354f, .854f, .146f, -.354f, .135f, .188f, .035f),
                p(-.271f, .653f, .271f, -.653f, .155f, .188f, .04f),
                p(-.146f, .354f, .354f, -.854f, .205f, .188f, .024f),
                p(-.146f, .354f, -.354f, .854f, .205f, .188f, -.024f)));

        put(g, l, RackItemType.CARROT_ON_A_STICK, table(1f,
                p(-.271f, .653f, -.271f, .653f, .155f, .078f, -.05f),
                p(-.354f, .854f, -.146f, .354f, .135f, .078f, -.035f),
                p(-.354f, .854f, .146f, -.354f, .135f, .078f, .035f),
                p(-.271f, .653f, .271f, -.653f, .155f, .078f, .05f),
                p(-.146f, .354f, .354f, -.854f, .205f, .078f, .025f),
                p(-.146f, .354f, -.354f, .854f, .205f, .078f, -.025f)));

        put(g, l, RackItemType.FISHING_ROD, table(1f,
                p(-.271f, .653f, -.271f, .653f, .155f, .078f, -.05f),
                p(-.354f, .854f, -.146f, .354f, .135f, .078f, -.035f),
                p(-.354f, .854f, .146f, -.354f, .135f, .078f, .035f),
                p(-.271f, .653f, .271f, -.653f, .155f, .078f, .05f),
                p(-.146f, .354f, .354f, -.854f, .205f, .078f, .025f),
                p(-.146f, .354f, -.354f, .854f, .205f, .078f, -.025f)));

        put(g, l, RackItemType.HOE, table(1f,
                p(-.271f, .653f, -.271f, .653f, .155f, .063f, -.04f),
                p(-.354f, .854f, -.146f, .354f, .135f, .063f, -.035f),
                p(-.354f, .854f, .146f, -.354f, .135f, .063f, .035f),
                p(-.271f, .653f, .271f, -.653f, .155f, .063f, .04f),
                p(-.146f, .354f, .354f, -.854f, .205f, .063f, .024f),
                p(-.146f, .354f, -.354f, .854f, .205f, .063f, -.024f)));

        put(g, l, RackItemType.MACE, table(1f,
                p(-.271f, .653f, -.271f, .653f, .155f, .268f, 0f),
                p(-.354f, .854f, -.146f, .354f, .155f, .268f, 0f),
                p(-.354f, .854f, .146f, -.354f, .155f, .268f, 0f),
                p(-.271f, .653f, .271f, -.653f, .155f, .268f, 0f),
                p(-.146f, .354f, .354f, -.854f, .155f, .268f, 0f),
                p(-.146f, .354f, -.354f, .854f, .155f, .268f, 0f)));

        put(g, l, RackItemType.PICKAXE, table(1f,
                p(-.271f, .653f, -.271f, .653f, .155f, .076f, -.04f),
                p(-.354f, .854f, -.146f, .354f, .135f, .076f, -.035f),
                p(-.354f, .854f, .146f, -.354f, .135f, .076f, .035f),
                p(-.271f, .653f, .271f, -.653f, .155f, .076f, .04f),
                p(-.146f, .354f, .354f, -.854f, .205f, .076f, .024f),
                p(-.146f, .354f, -.354f, .854f, .205f, .076f, -.024f)));

        put(g, l, RackItemType.SHOVEL, table(1f,
                p(-.271f, .653f, -.271f, .653f, .155f, .145f, -.04f),
                p(-.354f, .854f, -.146f, .354f, .135f, .145f, -.035f),
                p(-.354f, .854f, .146f, -.354f, .135f, .145f, .035f),
                p(-.271f, .653f, .271f, -.653f, .155f, .145f, .04f),
                p(-.146f, .354f, .354f, -.854f, .205f, .145f, .024f),
                p(-.146f, .354f, -.354f, .854f, .205f, .145f, -.024f)));

        put(g, l, RackItemType.SPEAR, handTable(1.1f,
                p(0f, 1f, 0f, 0f, .155f, .68f, -.135f),
                p(0f, 0.383f, 0f, 0.924f, .225f, .68f, .09f),
                p(0f, -0.383f, 0f, 0.924f, .085f, .68f, .09f),
                p(0f, 0f, 0f, 1f, .155f, .68f, .135f),
                p(0.087f, 0f, 0f, 0.996f, .155f, .62f, .225f),
                p(-0.087f, 0f, 0f, 0.996f, .155f, .68f, .055f)));

        put(g, l, RackItemType.SWORD, table(1f,
                p(.653f, .271f, .653f, .271f, .155f, .168f, 0f),
                p(.854f, .354f, .354f, .146f, .155f, .168f, 0f),
                p(.854f, .354f, -.354f, -.146f, .155f, .168f, 0f),
                p(.653f, .271f, -.653f, -.271f, .155f, .168f, 0f),
                p(.354f, .146f, -.854f, -.354f, .155f, .168f, 0f),
                p(.354f, .146f, .854f, .354f, .155f, .168f, 0f)));

        put(g, l, RackItemType.WARPED_FUNGUS_ON_A_STICK, table(1f,
                p(-.271f, .653f, -.271f, .653f, .155f, .128f, 0f),
                p(-.354f, .854f, -.146f, .354f, .165f, .128f, .005f),
                p(-.354f, .854f, .146f, -.354f, .165f, .128f, -.005f),
                p(-.271f, .653f, .271f, -.653f, .155f, .128f, 0f),
                p(-.146f, .354f, .354f, -.854f, .175f, .128f, .005f),
                p(-.146f, .354f, -.354f, .854f, .175f, .128f, -.005f)));
    }

    // -- Ground, right slot --------------------------------------------------------------------------

    private static void registerGroundRight() {
        RackType g = RackType.GROUND;
        RackPart r = RackPart.RIGHT;

        put(g, r, RackItemType.AXE, table(1f,
                p(-.271f, .653f, -.271f, .653f, -.155f, .188f, -.04f),
                p(-.354f, .854f, -.146f, .354f, -.205f, .188f, -.035f),
                p(-.354f, .854f, .146f, -.354f, -.205f, .188f, .035f),
                p(-.271f, .653f, .271f, -.653f, -.155f, .188f, .04f),
                p(-.146f, .354f, .354f, -.854f, -.135f, .188f, .024f),
                p(-.146f, .354f, -.354f, .854f, -.135f, .188f, -.024f)));

        put(g, r, RackItemType.CARROT_ON_A_STICK, table(1f,
                p(-.271f, .653f, -.271f, .653f, -.155f, .078f, -.05f),
                p(-.354f, .854f, -.146f, .354f, -.205f, .078f, -.035f),
                p(-.354f, .854f, .146f, -.354f, -.205f, .078f, .035f),
                p(-.271f, .653f, .271f, -.653f, -.155f, .078f, .05f),
                p(-.146f, .354f, .354f, -.854f, -.135f, .078f, .025f),
                p(-.146f, .354f, -.354f, .854f, -.135f, .078f, -.025f)));

        put(g, r, RackItemType.FISHING_ROD, table(1f,
                p(-.271f, .653f, -.271f, .653f, -.155f, .078f, -.05f),
                p(-.354f, .854f, -.146f, .354f, -.205f, .078f, -.035f),
                p(-.354f, .854f, .146f, -.354f, -.205f, .078f, .035f),
                p(-.271f, .653f, .271f, -.653f, -.155f, .078f, .05f),
                p(-.146f, .354f, .354f, -.854f, -.135f, .078f, .025f),
                p(-.146f, .354f, -.354f, .854f, -.135f, .078f, -.025f)));

        put(g, r, RackItemType.HOE, table(1f,
                p(-.271f, .653f, -.271f, .653f, -.155f, .063f, -.04f),
                p(-.354f, .854f, -.146f, .354f, -.205f, .063f, -.035f),
                p(-.354f, .854f, .146f, -.354f, -.205f, .063f, .035f),
                p(-.271f, .653f, .271f, -.653f, -.155f, .063f, .04f),
                p(-.146f, .354f, .354f, -.854f, -.135f, .063f, .024f),
                p(-.146f, .354f, -.354f, .854f, -.135f, .063f, -.024f)));

        put(g, r, RackItemType.MACE, table(1f,
                p(-.271f, .653f, -.271f, .653f, -.155f, .268f, 0f),
                p(-.354f, .854f, -.146f, .354f, -.155f, .268f, 0f),
                p(-.354f, .854f, .146f, -.354f, -.155f, .268f, 0f),
                p(-.271f, .653f, .271f, -.653f, -.155f, .268f, 0f),
                p(-.146f, .354f, .354f, -.854f, -.155f, .268f, 0f),
                p(-.146f, .354f, -.354f, .854f, -.155f, .268f, 0f)));

        put(g, r, RackItemType.PICKAXE, table(1f,
                p(-.271f, .653f, -.271f, .653f, -.155f, .076f, -.04f),
                p(-.354f, .854f, -.146f, .354f, -.205f, .076f, -.035f),
                p(-.354f, .854f, .146f, -.354f, -.205f, .076f, .035f),
                p(-.271f, .653f, .271f, -.653f, -.155f, .076f, .04f),
                p(-.146f, .354f, .354f, -.854f, -.135f, .076f, .024f),
                p(-.146f, .354f, -.354f, .854f, -.135f, .076f, -.024f)));

        put(g, r, RackItemType.SHOVEL, table(1f,
                p(-.271f, .653f, -.271f, .653f, -.155f, .145f, -.04f),
                p(-.354f, .854f, -.146f, .354f, -.205f, .145f, -.035f),
                p(-.354f, .854f, .146f, -.354f, -.205f, .145f, .035f),
                p(-.271f, .653f, .271f, -.653f, -.155f, .145f, .04f),
                p(-.146f, .354f, .354f, -.854f, -.135f, .145f, .024f),
                p(-.146f, .354f, -.354f, .854f, -.135f, .145f, -.024f)));

        put(g, r, RackItemType.SPEAR, handTable(1.1f,
                p(0f, 1f, 0f, 0f, -.155f, .68f, -.135f),
                p(0f, 0.383f, 0f, 0.924f, -.085f, .68f, .09f),
                p(0f, -0.383f, 0f, 0.924f, -.225f, .68f, .09f),
                p(0f, 0f, 0f, 1f, -.155f, .68f, .135f),
                p(0.087f, 0f, 0f, 0.996f, -.155f, .62f, .225f),
                p(-0.087f, 0f, 0f, 0.996f, -.155f, .68f, .055f)));

        put(g, r, RackItemType.SWORD, table(1f,
                p(.653f, .271f, .653f, .271f, -.155f, .168f, 0f),
                p(.854f, .354f, .354f, .146f, -.155f, .168f, 0f),
                p(.854f, .354f, -.354f, -.146f, -.155f, .168f, 0f),
                p(.653f, .271f, -.653f, -.271f, -.155f, .168f, 0f),
                p(.354f, .146f, -.854f, -.354f, -.155f, .168f, 0f),
                p(.354f, .146f, .854f, .354f, -.155f, .168f, 0f)));

        put(g, r, RackItemType.WARPED_FUNGUS_ON_A_STICK, table(1f,
                p(-.271f, .653f, -.271f, .653f, -.155f, .128f, 0f),
                p(-.354f, .854f, -.146f, .354f, -.175f, .128f, .005f),
                p(-.354f, .854f, .146f, -.354f, -.175f, .128f, -.005f),
                p(-.271f, .653f, .271f, -.653f, -.155f, .128f, 0f),
                p(-.146f, .354f, .354f, -.854f, -.165f, .128f, .005f),
                p(-.146f, .354f, -.354f, .854f, -.165f, .128f, -.005f)));
    }

    // -- Wall, left slot -----------------------------------------------------------------------------
    //
    // A wall rack has a single item slot, so there is no right-hand table. Four poses, not six.

    private static void registerWallLeft() {
        RackType w = RackType.WALL;
        RackPart l = RackPart.LEFT;

        put(w, l, RackItemType.AXE, table(1f,
                p(0f, 0f, -0.866f, 0.5f, 0.074f, 0.097f, 0.397f),
                p(0f, 0f, -0.383f, 0.924f, 0.0f, -0.161f, 0.378f),
                p(-0.866f, 0.5f, 0f, 0f, -0.074f, 0.097f, 0.397f),
                p(-0.383f, 0.924f, 0f, 0f, 0.0f, -0.161f, 0.378f)));

        put(w, l, RackItemType.BOW, table(1f,
                p(0f, 0f, 0.383f, 0.924f, 0f, -0.134f, 0.355f),
                p(0f, 0f, 0.924f, 0.383f, 0.3f, -0.433f, 0.378f),
                p(0.383f, 0.924f, 0f, 0f, 0f, -0.134f, 0.355f),
                p(0.924f, 0.383f, 0f, 0f, -0.3f, -0.433f, 0.378f)));

        put(w, l, RackItemType.CARROT_ON_A_STICK, table(1f,
                p(0f, 0f, 0.383f, 0.924f, -0.09f, 0.037f, 0.386f),
                p(0.383f, 0.924f, 0f, 0f, 0.09f, 0.037f, 0.386f),
                p(0f, 0f, 0.383f, 0.924f, -0.09f, 0.037f, 0.386f),
                p(0.383f, 0.924f, 0f, 0f, 0.09f, 0.037f, 0.386f)));

        put(w, l, RackItemType.CROSSBOW, table(1f,
                p(0f, 0f, 0.383f, 0.924f, 0f, -0.195f, 0.355f),
                p(0.383f, 0.924f, 0f, 0f, 0f, -0.195f, 0.355f),
                p(0f, 0f, 0.383f, 0.924f, 0f, -0.195f, 0.355f),
                p(0.383f, 0.924f, 0f, 0f, 0f, -0.195f, 0.355f)));

        put(w, l, RackItemType.FISHING_ROD, table(1f,
                p(0f, 0f, 0.383f, 0.924f, -0.09f, 0.037f, 0.386f),
                p(0.383f, 0.924f, 0f, 0f, 0.09f, 0.037f, 0.386f),
                p(0f, 0f, 0.383f, 0.924f, -0.09f, 0.037f, 0.386f),
                p(0.383f, 0.924f, 0f, 0f, 0.09f, 0.037f, 0.386f)));

        put(w, l, RackItemType.HOE, table(1f,
                p(0f, 0f, 0.342f, 0.94f, -0.045f, 0.086f, 0.397f),
                p(0.342f, 0.94f, 0f, 0f, 0.045f, 0.086f, 0.397f),
                p(0f, 0f, 0.342f, 0.94f, -0.045f, 0.086f, 0.397f),
                p(0.342f, 0.94f, 0f, 0f, 0.045f, 0.086f, 0.397f)));

        put(w, l, RackItemType.MACE, table(1f,
                p(0.259f, 0.966f, 0f, 0f, 0.044f, 0.137f, 0.397f),
                p(-0.383f, 0.924f, 0f, 0f, 0.0f, -0.081f, 0.396f),
                p(0f, 0f, 0.259f, 0.966f, -0.044f, 0.137f, 0.397f),
                p(0f, 0f, -0.383f, 0.924f, 0.0f, -0.081f, 0.396f)));

        put(w, l, RackItemType.PICKAXE, table(1f,
                p(0f, 0f, -0.383f, 0.924f, 0.05f, -0.161f, 0.378f),
                p(-0.383f, 0.924f, 0f, 0f, -0.05f, -0.161f, 0.378f),
                p(0f, 0f, -0.383f, 0.924f, 0.05f, -0.161f, 0.378f),
                p(-0.383f, 0.924f, 0f, 0f, -0.05f, -0.161f, 0.378f)));

        put(w, l, RackItemType.SHEARS, table(0.65f,
                p(0.924f, 0.383f, 0f, 0f, 0.2983f, -0.4834f, 0.375f),
                p(0.924f, 0.383f, 0f, 0f, -0.2983f, -0.4834f, 0.375f),
                p(0f, 0f, 0.924f, 0.383f, 0.2983f, -0.4834f, 0.375f),
                p(0f, 0f, 0.924f, 0.383f, -0.2983f, -0.4834f, 0.375f)));

        put(w, l, RackItemType.SHIELD, table(1f,
                p(0f, 0f, 0f, 1f, -0.5f, 0.5f, -0.292f),
                p(0f, 0f, 0.707f, 0.707f, -0.5f, -0.5f, -0.292f),
                p(0f, 0f, 1f, 0f, 0.5f, -0.5f, -0.292f),
                p(0f, 0f, 0.707f, -0.707f, 0.5f, 0.5f, -0.292f)));

        put(w, l, RackItemType.SHOVEL, table(1f,
                p(0f, 0f, 0.342f, 0.94f, -0.015f, 0.106f, 0.397f),
                p(0.342f, 0.94f, 0f, 0f, 0.015f, 0.106f, 0.397f),
                p(0f, 0f, 0.342f, 0.94f, -0.015f, 0.106f, 0.397f),
                p(0.342f, 0.94f, 0f, 0f, 0.015f, 0.106f, 0.397f)));

        put(w, l, RackItemType.SPEAR, handTable(1.1f,
                p(0.5f, 0.5f, -0.5f, 0.5f, -.115f, -.115f, .385f),
                p(0.5f, -0.5f, 0.5f, 0.5f, .115f, -.115f, .385f),
                p(-0.5f, -0.5f, -0.5f, 0.5f, -.115f, .165f, .385f),
                p(-0.5f, 0.5f, 0.5f, 0.5f, .115f, .165f, .385f)));

        put(w, l, RackItemType.SPYGLASS, table(1f,
                p(0f, 0f, -0.707f, 0.707f, 0f, 0.048f, 0.368f),
                p(0f, 0f, 0.707f, 0.707f, 0f, 0.048f, 0.368f),
                p(0f, 0f, -0.707f, 0.707f, 0f, 0.048f, 0.368f),
                p(0f, 0f, 0.707f, 0.707f, 0f, 0.048f, 0.368f)));

        put(w, l, RackItemType.SWORD, table(1f,
                p(0.383f, 0.924f, 0f, 0f, 0.07f, 0.0178f, 0.386f),
                p(0.924f, 0.383f, 0f, 0f, 0f, -0.161f, 0.378f),
                p(0f, 0f, 0.383f, 0.924f, -0.07f, 0.0178f, 0.386f),
                p(0f, 0f, 0.924f, 0.383f, 0f, -0.161f, 0.378f)));

        put(w, l, RackItemType.TRIDENT, handTable(1f,
                p(-0.354f, 0.354f, 0.612f, 0.612f, -0.075f, 0.03f, 0.435f),
                p(0.354f, 0.354f, -0.612f, 0.612f, 0.075f, -0.08f, 0.435f),
                p(0.612f, -0.612f, 0.354f, 0.354f, -0.075f, -0.08f, 0.36f),
                p(-0.612f, -0.612f, -0.354f, 0.354f, 0.075f, 0.03f, 0.36f)));

        put(w, l, RackItemType.WARPED_FUNGUS_ON_A_STICK, table(1f,
                p(0f, 0f, 0.383f, 0.924f, -0.13f, -0.013f, 0.386f),
                p(0.383f, 0.924f, 0f, 0f, 0.13f, -0.013f, 0.386f),
                p(0f, 0f, 0.383f, 0.924f, -0.13f, -0.013f, 0.386f),
                p(0.383f, 0.924f, 0f, 0f, 0.13f, -0.013f, 0.386f)));
    }

    // ------------------------------------------------------------------------------------------------
    // Pose recovery
    // ------------------------------------------------------------------------------------------------

    /** Loosely larger than the tables' three-decimal precision, far smaller than any gap between poses. */
    private static final float MATCH_EPSILON = 1.0e-3f;

    /**
     * Recovers which pose a display is currently in by matching its transform against the table.
     *
     * <p>Needed when adopting a rack the data pack placed: the pose lived only in the data pack's
     * command storage, which a plugin cannot read, but it is written all over the item display's
     * transform — so it is read back from there instead of resetting every adopted rack to pose 0.
     * Returns 0 when nothing matches (an empty slot, or an item whose pose was never applied).
     */
    public static short inferPose(RackType rackType, RackPart part, RackItemType itemType,
                                  @Nullable Transformation current) {
        ItemTable table = item(rackType, part, itemType);
        if (table == null || current == null) return 0;

        for (int i = 0; i < table.poses().length; i++) {
            Pose pose = table.poses()[i];
            if (close(pose.translation(), current.getTranslation())
                    && close(pose.leftRotation(), current.getLeftRotation())) {
                return (short) i;
            }
        }
        return 0;
    }

    private static boolean close(Vector3f a, Vector3f b) {
        return Math.abs(a.x - b.x) < MATCH_EPSILON
                && Math.abs(a.y - b.y) < MATCH_EPSILON
                && Math.abs(a.z - b.z) < MATCH_EPSILON;
    }

    /** {@code q} and {@code -q} are the same rotation, so a sign flip must still count as a match. */
    private static boolean close(Quaternionf a, Quaternionf b) {
        return componentsClose(a, b, 1f) || componentsClose(a, b, -1f);
    }

    private static boolean componentsClose(Quaternionf a, Quaternionf b, float sign) {
        return Math.abs(a.x - sign * b.x) < MATCH_EPSILON
                && Math.abs(a.y - sign * b.y) < MATCH_EPSILON
                && Math.abs(a.z - sign * b.z) < MATCH_EPSILON
                && Math.abs(a.w - sign * b.w) < MATCH_EPSILON;
    }
}
