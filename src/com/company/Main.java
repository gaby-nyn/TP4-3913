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
        File dirToFile = directory.toFile();

        // Vérifie si LocalRep est vide, si non: delete
        if (dirToFile.listFiles() != null) {
            Files.walk(directory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }

        // Si LocalRep n'existe pas, crée LocalRep
        if (!dirToFile.exists()){
            dirToFile.mkdirs();
        }

        // Clone projet cible dans repo local
        ProcessBuilder gitClone = new ProcessBuilder().command("git", "clone", link).directory(directory.toFile());
        Process processClone = gitClone.start();
        try{
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e = new InterruptedException("Wait interrupted.");
        }

        // Compte le nombre de commits
        File[] files = directory.toFile().listFiles();
        Path cloneDirectory = files[0].toPath();
        String temp;
        ProcessBuilder gitCount = new ProcessBuilder().command("git", "rev-list","--count", "master").directory(cloneDirectory.toFile());
        Process processGitCount = gitCount.start();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(processGitCount.getInputStream()));
        int nbCommit = 0;
        try {
            nbCommit = Integer.parseInt(stdInput.readLine());
        } catch (IOException e) {
            e = new IOException("Couldn't get count.");
        }

        // Prend les IDs du repo cible
        ProcessBuilder gitRevList = new ProcessBuilder().command("git", "rev-list", "master").directory(cloneDirectory.toFile());
        Process processRevList = gitRevList.start();

        // Arraylist contenant tous les IDs
        ArrayList<String> idVersionListTemp = new ArrayList<>();
        stdInput = new BufferedReader(new InputStreamReader(processRevList.getInputStream()));
        while ((temp = stdInput.readLine()) != null) {
            idVersionListTemp.add(temp);
        }

        // Prend aléatoirement 5% des versions
        ArrayList<String> idVersionList = new ArrayList<>();
        Collections.shuffle(idVersionListTemp);
        int bound = (int)(nbCommit*(5.0f/100.0f));
        int boundCount = 0;
        while (boundCount < bound) {
            idVersionList.add(idVersionListTemp.get(boundCount));
            boundCount++;
        }

        // Compte nombre de classe par commit
        ArrayList<Integer> nombreClasseList = new ArrayList<>();
        for (String s : idVersionList) {
            ProcessBuilder gitReset = new ProcessBuilder().command("git", "reset","--HARD", s).directory(cloneDirectory.toFile());
            Process processGitReset = gitReset.start();
            nombreClasseList.add(getFichiers(cloneDirectory.toFile()));
        }

        // Crée lignes de données pour fichier csv
        List<String[]> ligneDonnees = new ArrayList<>();
        ligneDonnees.add(new String[] {"id_version", "NC"});
        int counter = 0;
        while (counter < idVersionList.size()) {
            ligneDonnees.add(new String[] {idVersionList.get(counter), nombreClasseList.get(counter).toString()});
            counter++;
        }

        // Création fichier csv
        Csv csv = new Csv();
        File resultDirectory = new File("Result");
        File fichierCSV = new File(resultDirectory, "results.csv");
        boolean verif = csv.creationFichierCSV(fichierCSV, ligneDonnees);

        if (verif == true) {
            System.out.println("Success.");
        } else {
            System.out.println("Fail.");
        }
    }

    /**
     * Méthode pour aller chercher tous les fichiers dans le directory donnée.
     * @param dir directory cible
     */
    public static int getFichiers(File dir) {
        int count = 0;
        if (dir.isDirectory()) {
            File[] listeFichiers = dir.listFiles();
            for (File f : listeFichiers) {
                count += getFichiers(f);
            }
        } else {
            if(dir.getName().endsWith(".java")) {
                count++;
            }
        }
        return count;
    }
}
