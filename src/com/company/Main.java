package com.company;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        String link;
        Scanner scanner = new Scanner(System.in);

        System.out.println("Git repository link:");
        link = scanner.next();
        Path directory = Paths.get("LocalRep");

        // Vérifie si LocalRep est vide, si non: delete
        if (directory.toFile().listFiles() != null) {
            Files.walk(directory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }

        // Si LocalRep n'existe pas, crée LocalRep
        if (!directory.toFile().exists()){
            directory.toFile().mkdirs();
        }

        ArrayList<String> idVersionList = new ArrayList<>();
        ArrayList<Integer> nbClassList = new ArrayList<>();
        ArrayList<Float> avgWmcList = new ArrayList<>();
        Metric metric = new Metric(directory, link);
        metric.getData(idVersionList, nbClassList, avgWmcList);

        // Crée lignes de données pour fichier csv
        List<String[]> ligneDonnees = new ArrayList<>();
        ligneDonnees.add(new String[] {"id_version", "NC", "mWMC"});
        int counter = 0;
        while (counter < idVersionList.size()) {
            ligneDonnees.add(new String[] {idVersionList.get(counter), nbClassList.get(counter).toString(), String.format("%2.04f", avgWmcList.get(counter))});
            counter++;
        }

        // Création fichier csv
        Csv csv = new Csv();
        File resultDirectory = new File("Result");
        File fichierCSV = new File(resultDirectory, "results.csv");
        boolean verif = csv.creationFichierCSV(fichierCSV, ligneDonnees);

        if (verif) {
            System.out.println("Success.");
        } else {
            System.out.println("Fail.");
        }
    }
}
