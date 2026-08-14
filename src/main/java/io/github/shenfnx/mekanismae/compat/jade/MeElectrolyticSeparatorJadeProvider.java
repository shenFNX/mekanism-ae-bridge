package io.github.shenfnx.mekanismae.compat.jade;

public final class MeElectrolyticSeparatorJadeProvider extends AbstractMultiKeyMeMachineJadeProvider {
    public static final MeElectrolyticSeparatorJadeProvider INSTANCE =
            new MeElectrolyticSeparatorJadeProvider();

    private MeElectrolyticSeparatorJadeProvider() {
        super("me_electrolytic_separator");
    }
}
