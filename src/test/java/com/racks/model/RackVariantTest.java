package com.racks.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The twelve woods.
 *
 * <p>{@link RackVariant} resolves its planks, fence and button through {@code Material.valueOf} in an
 * enum constructor, which means a wood the server does not have would not fail gracefully — it would
 * fail the whole enum's class initialisation and take the plugin down with it. Touching every constant
 * here turns that into a build failure instead of a startup one.
 */
class RackVariantTest {

    @Test
    void everyVariantResolvesItsBlocks() {
        for (RackVariant variant : RackVariant.values()) {
            assertNotNull(variant.planks(), variant + " planks");
            assertNotNull(variant.fence(), variant + " fence");
            assertNotNull(variant.button(), variant + " button");
            assertTrue(variant.texture().length() > 32, variant + " should carry a head texture");
        }
        assertEquals(12, RackVariant.values().length);
    }

    @Test
    void variantsAreRecognisedByIdAndByModelString() {
        for (RackVariant variant : RackVariant.values()) {
            assertEquals(variant, RackVariant.byId(variant.id()));
            assertEquals(variant, RackVariant.byId(variant.id().toUpperCase(java.util.Locale.ROOT)));
            assertEquals(variant, RackVariant.byModelString(variant.modelString()));
            assertEquals(variant, RackVariant.byFence(variant.fence()));
        }
    }

    /** The model string is how a rack item made by the data pack is still recognised, so it must not drift. */
    @Test
    void modelStringMatchesTheDataPack() {
        assertEquals("pk_racks:oak_rack", RackVariant.OAK.modelString());
        assertEquals("pk_racks:dark_oak_rack", RackVariant.DARK_OAK.modelString());
    }

    @Test
    void anythingElseIsNotARack() {
        assertNull(RackVariant.byId("mahogany"));
        assertNull(RackVariant.byId(null));
        assertNull(RackVariant.byModelString("pk_racks:oak_chair"));
        assertNull(RackVariant.byModelString("someplugin:oak_rack"));
        assertNull(RackVariant.byModelString(null));
    }
}
