package com.company;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Csv {
    /**
     * Méthode utilisée pour formater ligne de données.
     *
     * @param donnees ligne de données
     * @return ligne de données formaté
     */

    private String conversionCSV(String[] donnees) {
        return Stream.of(donnees).collect(Collectors.joining(","));
    }

    /**
     * Méthode utilisée pour créer les fichier CSV
     *
     * @param fichierCSV    le fichier CSV
     * @param lignesDonnees les lignes a ajouter dans le CSV
     * @return true si le fichier a bien été créé
     */
    public boolean creationFichierCSV(File fichierCSV, List<String[]> lignesDonnees) {
        try {
            if (!fichierCSV.createNewFile()) {
                fichierCSV.delete();
            }
            try (PrintWriter pw = new PrintWriter(fichierCSV)) {
                lignesDonnees.stream().map(this::conversionCSV).forEach(pw::println);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
