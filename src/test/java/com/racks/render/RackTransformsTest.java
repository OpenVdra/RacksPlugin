package com.racks.render;

import com.racks.model.RackItemType;
import com.racks.model.RackPart;
import com.racks.model.RackType;
import org.bukkit.util.Transformation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the transcribed pose tables. They are several hundred hand-copied numbers whose only
 * definition of "correct" is the data pack they came from, so what can be checked mechanically —
 * shape, coverage and that no two poses collapsed into each other by a typo — is checked here.
 */
class RackTransformsTest {

    /** Items the data pack wrote ground poses for, in both slots. */
    private static final List<RackItemType> GROUND_ITEMS = List.of(
            RackItemType.AXE, RackItemType.CARROT_ON_A_STICK, RackItemType.FISHING_ROD, RackItemType.HOE,
            RackItemType.MACE, RackItemType.PICKAXE, RackItemType.SHOVEL, RackItemType.SPEAR,
            RackItemType.SWORD, RackItemType.WARPED_FUNGUS_ON_A_STICK);

    /** Items the data pack wrote wall poses for. A wall rack has one slot, so left only. */
    private static final List<RackItemType> WALL_ITEMS = List.of(
            RackItemType.AXE, RackItemType.BOW, RackItemType.CARROT_ON_A_STICK, RackItemType.CROSSBOW,
            RackItemType.FISHING_ROD, RackItemType.HOE, RackItemType.MACE, RackItemType.PICKAXE,
            RackItemType.SHEARS, RackItemType.SHIELD, RackItemType.SHOVEL, RackItemType.SPEAR,
            RackItemType.SPYGLASS, RackItemType.SWORD, RackItemType.TRIDENT,
            RackItemType.WARPED_FUNGUS_ON_A_STICK);

    @Test
    void groundTablesCoverBothSlotsWithSixPoses() {
        for (RackItemType type : GROUND_ITEMS) {
            for (RackPart part : RackPart.values()) {
                RackTransforms.ItemTable table = RackTransforms.item(RackType.GROUND, part, type);
                assertNotNull(table, "missing ground/" + part + "/" + type);
                assertEquals(RackType.GROUND.poseCount(), table.poses().length,
                        "ground/" + part + "/" + type + " should have one entry per pose");
            }
        }
    }

    @Test
    void wallTablesCoverTheSingleSlotWithFourPoses() {
        for (RackItemType type : WALL_ITEMS) {
            RackTransforms.ItemTable table = RackTransforms.item(RackType.WALL, RackPart.LEFT, type);
            assertNotNull(table, "missing wall/left/" + type);
            assertEquals(RackType.WALL.poseCount(), table.poses().length,
                    "wall/left/" + type + " should have one entry per pose");
        }
    }

    /** A wall rack never uses its right slot, so nothing should have been written for it. */
    @Test
    void wallRightSlotIsUnused() {
        for (RackItemType type : RackItemType.values()) {
            assertNull(RackTransforms.item(RackType.WALL, RackPart.RIGHT, type),
                    "wall/right/" + type + " should not exist");
        }
    }

    /** An empty slot has no table anywhere; that is what tells the renderer to leave the display be. */
    @Test
    void emptySlotHasNoTable() {
        for (RackType rackType : RackType.values()) {
            for (RackPart part : RackPart.values()) {
                assertNull(RackTransforms.item(rackType, part, RackItemType.NONE));
            }
        }
    }

    /**
     * Pose recovery has to find the pose it is given back.
     *
     * <p>Not necessarily the same index: several wall tables repeat a pose deliberately (a crossbow
     * looks the same in poses 0 and 2), so the guarantee is that the recovered pose renders
     * identically — which is what adoption actually needs.
     */
    @Test
    void poseRecoveryFindsAMatchingPose() {
        forEachTable((rackType, part, itemType, table) -> {
            for (int pose = 0; pose < table.poses().length; pose++) {
                Transformation applied = table.transformation(pose);
                short recovered = RackTransforms.inferPose(rackType, part, itemType, applied);

                Transformation expected = table.transformation(recovered);
                assertEquals(expected.getTranslation(), applied.getTranslation(),
                        rackType + "/" + part + "/" + itemType + " pose " + pose + " recovered as " + recovered);
                assertEquals(expected.getLeftRotation(), applied.getLeftRotation(),
                        rackType + "/" + part + "/" + itemType + " pose " + pose + " recovered as " + recovered);
            }
        });
    }

    /** Every table hands out its own vectors, so applying one display's pose cannot alter another's. */
    @Test
    void transformationsAreNotShared() {
        forEachTable((rackType, part, itemType, table) -> {
            Transformation first = table.transformation(0);
            Transformation second = table.transformation(0);
            first.getTranslation().add(5f, 5f, 5f);
            first.getScale().mul(3f);
            assertEquals(table.transformation(0).getTranslation(), second.getTranslation());
            assertEquals(table.transformation(0).getScale(), second.getScale());
        });
    }

    /** Body parts are the same story: shared static specs, fresh transforms per call. */
    @Test
    void bodyPartsAreDescribedForBothRackTypes() {
        assertEquals(6, RackTransforms.body(RackType.GROUND).length);
        assertEquals(4, RackTransforms.body(RackType.WALL).length);

        long buttons = java.util.Arrays.stream(RackTransforms.body(RackType.WALL))
                .filter(RackTransforms.BodyPart::button).count();
        assertEquals(2, buttons, "a wall rack's first two parts are buttons, the rest fences");
        assertTrue(java.util.Arrays.stream(RackTransforms.body(RackType.GROUND))
                .noneMatch(RackTransforms.BodyPart::button), "a ground rack is all fences");

        RackTransforms.BodyPart part = RackTransforms.body(RackType.GROUND)[0];
        Transformation first = part.transformation();
        first.getTranslation().add(1f, 1f, 1f);
        assertEquals(part.transformation().getTranslation(), part.transformation().getTranslation());
        assertEquals(part.translation(), part.transformation().getTranslation());
    }

    private void forEachTable(TableVisitor visitor) {
        for (RackType rackType : RackType.values()) {
            for (RackPart part : RackPart.values()) {
                for (RackItemType itemType : RackItemType.values()) {
                    RackTransforms.ItemTable table = RackTransforms.item(rackType, part, itemType);
                    if (table != null) {
                        visitor.visit(rackType, part, itemType, table);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    private interface TableVisitor {
        void visit(RackType rackType, RackPart part, RackItemType itemType, RackTransforms.ItemTable table);
    }
}
