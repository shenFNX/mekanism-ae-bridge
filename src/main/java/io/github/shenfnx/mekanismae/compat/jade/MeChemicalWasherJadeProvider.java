package io.github.shenfnx.mekanismae.compat.jade;

public final class MeChemicalWasherJadeProvider extends AbstractMultiKeyMeMachineJadeProvider {
    public static final MeChemicalWasherJadeProvider INSTANCE =
            new MeChemicalWasherJadeProvider();

    private MeChemicalWasherJadeProvider() {
        super("me_chemical_washer");
    }
}
