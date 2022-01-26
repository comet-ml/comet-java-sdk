package ml.comet.experiment.impl;

import ml.comet.experiment.impl.utils.ModelUtils;
import ml.comet.experiment.registrymodel.Model;
import org.junit.jupiter.api.Test;

import static ml.comet.experiment.impl.resources.LogMessages.INVALID_MODEL_REGISTRY_NAME_PROVIDED;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static ml.comet.experiment.impl.utils.ModelUtils.createRegistryModelName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class RegistryModelImplTest {

    private static final String SOME_MODEL_NAME = "someModelName";
    private static final String SOME_MODEL_CORRECT_REGISTRY_NAME = "some-correct-registry-name";
    private static final String SOME_MODEL_INCORRECT_REGISTRY_NAME = "someIncorrectRegistryName";

    @Test
    public void testModelBuilder_emptyRegistryName() {
        RegistryModelImpl.RegistryModelBuilderImpl builder =
                new RegistryModelImpl.RegistryModelBuilderImpl(SOME_MODEL_NAME);
        assertNull(builder.model.getRegistryName(), "no registry name should be set yet");

        String expectedRegistryName = ModelUtils.createRegistryModelName(SOME_MODEL_NAME);
        Model model = builder.build();
        assertEquals(expectedRegistryName, model.getRegistryName(), "wrong registry name");
    }

    @Test
    public void testModelBuilder_correctRegistryName() {
        RegistryModelImpl.RegistryModelBuilderImpl builder =
                new RegistryModelImpl.RegistryModelBuilderImpl(SOME_MODEL_NAME);
        assertNull(builder.model.getRegistryName(), "no registry name should be set yet");

        Model model = builder.withRegistryName(SOME_MODEL_CORRECT_REGISTRY_NAME).build();
        assertEquals(SOME_MODEL_CORRECT_REGISTRY_NAME, model.getRegistryName(), "wrong registry name");
    }

    @Test
    public void testModelBuilder_incorrectRegistryName() {
        RegistryModelImpl.RegistryModelBuilderImpl builder =
                new RegistryModelImpl.RegistryModelBuilderImpl(SOME_MODEL_NAME);
        assertNull(builder.model.getRegistryName(), "no registry name should be set yet");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                builder.withRegistryName(SOME_MODEL_INCORRECT_REGISTRY_NAME).build());
        String expectedMessage = getString(INVALID_MODEL_REGISTRY_NAME_PROVIDED,
                SOME_MODEL_INCORRECT_REGISTRY_NAME, createRegistryModelName(SOME_MODEL_INCORRECT_REGISTRY_NAME));
        assertEquals(expectedMessage, ex.getMessage(), "wrong exception message");
    }
}
