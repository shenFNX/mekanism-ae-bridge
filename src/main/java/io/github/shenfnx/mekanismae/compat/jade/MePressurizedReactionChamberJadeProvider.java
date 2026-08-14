package io.github.shenfnx.mekanismae.compat.jade;

public final class MePressurizedReactionChamberJadeProvider
        extends AbstractMultiKeyMeMachineJadeProvider {
    public static final MePressurizedReactionChamberJadeProvider INSTANCE =
            new MePressurizedReactionChamberJadeProvider();

    private MePressurizedReactionChamberJadeProvider() {
        super("me_pressurized_reaction_chamber");
    }
}
