package io.github.shenfnx.mekanismae.compat.jade;

public final class MeChemicalDissolutionChamberJadeProvider extends AbstractDualKeyMeMachineJadeProvider {
    public static final MeChemicalDissolutionChamberJadeProvider INSTANCE =
            new MeChemicalDissolutionChamberJadeProvider();

    private MeChemicalDissolutionChamberJadeProvider() {
        super("me_chemical_dissolution_chamber");
    }
}
