package scanner;

import java.util.ArrayList;
import java.util.List;

import models.AFNDE;

public class Main {
    public static void main(String[] args) {
        System.out.println("Scanner starting...");

        // Factories
        List<AFNDE> afndeList = new ArrayList<>();
        ErToAFNDE afndeGenerator = new ErToAFNDE();

        // ER to AFNDE
        String er_numbers = "[ - ]? [ [ 0-9 ]+ [ . [ 0-9 ]* ]? | [ . [ 0-9 ]+ ] ] [ [ e / E ] [ - ]? [ 0-9 ]+ ]?";
        AFNDE numbers_AFNDE = afndeGenerator.symbol(er_numbers);
        afndeList.add(numbers_AFNDE);

        String er_strings = "\" [ [ ^ \" \\ ] | [ \\ [ \" / \\ / n / r / t ] ] ]* \"";
        AFNDE strings_AFNDE = afndeGenerator.symbol(er_strings);
        afndeList.add(strings_AFNDE);

        String er_identifiers = "[ a-zA-Z!$%&*\\/:<=>?^_~ ] [ a-zA-Z0-9!$%&*\\/:<=>?^_~+\\-.@ ]*";
        AFNDE identifiers_AFNDE = afndeGenerator.symbol(er_identifiers);
        afndeList.add(identifiers_AFNDE);

        String er_boolean = "# [ t / T / f / F ]";
        AFNDE boolean_AFNDE = afndeGenerator.symbol(er_boolean);
        afndeList.add(boolean_AFNDE);

        // Build
        AFNDE master_AFNDE = afndeGenerator.buildMasterScanner(afndeList);

        // TODO: master_AFNDE to master_AFND

        System.out.println(master_AFNDE.toString());
    }
}